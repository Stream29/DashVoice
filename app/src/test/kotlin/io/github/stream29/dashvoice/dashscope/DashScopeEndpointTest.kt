package io.github.stream29.dashvoice.dashscope

import io.github.stream29.dashvoice.data.DashVoiceSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class DashScopeEndpointTest {
    @Test
    fun secureWebSocketBaseUrlRemainsAWebSocketUrl() {
        val endpoint = dashScopeEndpoint(
            "wss://dashscope.example.com/api-ws/v1/realtime",
        )

        assertEquals("wss", endpoint.protocol.name)
        assertEquals("dashscope.example.com", endpoint.host)
        assertEquals("/api-ws/v1/realtime", endpoint.encodedPath)
        assertEquals(DashVoiceSettings.MODEL, endpoint.parameters["model"])
    }

    @Test
    fun modelQueryParameterIsReplacedWithoutDroppingOtherParameters() {
        val endpoint = dashScopeEndpoint(
            "wss://dashscope.example.com/realtime?workspace=test&model=old",
        )

        assertEquals("test", endpoint.parameters["workspace"])
        assertEquals(DashVoiceSettings.MODEL, endpoint.parameters["model"])
        assertEquals(1, endpoint.parameters.getAll("model")?.size)
    }
}
