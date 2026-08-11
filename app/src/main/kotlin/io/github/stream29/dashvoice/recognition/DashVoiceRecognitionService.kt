package io.github.stream29.dashvoice.recognition

import android.Manifest
import android.content.Context
import android.content.ContextParams
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionService
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import io.github.stream29.dashvoice.DashVoiceApplication
import io.github.stream29.dashvoice.dashscope.DashScopeRealtimeSession
import io.github.stream29.dashvoice.data.DashVoiceSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class DashVoiceRecognitionService : RecognitionService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val configurationRepository by lazy {
        (application as DashVoiceApplication).container.configurationRepository
    }
    private var activeRecognition: ActiveRecognition? = null
    private var pendingStart: PendingStart? = null

    override fun onStartListening(recognizerIntent: Intent, listener: Callback) {
        serviceScope.launch {
            if (activeRecognition != null || pendingStart != null) {
                listener.safeError(SpeechRecognizer.ERROR_RECOGNIZER_BUSY)
                return@launch
            }
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                listener.safeError(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS)
                return@launch
            }

            val recordingContext = runCatching { recordingContext(listener) }
                .getOrElse {
                    listener.safeError(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS)
                    return@launch
                }

            val pending = PendingStart(
                callback = listener,
                recognizerIntent = recognizerIntent,
                recordingContext = recordingContext,
            )
            pendingStart = pending
            pending.job = serviceScope.launch {
                val settings = try {
                    configurationRepository.load()
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    Log.w(TAG, "Unable to load Room configuration", exception)
                    if (pendingStart === pending) {
                        pendingStart = null
                        listener.safeError(SpeechRecognizer.ERROR_CLIENT)
                    }
                    return@launch
                }
                if (pendingStart !== pending) return@launch
                pendingStart = null
                if (!settings.isReady) {
                    listener.safeError(SpeechRecognizer.ERROR_CLIENT)
                    return@launch
                }

                val recognition = ActiveRecognition(
                    callback = listener,
                    recognizerIntent = recognizerIntent,
                    recordingContext = recordingContext,
                    settings = settings,
                )
                activeRecognition = recognition
                recognition.start()
                if (pending.stopRequested) recognition.requestStop()
            }
        }
    }

    override fun onStopListening(listener: Callback) {
        serviceScope.launch {
            val recognition = activeRecognition
            if (recognition != null && recognition.callback === listener) {
                recognition.requestStop()
                return@launch
            }

            val pending = pendingStart
            if (pending != null && pending.callback === listener) {
                pending.stopRequested = true
                return@launch
            }
            listener.safeError(SpeechRecognizer.ERROR_CLIENT)
        }
    }

    override fun onCancel(listener: Callback) {
        serviceScope.launch {
            pendingStart
                ?.takeIf { it.callback === listener }
                ?.let {
                    it.job?.cancel()
                    pendingStart = null
                }
            activeRecognition
                ?.takeIf { it.callback === listener }
                ?.cancel()
        }
    }

    override fun onDestroy() {
        pendingStart?.job?.cancel()
        pendingStart = null
        activeRecognition?.cancel()
        activeRecognition = null
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun recordingContext(callback: Callback): Context {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return this
        val params = ContextParams.Builder()
            .setNextAttributionSource(callback.callingAttributionSource)
            .build()
        return createContext(params)
    }

    private inner class ActiveRecognition(
        val callback: Callback,
        recognizerIntent: Intent,
        recordingContext: Context,
        settings: DashVoiceSettings,
    ) {
        private val languageDetectionEnabled =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                recognizerIntent.getBooleanExtra(
                    RecognizerIntent.EXTRA_ENABLE_LANGUAGE_DETECTION,
                    false,
                )
        private val realtimeSession = DashScopeRealtimeSession(
            recordingContext = recordingContext,
            settings = settings,
        )

        private var eventCollectionJob: Job? = null
        private var noSpeechTimeoutJob: Job? = null
        private var maximumSessionJob: Job? = null
        private var completionTimeoutJob: Job? = null
        private var ready = false
        private var speechStarted = false
        private var endOfSpeechSent = false
        private var finished = false
        private var noSpeechTimedOut = false
        private var lastReportedLanguage: String? = null

        fun start() {
            eventCollectionJob = serviceScope.launch {
                realtimeSession.events
                    .catch {
                        Log.w(TAG, "Recognition event flow failed", it)
                        realtimeSession.cancel()
                        reportError(SpeechRecognizer.ERROR_CLIENT)
                    }
                    .collect(::handleEvent)
            }
        }

        fun requestStop() {
            if (finished) return
            noSpeechTimeoutJob?.cancel()
            if (speechStarted) reportEndOfSpeech()
            realtimeSession.finish()
            scheduleCompletionTimeout()
        }

        fun cancel() {
            if (finished) return
            finished = true
            cancelTimeouts()
            eventCollectionJob?.cancel()
            realtimeSession.cancel()
            if (activeRecognition === this) activeRecognition = null
        }

        private fun handleEvent(event: DashScopeRealtimeSession.Event) {
            if (finished || activeRecognition !== this) return

            when (event) {
                DashScopeRealtimeSession.Event.Ready -> {
                    if (ready) return
                    ready = true
                    callback.safeCall { readyForSpeech(Bundle()) }
                    scheduleNoSpeechTimeout()
                    scheduleMaximumSessionTimeout()
                }

                DashScopeRealtimeSession.Event.BeginningOfSpeech -> {
                    if (speechStarted) return
                    speechStarted = true
                    noSpeechTimeoutJob?.cancel()
                    callback.safeCall { beginningOfSpeech() }
                }

                is DashScopeRealtimeSession.Event.RmsChanged -> {
                    callback.safeCall { rmsChanged(event.rms) }
                }

                is DashScopeRealtimeSession.Event.PartialTranscript -> {
                    reportLanguage(event.language)
                    if (event.transcript.isNotBlank()) {
                        callback.safeCall {
                            partialResults(recognitionResults(event.transcript))
                        }
                    }
                }

                DashScopeRealtimeSession.Event.EndOfSpeech -> {
                    reportEndOfSpeech()
                    scheduleCompletionTimeout()
                }

                is DashScopeRealtimeSession.Event.Completed -> {
                    reportLanguage(event.language)
                    when {
                        !event.transcript.isNullOrBlank() -> {
                            finishRecognition {
                                callback.safeCall {
                                    results(recognitionResults(event.transcript))
                                }
                            }
                        }

                        noSpeechTimedOut -> {
                            reportError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
                        }

                        !event.speechDetected -> {
                            reportError(SpeechRecognizer.ERROR_NO_MATCH)
                        }

                        else -> {
                            reportError(SpeechRecognizer.ERROR_NO_MATCH)
                        }
                    }
                }

                is DashScopeRealtimeSession.Event.Failure -> {
                    Log.w(TAG, "Recognition failed: ${event.kind} (${event.detail})")
                    val error = when (event.kind) {
                        DashScopeRealtimeSession.FailureKind.NETWORK ->
                            SpeechRecognizer.ERROR_NETWORK

                        DashScopeRealtimeSession.FailureKind.AUTHENTICATION,
                        DashScopeRealtimeSession.FailureKind.SERVER ->
                            SpeechRecognizer.ERROR_SERVER

                        DashScopeRealtimeSession.FailureKind.AUDIO ->
                            SpeechRecognizer.ERROR_AUDIO

                        DashScopeRealtimeSession.FailureKind.CLIENT ->
                            SpeechRecognizer.ERROR_CLIENT
                    }
                    reportError(error)
                }
            }
        }

        private fun reportEndOfSpeech() {
            if (endOfSpeechSent) return
            endOfSpeechSent = true
            callback.safeCall { endOfSpeech() }
        }

        private fun reportLanguage(language: String?) {
            if (!languageDetectionEnabled || language.isNullOrBlank()) return
            val languageTag = dashScopeLanguageToBcp47(language)
            if (languageTag == lastReportedLanguage) return
            lastReportedLanguage = languageTag
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                callback.safeCall {
                    languageDetection(
                        Bundle().apply {
                            putString(SpeechRecognizer.DETECTED_LANGUAGE, languageTag)
                        },
                    )
                }
            }
        }

        private fun reportError(error: Int) {
            finishRecognition {
                callback.safeError(error)
            }
        }

        private fun finishRecognition(report: () -> Unit) {
            if (finished) return
            finished = true
            cancelTimeouts()
            if (activeRecognition === this) activeRecognition = null
            report()
        }

        private fun scheduleNoSpeechTimeout() {
            noSpeechTimeoutJob?.cancel()
            noSpeechTimeoutJob = serviceScope.launch {
                delay(NO_SPEECH_TIMEOUT_MILLIS)
                if (!finished && activeRecognition === this@ActiveRecognition && !speechStarted) {
                    noSpeechTimedOut = true
                    realtimeSession.finish()
                    scheduleCompletionTimeout()
                }
            }
        }

        private fun scheduleMaximumSessionTimeout() {
            maximumSessionJob?.cancel()
            maximumSessionJob = serviceScope.launch {
                delay(MAXIMUM_SESSION_MILLIS)
                if (!finished && activeRecognition === this@ActiveRecognition) {
                    requestStop()
                }
            }
        }

        private fun scheduleCompletionTimeout() {
            completionTimeoutJob?.cancel()
            completionTimeoutJob = serviceScope.launch {
                delay(COMPLETION_TIMEOUT_MILLIS)
                if (!finished && activeRecognition === this@ActiveRecognition) {
                    realtimeSession.cancel()
                    reportError(SpeechRecognizer.ERROR_NETWORK_TIMEOUT)
                }
            }
        }

        private fun cancelTimeouts() {
            noSpeechTimeoutJob?.cancel()
            maximumSessionJob?.cancel()
            completionTimeoutJob?.cancel()
        }
    }

    private fun Callback.safeError(errorCode: Int) {
        safeCall { error(errorCode) }
    }

    private inline fun Callback.safeCall(block: Callback.() -> Unit) {
        runCatching { block() }
            .onFailure { Log.w(TAG, "Recognition callback is no longer available", it) }
    }

    private data class PendingStart(
        val callback: Callback,
        val recognizerIntent: Intent,
        val recordingContext: Context,
        var stopRequested: Boolean = false,
        var job: Job? = null,
    )

    private companion object {
        const val TAG = "DashVoiceService"
        const val NO_SPEECH_TIMEOUT_MILLIS = 10_000L
        const val MAXIMUM_SESSION_MILLIS = 60_000L
        const val COMPLETION_TIMEOUT_MILLIS = 15_000L
    }
}
