package io.github.stream29.dashvoice.recognition

internal object TranscriptPolishing {
    fun shouldPolish(
        transcript: String,
        minimumCharacterCount: Int,
    ): Boolean = effectiveCharacterCount(transcript) >= minimumCharacterCount

    fun effectiveCharacterCount(text: String): Int =
        text.count(Char::isLetterOrDigit)
}
