package io.github.stream29.dashvoice.data

import java.net.URI

data class DashVoiceSettings(
    val apiKey: String = "",
    val baseUrl: String = "",
    val vadThreshold: Double = DEFAULT_VAD_THRESHOLD,
    val silenceDurationMillis: Int = DEFAULT_SILENCE_DURATION_MILLIS,
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
        const val MODEL = "qwen3-asr-flash-realtime"
        const val DEFAULT_VAD_THRESHOLD = 0.0
        const val MIN_VAD_THRESHOLD = -1.0
        const val MAX_VAD_THRESHOLD = 1.0
        const val DEFAULT_SILENCE_DURATION_MILLIS = 400
        const val MIN_SILENCE_DURATION_MILLIS = 200
        const val MAX_SILENCE_DURATION_MILLIS = 6_000

        fun isValidBaseUrl(value: String): Boolean = runCatching {
            val uri = URI(value.trim())
            uri.scheme.equals("wss", ignoreCase = true) &&
                !uri.host.isNullOrBlank() &&
                uri.userInfo == null
        }.getOrDefault(false)
    }
}
