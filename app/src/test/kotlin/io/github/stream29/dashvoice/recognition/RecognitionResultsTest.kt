package io.github.stream29.dashvoice.recognition

import org.junit.Assert.assertEquals
import org.junit.Test

class RecognitionResultsTest {
    @Test
    fun dashScopeLanguageCodesBecomeBcp47Tags() {
        assertEquals("zh-CN", dashScopeLanguageToBcp47("zh"))
        assertEquals("ja-JP", dashScopeLanguageToBcp47("ja"))
        assertEquals("en-US", dashScopeLanguageToBcp47("en"))
        assertEquals("yue-HK", dashScopeLanguageToBcp47("yue"))
    }

    @Test
    fun unknownLanguageCodeIsPreserved() {
        assertEquals("custom", dashScopeLanguageToBcp47("custom"))
    }
}
