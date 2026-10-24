package io.github.stream29.dashvoice.recognition

import org.junit.Assert.assertEquals
import org.junit.Test

class TranscriptNormalizerTest {
    @Test
    fun removesEverySpaceBetweenHanWords() {
        assertEquals(
            "我今天想测试中文输入",
            TranscriptNormalizer.normalize("我 今天 想 测试 中 文 输 入"),
        )
    }

    @Test
    fun removesWhitespaceAndUnicodeSpacesBetweenHanCharacters() {
        assertEquals(
            "今天我们测试中文",
            TranscriptNormalizer.normalize("今\t天\n我们\u00a0测试\u3000中文"),
        )
    }

    @Test
    fun removesHanSpacesWithoutRemovingEnglishWordSpaces() {
        assertEquals(
            "今天测试Kotlin Native和中文输入",
            TranscriptNormalizer.normalize("今天 测试 Kotlin Native 和 中文 输入"),
        )
    }

    @Test
    fun preservesKoreanWordSpaces() {
        assertEquals(
            "한국어 문장 사이 공백",
            TranscriptNormalizer.normalize("한국어 문장 사이 공백"),
        )
    }

    @Test
    fun preservesHanSpacesWhenFormattingIsDisabled() {
        assertEquals(
            "我 今天 想 测试 中文 输入",
            TranscriptNormalizer.normalize(
                "我 今天 想 测试 中文 输入",
                removeSpacesAtCjkBoundaries = false,
            ),
        )
    }

    @Test
    fun finalizesSpacedChineseAfterPolishing() {
        assertEquals(
            "今天我们测试中文输入",
            TranscriptNormalizer.finalize(
                "今天 我们 测试 中文 输入。",
                removeTrailingSentencePunctuation = true,
                removeSpacesAtCjkBoundaries = true,
            ),
        )
    }

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
