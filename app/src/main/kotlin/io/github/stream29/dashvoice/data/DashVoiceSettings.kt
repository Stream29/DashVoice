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
    val textPolishMinimumCharacterCount: Int =
        DEFAULT_TEXT_POLISH_MINIMUM_CHARACTER_COUNT,
    val textPolishPrompt: String = DEFAULT_TEXT_POLISH_PROMPT,
) {
    val hasValidVadConfiguration: Boolean
        get() = vadThreshold in MIN_VAD_THRESHOLD..MAX_VAD_THRESHOLD &&
            silenceDurationMillis in
            MIN_SILENCE_DURATION_MILLIS..MAX_SILENCE_DURATION_MILLIS

    val isReady: Boolean
        get() = apiKey.isNotBlank() &&
            isValidBaseUrl(baseUrl) &&
            hasValidVadConfiguration &&
            hasValidTextPolishConfiguration

    val hasValidTextPolishConfiguration: Boolean
        get() = textPolishMinimumCharacterCount in
            MIN_TEXT_POLISH_MINIMUM_CHARACTER_COUNT..MAX_TEXT_POLISH_MINIMUM_CHARACTER_COUNT &&
            textPolishPrompt.isNotBlank()

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
        const val TEXT_POLISH_MODEL = "qwen-flash"
        const val DEFAULT_TEXT_POLISH_MINIMUM_CHARACTER_COUNT = 20
        const val MIN_TEXT_POLISH_MINIMUM_CHARACTER_COUNT = 1
        const val MAX_TEXT_POLISH_MINIMUM_CHARACTER_COUNT = 1_000
        val DEFAULT_TEXT_POLISH_PROMPT = """
            你是中文和英文语音输入的文本润色器。
            根据上下文修正语音识别中的同音字、语法、数字写法和中英文空格；
            特别注意正确使用“的、地、得”。
            保持原意，不新增事实，不解释，不使用 Markdown。
            只返回 JSON 对象：{"text":"润色后的最终文本"}。
        """.trimIndent()

        fun isValidBaseUrl(value: String): Boolean = runCatching {
            val uri = URI(value.trim())
            uri.scheme.equals("wss", ignoreCase = true) &&
                !uri.host.isNullOrBlank() &&
                uri.userInfo == null
        }.getOrDefault(false)
    }
}
