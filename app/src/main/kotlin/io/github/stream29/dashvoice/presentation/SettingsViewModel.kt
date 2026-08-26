package io.github.stream29.dashvoice.presentation

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.stream29.dashvoice.R
import io.github.stream29.dashvoice.data.ConfigurationRepository
import io.github.stream29.dashvoice.data.DashVoiceSettings
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

data class SettingsUiState(
    val isLoading: Boolean = true,
    val apiKey: String = "",
    val baseUrl: String = "",
    val vadThresholdInput: String = DashVoiceSettings.DEFAULT_VAD_THRESHOLD.toString(),
    val silenceDurationMillisInput: String =
        DashVoiceSettings.DEFAULT_SILENCE_DURATION_MILLIS.toString(),
    val removeTrailingSentencePunctuation: Boolean =
        DashVoiceSettings.DEFAULT_REMOVE_TRAILING_SENTENCE_PUNCTUATION,
    val removeSpacesAtCjkBoundaries: Boolean =
        DashVoiceSettings.DEFAULT_REMOVE_SPACES_AT_CJK_BOUNDARIES,
    val semanticPunctuationEnabled: Boolean =
        DashVoiceSettings.DEFAULT_SEMANTIC_PUNCTUATION_ENABLED,
    val textPolishMinimumCharacterCountInput: String =
        DashVoiceSettings.DEFAULT_TEXT_POLISH_MINIMUM_CHARACTER_COUNT.toString(),
    val textPolishPrompt: String = DashVoiceSettings.DEFAULT_TEXT_POLISH_PROMPT,
    val apiKeyVisible: Boolean = false,
    val isSaving: Boolean = false,
    val microphonePermissionGranted: Boolean = false,
    @field:StringRes val statusMessageRes: Int? = null,
    val testRecognition: RecognitionUiState = RecognitionUiState.idle(),
) {
    val settings: DashVoiceSettings
        get() = DashVoiceSettings(
            apiKey = apiKey,
            baseUrl = baseUrl,
            vadThreshold = vadThresholdInput.toDoubleOrNull() ?: Double.NaN,
            silenceDurationMillis = silenceDurationMillisInput.toIntOrNull() ?: Int.MIN_VALUE,
            removeTrailingSentencePunctuation = removeTrailingSentencePunctuation,
            removeSpacesAtCjkBoundaries = removeSpacesAtCjkBoundaries,
            semanticPunctuationEnabled = semanticPunctuationEnabled,
            textPolishMinimumCharacterCount =
                textPolishMinimumCharacterCountInput.toIntOrNull() ?: Int.MIN_VALUE,
            textPolishPrompt = textPolishPrompt,
        )
}

sealed interface SettingsEvent {
    data object RequestMicrophonePermission : SettingsEvent
    data object OpenVoiceInputSettings : SettingsEvent
}

