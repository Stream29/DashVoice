package io.github.stream29.dashvoice.dashscope

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DashScopeProtocolTest {
    @Test
    fun automaticLanguageIsOmittedFromSerializedSessionUpdate() {
        val update = DashScopeProtocol.sessionUpdate(
            vadThreshold = 0.0,
            silenceDurationMillis = 400,
        )

        val encoded = DashScopeProtocol.json.encodeToString(update)
        val root = DashScopeProtocol.json.parseToJsonElement(encoded).jsonObject
        val session = root.getValue("session").jsonObject
        val transcription = session
            .getValue("input_audio_transcription")
            .jsonObject
        val turnDetection = session
            .getValue("turn_detection")
            .jsonObject

        assertEquals("session.update", root.getValue("type").toString().trim('"'))
        assertEquals("pcm", session.getValue("input_audio_format").toString().trim('"'))
        assertEquals("16000", session.getValue("sample_rate").toString())
        assertFalse(transcription.containsKey("language"))
        assertEquals("0.0", turnDetection.getValue("threshold").toString())
        assertEquals("400", turnDetection.getValue("silence_duration_ms").toString())
    }

    @Test
    fun partialTranscriptIsDeserializedIntoDomainEvent() {
        val event = DashScopeProtocol.parseServerEvent(
            """
            {
              "type": "conversation.item.input_audio_transcription.text",
              "text": "你好",
              "stash": "世界",
              "language": "zh",
              "ignored": true
            }
            """.trimIndent(),
        )

        assertEquals(
            DashScopeServerEvent.PartialTranscript(
                text = "你好",
                stash = "世界",
                language = "zh",
            ),
            event,
        )
    }
}
