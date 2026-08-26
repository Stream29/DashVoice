package io.github.stream29.dashvoice.recognition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptPolishingTest {
    @Test
    fun effectiveCharacterCountExcludesWhitespaceAndPunctuation() {
        assertEquals(
            20,
            TranscriptPolishing.effectiveCharacterCount("这是一段刚好二十个有效字符的输入，用来测试！"),
        )
    }

    @Test
    fun polishingStartsAtTheConfiguredThreshold() {
        assertTrue(
            TranscriptPolishing.shouldPolish(
                transcript = "这是一段刚好二十个有效字符的输入，用来测试！",
                minimumCharacterCount = 20,
            ),
        )
        assertFalse(
            TranscriptPolishing.shouldPolish(
                transcript = "短句",
                minimumCharacterCount = 20,
            ),
        )
    }
}
