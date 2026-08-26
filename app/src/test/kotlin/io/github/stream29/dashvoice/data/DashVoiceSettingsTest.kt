package io.github.stream29.dashvoice.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashVoiceSettingsTest {
    @Test
    fun transcriptFormattingDefaultsPreserveExistingBehavior() {
        val settings = DashVoiceSettings()

        assertTrue(settings.removeTrailingSentencePunctuation)
        assertTrue(settings.removeSpacesAtCjkBoundaries)
        assertTrue(settings.semanticPunctuationEnabled)
    }

    @Test
    fun readyConfigurationRequiresApiKeyAndSecureWebSocketUrl() {
        assertTrue(
            DashVoiceSettings(
                apiKey = "sk-test",
                baseUrl = "wss://dashscope.example.com/api-ws/v1/realtime",
            ).isReady,
        )
        assertFalse(
            DashVoiceSettings(
                apiKey = "",
                baseUrl = "wss://dashscope.example.com/api-ws/v1/realtime",
            ).isReady,
        )
        assertFalse(
            DashVoiceSettings(
                apiKey = "sk-test",
                baseUrl = "https://dashscope.example.com/api-ws/v1/realtime",
            ).isReady,
        )
    }

    @Test
    fun baseUrlRejectsMissingHostAndEmbeddedCredentials() {
        assertFalse(DashVoiceSettings.isValidBaseUrl("wss:///api-ws/v1/realtime"))
        assertFalse(
            DashVoiceSettings.isValidBaseUrl(
                "wss://user:password@dashscope.example.com/api-ws/v1/realtime",
            ),
        )
    }

    @Test
    fun baseUrlAllowsSurroundingWhitespace() {
        assertTrue(
            DashVoiceSettings.isValidBaseUrl(
                "  wss://dashscope.example.com/api-ws/v1/realtime  ",
            ),
        )
    }

    @Test
    fun vadConfigurationMustStayWithinDashScopeRanges() {
        assertTrue(
            DashVoiceSettings(
                vadThreshold = -1.0,
                silenceDurationMillis = 6_000,
            ).hasValidVadConfiguration,
        )
        assertFalse(
            DashVoiceSettings(
                vadThreshold = -1.01,
                silenceDurationMillis = 400,
            ).hasValidVadConfiguration,
        )
        assertFalse(
            DashVoiceSettings(
                vadThreshold = 0.2,
                silenceDurationMillis = 199,
            ).hasValidVadConfiguration,
        )
    }
}
