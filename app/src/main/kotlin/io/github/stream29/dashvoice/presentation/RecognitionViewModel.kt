package io.github.stream29.dashvoice.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.stream29.dashvoice.R
import io.github.stream29.dashvoice.data.ConfigurationRepository
import io.github.stream29.dashvoice.recognition.SpeechRecognitionGateway
import io.github.stream29.dashvoice.recognition.SpeechRecognitionFailure
import io.github.stream29.dashvoice.recognition.SpeechRecognitionUpdate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface RecognitionEvent {
    data object RequestMicrophonePermission : RecognitionEvent
    data object OpenSettings : RecognitionEvent
    data class FinishWithResults(val results: List<String>) : RecognitionEvent
    data object FinishCanceled : RecognitionEvent
}

class RecognitionViewModel(
    private val configurationRepository: ConfigurationRepository,
    private val speechRecognitionGateway: SpeechRecognitionGateway,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(RecognitionUiState.connecting())
    val uiState: StateFlow<RecognitionUiState> = mutableUiState.asStateFlow()

    private val eventChannel = Channel<RecognitionEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private var preparationJob: Job? = null
    private var recognitionJob: Job? = null
    private var recognitionRequested = false
    private var waitingForSettings = false

    fun prepare(force: Boolean = false) {
        if (!force && (preparationJob?.isActive == true || recognitionRequested)) return
        preparationJob?.cancel()
        recognitionJob?.cancel()
        speechRecognitionGateway.cancel()
        recognitionRequested = false
        mutableUiState.value = RecognitionUiState.connecting()

        preparationJob = viewModelScope.launch {
            val settings = try {
                configurationRepository.load()
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                mutableUiState.value = RecognitionUiState.error(
                    messageRes = R.string.recognition_error_read_configuration,
                    guidanceRes = R.string.recognition_error_read_configuration_guidance,
                    configurationError = true,
                )
                return@launch
            }
            if (!settings.isReady) {
                mutableUiState.value = RecognitionUiState.error(
                    messageRes = R.string.recognition_error_incomplete_configuration,
                    guidanceRes = R.string.recognition_error_incomplete_configuration_guidance,
                    configurationError = true,
                )
                return@launch
            }
            eventChannel.send(RecognitionEvent.RequestMicrophonePermission)
        }
    }

    fun onMicrophonePermissionResult(granted: Boolean) {
        if (!granted) {
            mutableUiState.value =
                recognitionErrorState(SpeechRecognitionFailure.PERMISSION)
            return
        }
        if (recognitionRequested) return

        recognitionRequested = true
        mutableUiState.value = RecognitionUiState.connecting()
        recognitionJob = viewModelScope.launch {
            speechRecognitionGateway.recognize()
                .catch {
                    emit(SpeechRecognitionUpdate.Error(SpeechRecognitionFailure.CLIENT))
                }
                .collect(::handleRecognitionUpdate)
        }
    }

    fun stop() {
        speechRecognitionGateway.stop()
    }

    fun retry() {
        prepare(force = true)
    }

    fun openSettings() {
        waitingForSettings = true
        eventChannel.trySend(RecognitionEvent.OpenSettings)
    }

    fun onHostResumed() {
        if (!waitingForSettings) return
        waitingForSettings = false
        retry()
    }

    fun cancel() {
        recognitionJob?.cancel()
        speechRecognitionGateway.cancel()
        recognitionRequested = false
        eventChannel.trySend(RecognitionEvent.FinishCanceled)
    }

    override fun onCleared() {
        speechRecognitionGateway.cancel()
        super.onCleared()
    }

    private fun handleRecognitionUpdate(update: SpeechRecognitionUpdate) {
        when (update) {
            SpeechRecognitionUpdate.Ready -> {
                mutableUiState.update { it.listening() }
            }

            SpeechRecognitionUpdate.BeginningOfSpeech -> {
                mutableUiState.update { it.speaking() }
            }

            is SpeechRecognitionUpdate.RmsChanged -> {
                mutableUiState.update { it.copy(rms = update.rms) }
            }

            SpeechRecognitionUpdate.EndOfSpeech -> {
                mutableUiState.update { it.processing() }
            }

            is SpeechRecognitionUpdate.PartialResult -> {
                mutableUiState.update { it.copy(transcript = update.transcript) }
            }

            is SpeechRecognitionUpdate.Results -> {
                recognitionRequested = false
                if (update.results.isEmpty()) {
                    mutableUiState.value =
                        recognitionErrorState(SpeechRecognitionFailure.NO_MATCH)
                } else {
                    eventChannel.trySend(
                        RecognitionEvent.FinishWithResults(update.results),
                    )
                }
            }

            is SpeechRecognitionUpdate.LanguageDetected -> {
                mutableUiState.update { it.copy(languageTag = update.languageTag) }
            }

            is SpeechRecognitionUpdate.Error -> {
                recognitionRequested = false
                mutableUiState.value = recognitionErrorState(
                    failure = update.failure,
                    diagnosticCode = update.diagnosticCode,
                )
            }
        }
    }
}