class SettingsViewModel(
    private val configurationRepository: ConfigurationRepository,
    private val speechRecognitionGateway: SpeechRecognitionGateway,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = mutableUiState.asStateFlow()

    private val eventChannel = Channel<SettingsEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private var testRecognitionJob: Job? = null
    private var startRecognitionAfterPermissionResult = false
    private var persistedSettings: DashVoiceSettings? = null

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            mutableUiState.update { it.copy(isLoading = true, statusMessageRes = null) }
            val settings = try {
                configurationRepository.load()
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                mutableUiState.update {
                    it.copy(
                        isLoading = false,
                        statusMessageRes = R.string.settings_error_read_room,
                    )
                }
                return@launch
            }
            persistedSettings = settings
            mutableUiState.update {
                it.copy(
                    isLoading = false,
                    apiKey = settings.apiKey,
                    baseUrl = settings.baseUrl,
                    vadThresholdInput = settings.vadThreshold.toString(),
                    silenceDurationMillisInput = settings.silenceDurationMillis.toString(),
                    removeTrailingSentencePunctuation =
                        settings.removeTrailingSentencePunctuation,
                    removeSpacesAtCjkBoundaries = settings.removeSpacesAtCjkBoundaries,
                    semanticPunctuationEnabled = settings.semanticPunctuationEnabled,
                    textPolishMinimumCharacterCountInput =
                        settings.textPolishMinimumCharacterCount.toString(),
                    textPolishPrompt = settings.textPolishPrompt,
                )
            }
        }
    }

    fun onApiKeyChanged(value: String) {
        mutableUiState.update { it.copy(apiKey = value, statusMessageRes = null) }
    }

    fun onBaseUrlChanged(value: String) {
        mutableUiState.update { it.copy(baseUrl = value, statusMessageRes = null) }
    }

    fun toggleApiKeyVisibility() {
        mutableUiState.update { it.copy(apiKeyVisible = !it.apiKeyVisible) }
    }

    fun onVadThresholdChanged(value: String) {
        mutableUiState.update {
            it.copy(vadThresholdInput = value, statusMessageRes = null)
        }
    }

    fun onSilenceDurationMillisChanged(value: String) {
        mutableUiState.update {
            it.copy(silenceDurationMillisInput = value, statusMessageRes = null)
        }
    }

    fun onRemoveTrailingSentencePunctuationChanged(value: Boolean) {
        mutableUiState.update {
            it.copy(removeTrailingSentencePunctuation = value, statusMessageRes = null)
        }
    }

    fun onRemoveSpacesAtCjkBoundariesChanged(value: Boolean) {
        mutableUiState.update {
            it.copy(removeSpacesAtCjkBoundaries = value, statusMessageRes = null)
        }
    }

    fun onSemanticPunctuationEnabledChanged(value: Boolean) {
        mutableUiState.update {
            it.copy(semanticPunctuationEnabled = value, statusMessageRes = null)
        }
    }

    fun onTextPolishMinimumCharacterCountChanged(value: String) {
        mutableUiState.update {
            it.copy(textPolishMinimumCharacterCountInput = value, statusMessageRes = null)
        }
    }

    fun onTextPolishPromptChanged(value: String) {
        mutableUiState.update {
            it.copy(textPolishPrompt = value, statusMessageRes = null)
        }
    }

    fun onMicrophonePermissionChanged(granted: Boolean) {
        mutableUiState.update { it.copy(microphonePermissionGranted = granted) }
    }

    fun requestMicrophonePermission() {
        startRecognitionAfterPermissionResult = false
        eventChannel.trySend(SettingsEvent.RequestMicrophonePermission)
    }

    fun save() {
        val state = mutableUiState.value
        if (state.isLoading || state.settings.normalized() == persistedSettings) return
        viewModelScope.launch {
            persistCurrentSettings(successMessageRes = R.string.settings_saved)
        }
    }

    fun startTest() {
        if (mutableUiState.value.testRecognition.isActive) return
        viewModelScope.launch {
            if (persistCurrentSettings(successMessageRes = null)) {
                startRecognitionAfterPermissionResult = true
                eventChannel.send(SettingsEvent.RequestMicrophonePermission)
            }
        }
    }

    fun stopTest() {
        speechRecognitionGateway.stop()
    }

    fun onMicrophonePermissionResult(granted: Boolean) {
        val shouldStartRecognition = startRecognitionAfterPermissionResult
        startRecognitionAfterPermissionResult = false
        onMicrophonePermissionChanged(granted)
        if (!granted) {
            if (shouldStartRecognition) {
                mutableUiState.update {
                    it.copy(
                        testRecognition = recognitionErrorState(
                            SpeechRecognitionFailure.PERMISSION,
                        ),
                    )
                }
            }
            return
        }
        if (!shouldStartRecognition) return

        testRecognitionJob?.cancel()
        mutableUiState.update {
            it.copy(testRecognition = RecognitionUiState.connecting())
        }
        testRecognitionJob = viewModelScope.launch {
            speechRecognitionGateway.recognize()
                .catch {
                    emit(SpeechRecognitionUpdate.Error(SpeechRecognitionFailure.CLIENT))
                }
                .collect(::handleRecognitionUpdate)
        }
    }

    fun openVoiceInputSettings() {
        eventChannel.trySend(SettingsEvent.OpenVoiceInputSettings)
    }

    override fun onCleared() {
        speechRecognitionGateway.cancel()
        super.onCleared()
    }

    private fun handleRecognitionUpdate(update: SpeechRecognitionUpdate) {
        when (update) {
            SpeechRecognitionUpdate.Ready -> {
                updateTestRecognition { it.listening() }
            }

            SpeechRecognitionUpdate.BeginningOfSpeech -> {
                updateTestRecognition { it.speaking() }
            }

            is SpeechRecognitionUpdate.RmsChanged -> {
                updateTestRecognition { it.copy(rms = update.rms) }
            }

            SpeechRecognitionUpdate.EndOfSpeech -> {
                updateTestRecognition { it.processing() }
            }

            is SpeechRecognitionUpdate.PartialResult -> {
                updateTestRecognition { it.copy(transcript = update.transcript) }
            }

            is SpeechRecognitionUpdate.Results -> {
                val transcript = update.results.firstOrNull().orEmpty()
                mutableUiState.update {
                    it.copy(
                        testRecognition = if (transcript.isBlank()) {
                            recognitionErrorState(SpeechRecognitionFailure.NO_MATCH)
                        } else {
                            RecognitionUiState.result(transcript)
                        },
                    )
                }
            }

            is SpeechRecognitionUpdate.LanguageDetected -> {
                updateTestRecognition { it.copy(languageTag = update.languageTag) }
            }

            is SpeechRecognitionUpdate.Error -> {
                mutableUiState.update {
                    it.copy(
                        testRecognition = recognitionErrorState(
                            failure = update.failure,
                            diagnosticCode = update.diagnosticCode,
                        ),
                    )
                }
            }
        }
    }

    private suspend fun persistCurrentSettings(
        @StringRes successMessageRes: Int?,
    ): Boolean {
        val settings = mutableUiState.value.settings.normalized()
        if (!settings.isReady) {
            val state = mutableUiState.value
            mutableUiState.update {
                it.copy(
                    statusMessageRes = when {
                        settings.apiKey.isBlank() ->
                            R.string.settings_validation_api_key

                        !DashVoiceSettings.isValidBaseUrl(settings.baseUrl) ->
                            R.string.settings_validation_base_url

                        state.vadThresholdInput.toDoubleOrNull()?.let {
                            it in DashVoiceSettings.MIN_VAD_THRESHOLD..
                                DashVoiceSettings.MAX_VAD_THRESHOLD
                        } != true ->
                            R.string.settings_validation_vad_threshold

                        state.textPolishMinimumCharacterCountInput.toIntOrNull()?.let {
                            it in DashVoiceSettings.MIN_TEXT_POLISH_MINIMUM_CHARACTER_COUNT..
                                DashVoiceSettings.MAX_TEXT_POLISH_MINIMUM_CHARACTER_COUNT
                        } != true ->
                            R.string.settings_validation_text_polish_minimum_characters

                        state.textPolishPrompt.isBlank() ->
                            R.string.settings_validation_text_polish_prompt

                        else ->
                            R.string.settings_validation_silence_duration
                    },
                )
            }
            return false
        }

        mutableUiState.update { it.copy(isSaving = true, statusMessageRes = null) }
        val saved = configurationRepository.save(settings)
        if (saved) {
            persistedSettings = settings
        }
        mutableUiState.update {
            it.copy(
                isSaving = false,
                apiKey = settings.apiKey.trim(),
                baseUrl = settings.baseUrl.trim(),
                vadThresholdInput = settings.vadThreshold.toString(),
                silenceDurationMillisInput = settings.silenceDurationMillis.toString(),
                removeTrailingSentencePunctuation =
                    settings.removeTrailingSentencePunctuation,
                removeSpacesAtCjkBoundaries = settings.removeSpacesAtCjkBoundaries,
                semanticPunctuationEnabled = settings.semanticPunctuationEnabled,
                textPolishMinimumCharacterCountInput =
                    settings.textPolishMinimumCharacterCount.toString(),
                textPolishPrompt = settings.textPolishPrompt,
                statusMessageRes = if (saved) {
                    successMessageRes
                } else {
                    R.string.settings_error_save_room
                },
            )
        }
        return saved
    }

    private fun DashVoiceSettings.normalized(): DashVoiceSettings =
        copy(
            apiKey = apiKey.trim(),
            baseUrl = baseUrl.trim(),
        )

    private fun updateTestRecognition(
        transform: (RecognitionUiState) -> RecognitionUiState,
    ) {
        mutableUiState.update {
            it.copy(testRecognition = transform(it.testRecognition))
        }
    }
}
