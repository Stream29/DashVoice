package io.github.stream29.dashvoice.dashscope

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DashScopeEndpointTest {
    @Test
    fun secureWebSocketBaseUrlUsesInferenceEndpoint() {
        val endpoint = dashScopeEndpoint(
            "wss://dashscope.example.com/api-ws/v1/realtime",
        )

        assertEquals("wss", endpoint.protocol.name)
        assertEquals("dashscope.example.com", endpoint.host)
        assertEquals("/api-ws/v1/inference", endpoint.encodedPath)
        assertNull(endpoint.parameters["model"])
    }

    @Test
    fun modelQueryParameterIsRemovedWithoutDroppingOtherParameters() {
        val endpoint = dashScopeEndpoint(
            "wss://dashscope.example.com/realtime?workspace=test&model=old",
        )

        assertEquals("test", endpoint.parameters["workspace"])
        assertNull(endpoint.parameters["model"])
    }
}
