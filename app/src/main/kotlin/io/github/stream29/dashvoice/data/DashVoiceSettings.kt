package io.github.stream29.dashvoice.data

import java.net.URI

data class DashVoiceSettings(
    val apiKey: String = "",
    val baseUrl: String = "",
    val vadThreshold: Double = DEFAULT_VAD_THRESHOLD,
    val silenceDurationMillis: Int = DEFAULT_SILENCE_DURATION_MILLIS,
    val removeTrailingSentencePunctuation: Boolean =
        DEFAULT_REMOVE_TRAILING_SENTENCE_PUNCTUATION,
    val removeSpacesAtCjkBoundaries: Boolean =
        DEFAULT_REMOVE_SPACES_AT_CJK_BOUNDARIES,
    val semanticPunctuationEnabled: Boolean =
        DEFAULT_SEMANTIC_PUNCTUATION_ENABLED,
) {
    val hasValidVadConfiguration: Boolean
        get() = vadThreshold in MIN_VAD_THRESHOLD..MAX_VAD_THRESHOLD &&
            silenceDurationMillis in
            MIN_SILENCE_DURATION_MILLIS..MAX_SILENCE_DURATION_MILLIS

    val isReady: Boolean
        get() = apiKey.isNotBlank() &&
            isValidBaseUrl(baseUrl) &&
            hasValidVadConfiguration

    companion object {
        const val MODEL = "qwen-audio-3.0-asr-flash-streaming"
        const val DEFAULT_VAD_THRESHOLD = 0.0
        const val MIN_VAD_THRESHOLD = -1.0
        const val MAX_VAD_THRESHOLD = 1.0
        const val DEFAULT_SILENCE_DURATION_MILLIS = 400
        const val MIN_SILENCE_DURATION_MILLIS = 200
        const val MAX_SILENCE_DURATION_MILLIS = 6_000
        const val DEFAULT_REMOVE_TRAILING_SENTENCE_PUNCTUATION = true
        const val DEFAULT_REMOVE_SPACES_AT_CJK_BOUNDARIES = true
        const val DEFAULT_SEMANTIC_PUNCTUATION_ENABLED = true

        fun isValidBaseUrl(value: String): Boolean = runCatching {
            val uri = URI(value.trim())
            uri.scheme.equals("wss", ignoreCase = true) &&
                !uri.host.isNullOrBlank() &&
                uri.userInfo == null
        }.getOrDefault(false)
    }
}
