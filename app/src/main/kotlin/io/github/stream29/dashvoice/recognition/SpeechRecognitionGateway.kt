package io.github.stream29.dashvoice.recognition

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow

sealed interface SpeechRecognitionUpdate {
    data object Ready : SpeechRecognitionUpdate
    data object BeginningOfSpeech : SpeechRecognitionUpdate
    data class RmsChanged(val rms: Float) : SpeechRecognitionUpdate
    data object EndOfSpeech : SpeechRecognitionUpdate
    data class PartialResult(val transcript: String) : SpeechRecognitionUpdate
    data class Results(val results: List<String>) : SpeechRecognitionUpdate
    data class LanguageDetected(val languageTag: String) : SpeechRecognitionUpdate
    data class Error(
        val failure: SpeechRecognitionFailure,
        val diagnosticCode: Int? = null,
    ) : SpeechRecognitionUpdate
}

enum class SpeechRecognitionFailure {
    PERMISSION,
    NETWORK,
    SERVER,
    AUDIO,
    SPEECH_TIMEOUT,
    NO_MATCH,
    BUSY,
    LANGUAGE,
    CLIENT,
    OTHER,
}

interface SpeechRecognitionGateway {
    fun recognize(): Flow<SpeechRecognitionUpdate>
    fun stop()
    fun cancel()
}

class AndroidSpeechRecognitionGateway(
    context: Context,
) : SpeechRecognitionGateway {
    private val applicationContext = context.applicationContext
    private var activeRecognition: ActiveRecognition? = null

    override fun recognize(): Flow<SpeechRecognitionUpdate> = callbackFlow {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "SpeechRecognizer must be used from the main thread"
        }
        if (activeRecognition != null) {
            trySend(speechRecognitionError(SpeechRecognizer.ERROR_RECOGNIZER_BUSY))
            close()
            return@callbackFlow
        }

        val recognizer = runCatching {
            SpeechRecognizer.createSpeechRecognizer(
                applicationContext,
                ComponentName(
                    applicationContext,
                    DashVoiceRecognitionService::class.java,
                ),
            )
        }.getOrElse {
            trySend(speechRecognitionError(SpeechRecognizer.ERROR_CLIENT))
            close()
            return@callbackFlow
        }
        val recognition = ActiveRecognition(
            recognizer = recognizer,
            closeFlow = { close() },
        )
        activeRecognition = recognition

        recognizer.setRecognitionListener(
            object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle) {
                    trySend(SpeechRecognitionUpdate.Ready)
                }

                override fun onBeginningOfSpeech() {
                    trySend(SpeechRecognitionUpdate.BeginningOfSpeech)
                }

                override fun onRmsChanged(rmsdB: Float) {
                    trySend(SpeechRecognitionUpdate.RmsChanged(rmsdB))
                }

                override fun onBufferReceived(buffer: ByteArray) = Unit

                override fun onEndOfSpeech() {
                    trySend(SpeechRecognitionUpdate.EndOfSpeech)
                }

                override fun onError(error: Int) {
                    trySend(speechRecognitionError(error))
                    close()
                }

                override fun onResults(results: Bundle) {
                    trySend(
                        SpeechRecognitionUpdate.Results(
                            results.recognitionStrings(),
                        ),
                    )
                    close()
                }

                override fun onPartialResults(partialResults: Bundle) {
                    partialResults.recognitionStrings().firstOrNull()?.let {
                        trySend(SpeechRecognitionUpdate.PartialResult(it))
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle) = Unit

                override fun onSegmentResults(segmentResults: Bundle) {
                    segmentResults.recognitionStrings().firstOrNull()?.let {
                        trySend(SpeechRecognitionUpdate.PartialResult(it))
                    }
                }

                override fun onEndOfSegmentedSession() = Unit

                override fun onLanguageDetection(results: Bundle) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        results.getString(SpeechRecognizer.DETECTED_LANGUAGE)?.let {
                            trySend(SpeechRecognitionUpdate.LanguageDetected(it))
                        }
                    }
                }
            },
        )

        val request = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            request.putExtra(RecognizerIntent.EXTRA_ENABLE_LANGUAGE_DETECTION, true)
        }

        runCatching { recognizer.startListening(request) }
            .onFailure {
                trySend(speechRecognitionError(SpeechRecognizer.ERROR_CLIENT))
                close()
            }

        awaitClose {
            if (activeRecognition === recognition) {
                activeRecognition = null
            }
            runCatching { recognizer.cancel() }
            runCatching { recognizer.destroy() }
        }
    }.buffer(Channel.UNLIMITED)

    override fun stop() {
        runCatching { activeRecognition?.recognizer?.stopListening() }
    }

    override fun cancel() {
        activeRecognition?.let {
            runCatching { it.recognizer.cancel() }
            it.closeFlow()
        }
    }

    private data class ActiveRecognition(
        val recognizer: SpeechRecognizer,
        val closeFlow: () -> Unit,
    )

    @Suppress("DEPRECATION")
    private fun Bundle.recognitionStrings(): ArrayList<String> =
        getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: arrayListOf()
}

private fun speechRecognitionError(errorCode: Int) = SpeechRecognitionUpdate.Error(
    failure = when (errorCode) {
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
            SpeechRecognitionFailure.PERMISSION

        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
        SpeechRecognizer.ERROR_SERVER_DISCONNECTED ->
            SpeechRecognitionFailure.NETWORK

        SpeechRecognizer.ERROR_SERVER,
        SpeechRecognizer.ERROR_TOO_MANY_REQUESTS ->
            SpeechRecognitionFailure.SERVER

        SpeechRecognizer.ERROR_AUDIO ->
            SpeechRecognitionFailure.AUDIO

        SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
            SpeechRecognitionFailure.SPEECH_TIMEOUT

        SpeechRecognizer.ERROR_NO_MATCH ->
            SpeechRecognitionFailure.NO_MATCH

        SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
            SpeechRecognitionFailure.BUSY

        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
            SpeechRecognitionFailure.LANGUAGE

        SpeechRecognizer.ERROR_CLIENT ->
            SpeechRecognitionFailure.CLIENT

        else ->
            SpeechRecognitionFailure.OTHER
    },
    diagnosticCode = errorCode,
)
