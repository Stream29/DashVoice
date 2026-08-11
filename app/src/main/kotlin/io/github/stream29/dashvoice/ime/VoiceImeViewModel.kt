package io.github.stream29.dashvoice.ime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.annotation.StringRes
import io.github.stream29.dashvoice.presentation.RecognitionUiState
import io.github.stream29.dashvoice.presentation.recognitionErrorState
import io.github.stream29.dashvoice.recognition.SpeechRecognitionFailure
import io.github.stream29.dashvoice.recognition.SpeechRecognitionGateway
import io.github.stream29.dashvoice.recognition.SpeechRecognitionUpdate
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface VoiceImeEffect {
    data class SetComposingText(val text: String) : VoiceImeEffect
    data class CommitAndReturn(val text: String) : VoiceImeEffect
    data class FailAndReturn(@field:StringRes val messageRes: Int) : VoiceImeEffect
    data object CancelAndReturn : VoiceImeEffect
}

class VoiceImeViewModel(
    private val speechRecognitionGateway: SpeechRecognitionGateway,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(RecognitionUiState.connecting())
    val uiState: StateFlow<RecognitionUiState> = mutableUiState.asStateFlow()

    private val effectChannel = Channel<VoiceImeEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    private var recognitionJob: Job? = null

    fun start() {
        if (recognitionJob?.isActive == true) return

        speechRecognitionGateway.cancel()
        mutableUiState.value = RecognitionUiState.connecting()
        recognitionJob = viewModelScope.launch {
            speechRecognitionGateway.recognize()
                .catch {
                    emit(
                        SpeechRecognitionUpdate.Error(
                            SpeechRecognitionFailure.CLIENT,
                        ),
                    )
                }
                .collect(::handleRecognitionUpdate)
        }
    }

    fun stop() {
        speechRecognitionGateway.stop()
    }

    fun cancelAndReturn() {
        deactivate()
        effectChannel.trySend(VoiceImeEffect.CancelAndReturn)
    }

    fun deactivate() {
        recognitionJob?.cancel()
        speechRecognitionGateway.cancel()
    }

    override fun onCleared() {
        speechRecognitionGateway.cancel()
        super.onCleared()
    }

    private suspend fun handleRecognitionUpdate(update: SpeechRecognitionUpdate) {
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
                effectChannel.send(
                    VoiceImeEffect.SetComposingText(update.transcript),
                )
            }

            is SpeechRecognitionUpdate.Results -> {
                val transcript = update.results.firstOrNull()
                if (transcript.isNullOrBlank()) {
                    failAndReturn(SpeechRecognitionFailure.NO_MATCH)
                } else {
                    mutableUiState.value = RecognitionUiState.result(transcript)
                    effectChannel.send(
                        VoiceImeEffect.CommitAndReturn(transcript),
                    )
                }
            }

            is SpeechRecognitionUpdate.LanguageDetected -> {
                mutableUiState.update {
                    it.copy(languageTag = update.languageTag)
                }
            }

            is SpeechRecognitionUpdate.Error -> {
                failAndReturn(
                    failure = update.failure,
                    diagnosticCode = update.diagnosticCode,
                )
            }
        }
    }

    private suspend fun failAndReturn(
        failure: SpeechRecognitionFailure,
        diagnosticCode: Int? = null,
    ) {
        val errorState = recognitionErrorState(
            failure = failure,
            diagnosticCode = diagnosticCode,
        )
        mutableUiState.value = errorState
        delay(ERROR_DISPLAY_MILLIS)
        effectChannel.send(
            VoiceImeEffect.FailAndReturn(errorState.titleRes),
        )
    }

    private companion object {
        const val ERROR_DISPLAY_MILLIS = 400L
    }
}
