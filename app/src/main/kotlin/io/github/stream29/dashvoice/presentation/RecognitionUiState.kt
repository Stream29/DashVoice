package io.github.stream29.dashvoice.presentation

import androidx.annotation.StringRes
import io.github.stream29.dashvoice.R
import io.github.stream29.dashvoice.recognition.SpeechRecognitionFailure

data class RecognitionUiState(
    val phase: RecognitionPhase,
    @field:StringRes val titleRes: Int,
    @field:StringRes val guidanceRes: Int? = null,
    val transcript: String = "",
    val languageTag: String? = null,
    val rms: Float = 0f,
    val isConfigurationError: Boolean = false,
    val diagnosticCode: Int? = null,
) {
    val isActive: Boolean
        get() = phase == RecognitionPhase.CONNECTING ||
            phase == RecognitionPhase.LISTENING ||
            phase == RecognitionPhase.SPEAKING ||
            phase == RecognitionPhase.PROCESSING

    fun listening() = copy(
        phase = RecognitionPhase.LISTENING,
        titleRes = R.string.recognition_title_listening,
        guidanceRes = R.string.recognition_guidance_automatic_language,
    )

    fun speaking() = copy(
        phase = RecognitionPhase.SPEAKING,
        titleRes = R.string.recognition_title_speaking,
        guidanceRes = R.string.recognition_guidance_auto_finish,
    )

    fun processing() = copy(
        phase = RecognitionPhase.PROCESSING,
        titleRes = R.string.recognition_title_processing,
        guidanceRes = null,
        rms = 0f,
    )

    companion object {
        fun idle() = RecognitionUiState(
            phase = RecognitionPhase.IDLE,
            titleRes = R.string.recognition_title_idle,
            guidanceRes = R.string.recognition_guidance_idle,
        )

        fun connecting() = RecognitionUiState(
            phase = RecognitionPhase.CONNECTING,
            titleRes = R.string.recognition_title_connecting,
            guidanceRes = R.string.recognition_guidance_connecting,
        )

        fun result(transcript: String) = RecognitionUiState(
            phase = RecognitionPhase.RESULT,
            titleRes = R.string.recognition_title_success,
            transcript = transcript,
        )

        fun error(
            @StringRes messageRes: Int,
            @StringRes guidanceRes: Int? = null,
            configurationError: Boolean = false,
            diagnosticCode: Int? = null,
        ) = RecognitionUiState(
            phase = RecognitionPhase.ERROR,
            titleRes = messageRes,
            guidanceRes = guidanceRes,
            isConfigurationError = configurationError,
            diagnosticCode = diagnosticCode,
        )
    }
}

enum class RecognitionPhase {
    IDLE,
    CONNECTING,
    LISTENING,
    SPEAKING,
    PROCESSING,
    RESULT,
    ERROR,
}

fun recognitionErrorState(
    failure: SpeechRecognitionFailure,
    diagnosticCode: Int? = null,
): RecognitionUiState = when (failure) {
    SpeechRecognitionFailure.PERMISSION ->
        RecognitionUiState.error(
            messageRes = R.string.recognition_error_permission,
            guidanceRes = R.string.recognition_error_permission_guidance,
        )

    SpeechRecognitionFailure.NETWORK -> RecognitionUiState.error(
        messageRes = R.string.recognition_error_network,
        guidanceRes = R.string.recognition_error_network_guidance,
    )

    SpeechRecognitionFailure.SERVER ->
        RecognitionUiState.error(
            messageRes = R.string.recognition_error_server,
            guidanceRes = R.string.recognition_error_server_guidance,
            configurationError = true,
        )

    SpeechRecognitionFailure.AUDIO ->
        RecognitionUiState.error(
            messageRes = R.string.recognition_error_audio,
            guidanceRes = R.string.recognition_error_audio_guidance,
        )

    SpeechRecognitionFailure.SPEECH_TIMEOUT ->
        RecognitionUiState.error(
            messageRes = R.string.recognition_error_speech_timeout,
            guidanceRes = R.string.recognition_error_speech_timeout_guidance,
        )

    SpeechRecognitionFailure.NO_MATCH ->
        RecognitionUiState.error(
            messageRes = R.string.recognition_error_no_match,
            guidanceRes = R.string.recognition_error_no_match_guidance,
        )

    SpeechRecognitionFailure.BUSY ->
        RecognitionUiState.error(
            messageRes = R.string.recognition_error_busy,
            guidanceRes = R.string.recognition_error_busy_guidance,
        )

    SpeechRecognitionFailure.LANGUAGE ->
        RecognitionUiState.error(
            messageRes = R.string.recognition_error_language,
            guidanceRes = R.string.recognition_error_language_guidance,
        )

    SpeechRecognitionFailure.CLIENT ->
        RecognitionUiState.error(
            messageRes = R.string.recognition_error_client,
            guidanceRes = R.string.recognition_error_client_guidance,
        )

    SpeechRecognitionFailure.OTHER ->
        RecognitionUiState.error(
            messageRes = R.string.recognition_error_other,
            guidanceRes = diagnosticCode?.let { R.string.recognition_error_code },
            diagnosticCode = diagnosticCode,
        )
}
