package io.github.stream29.dashvoice.dashscope

import io.github.stream29.dashvoice.data.DashVoiceSettings
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DashScopeTextPolishRequestTest {
    @Test
    fun includesDefaultValuedWireParameters() {
        val request = DashScopeProtocol.json
            .parseToJsonElement(encodeTextPolishRequest("测试文本", "测试提示词"))
            .jsonObject

        assertEquals(DashVoiceSettings.TEXT_POLISH_MODEL, request.getValue("model").jsonPrimitive.content)
        assertFalse(request.getValue("stream").jsonPrimitive.boolean)
        assertEquals(0.2, request.getValue("temperature").jsonPrimitive.double, 0.0)
        assertFalse(request.getValue("enable_thinking").jsonPrimitive.boolean)
        assertEquals(
            "json_object",
            request.getValue("response_format").jsonObject.getValue("type").jsonPrimitive.content,
        )
    }

    @Test
    fun preservesPromptAndTranscriptInTheirOwnMessages() {
        val prompt = "保持原文。\n只返回{\"text\":\"内容\"}。"
        val transcript = "中文和Kotlin Native，包含\"引号\"。"
        val messages = DashScopeProtocol.json
            .parseToJsonElement(encodeTextPolishRequest(transcript, prompt))
            .jsonObject.getValue("messages").jsonArray

        assertEquals(2, messages.size)
        assertEquals("system", messages[0].jsonObject.getValue("role").jsonPrimitive.content)
        assertEquals(prompt, messages[0].jsonObject.getValue("content").jsonPrimitive.content)
        assertEquals("user", messages[1].jsonObject.getValue("role").jsonPrimitive.content)
        assertEquals(transcript, messages[1].jsonObject.getValue("content").jsonPrimitive.content)
    }

    @Test
    fun leavesAsrSerializationDefaultsUnchanged() {
        assertFalse(DashScopeProtocol.json.configuration.encodeDefaults)
    }
}
