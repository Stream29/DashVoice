package io.github.stream29.dashvoice.recognition

internal object TranscriptNormalizer {
    private const val CJK_CHARACTER =
        "[\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}\\p{IsHangul}]"
    private val whitespace = Regex("\\s+")
    private val cjkToLatinOrDigit = Regex("($CJK_CHARACTER)\\s+([A-Za-z0-9])")
    private val latinOrDigitToCjk = Regex("([A-Za-z0-9])\\s+($CJK_CHARACTER)")
    private val trailingSentencePunctuation = Regex("[。！？；：，、…．.!?;:]+$")

    fun normalize(
        text: String,
        removeSpacesAtCjkBoundaries: Boolean = true,
    ): String {
        val normalized = text
            .replace(whitespace, " ")
            .trim()

        return if (removeSpacesAtCjkBoundaries) {
            normalized
                .replace(cjkToLatinOrDigit, "$1$2")
                .replace(latinOrDigitToCjk, "$1$2")
        } else {
            normalized
        }
    }

    fun finalize(
        text: String,
        removeTrailingSentencePunctuation: Boolean,
        removeSpacesAtCjkBoundaries: Boolean,
    ): String {
        val normalized = normalize(text, removeSpacesAtCjkBoundaries)
        return if (removeTrailingSentencePunctuation) {
            normalized.replace(trailingSentencePunctuation, "")
        } else {
            normalized
        }
    }
}
