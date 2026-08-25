package io.github.stream29.dashvoice.recognition

internal object TranscriptNormalizer {
    private const val CJK_CHARACTER =
        "[\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}\\p{IsHangul}]"
    private val whitespace = Regex("\\s+")
    private val cjkToLatinOrDigit = Regex("($CJK_CHARACTER)\\s+([A-Za-z0-9])")
    private val latinOrDigitToCjk = Regex("([A-Za-z0-9])\\s+($CJK_CHARACTER)")

    fun normalize(text: String): String =
        text
            .replace(whitespace, " ")
            .trim()
            .replace(cjkToLatinOrDigit, "$1$2")
            .replace(latinOrDigitToCjk, "$1$2")
}
