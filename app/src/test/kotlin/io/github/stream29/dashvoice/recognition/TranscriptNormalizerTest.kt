package io.github.stream29.dashvoice.recognition

import org.junit.Assert.assertEquals
import org.junit.Test

class TranscriptNormalizerTest {
    @Test
    fun removesWhitespaceAtCjkAndLatinBoundaries() {
        assertEquals(
            "我使用Kotlin Native开发Android应用",
            TranscriptNormalizer.normalize("我使用 Kotlin Native 开发 Android 应用"),
        )
    }

    @Test
    fun removesWhitespaceAtCjkAndDigitBoundaries() {
        assertEquals(
            "版本3.0将在2026年发布",
            TranscriptNormalizer.normalize("版本 3.0 将在 2026 年发布"),
        )
    }

    @Test
    fun preservesWhitespaceInsideLatinText() {
        assertEquals(
            "Kotlin Native supports Android",
            TranscriptNormalizer.normalize(" Kotlin   Native supports   Android "),
        )
    }

    @Test
    fun preservesWhitespaceAtCjkAndLatinBoundariesWhenConfigured() {
        assertEquals(
            "我使用 Kotlin Native 开发 Android 应用",
            TranscriptNormalizer.normalize(
                text = "我使用 Kotlin Native 开发 Android 应用",
                removeSpacesAtCjkBoundaries = false,
            ),
        )
    }

    @Test
    fun removesOnlyParagraphTrailingSentencePunctuation() {
        assertEquals(
            "第一句。第二句",
            TranscriptNormalizer.finalize(
                text = "第一句。第二句？！",
                removeTrailingSentencePunctuation = true,
                removeSpacesAtCjkBoundaries = true,
            ),
        )
    }

    @Test
    fun preservesClosingQuotesAndBrackets() {
        assertEquals(
            "他说：“你好。”",
            TranscriptNormalizer.finalize(
                text = "他说：“你好。”",
                removeTrailingSentencePunctuation = true,
                removeSpacesAtCjkBoundaries = true,
            ),
        )
    }

    @Test
    fun preservesTrailingPunctuationWhenConfigured() {
        assertEquals(
            "第一句。第二句？！",
            TranscriptNormalizer.finalize(
                text = "第一句。第二句？！",
                removeTrailingSentencePunctuation = false,
                removeSpacesAtCjkBoundaries = true,
            ),
        )
    }
}
