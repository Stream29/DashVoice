package io.github.stream29.dashvoice.dashscope

import io.github.stream29.dashvoice.data.DashVoiceSettings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashScopeProtocolTest {
    @Test
    fun runTaskUsesAutomaticLanguageAndConfiguredVad() {
        val command = DashScopeProtocol.runTask(
            taskId = "task-id",
            vadThreshold = 0.0,
            silenceDurationMillis = 400,
            semanticPunctuationEnabled = true,
        )

        val encoded = DashScopeProtocol.json.encodeToString(command)
        val root = DashScopeProtocol.json.parseToJsonElement(encoded).jsonObject
        val header = root.getValue("header").jsonObject
        val payload = root.getValue("payload").jsonObject
        val parameters = payload.getValue("parameters").jsonObject

        assertEquals("run-task", header.getValue("action").toString().trim('"'))
        assertEquals("task-id", header.getValue("task_id").toString().trim('"'))
        assertEquals("duplex", header.getValue("streaming").toString().trim('"'))
        assertEquals("audio", payload.getValue("task_group").toString().trim('"'))
        assertEquals("asr", payload.getValue("task").toString().trim('"'))
        assertEquals(
            DashVoiceSettings.MODEL,
            payload.getValue("model").toString().trim('"'),
        )
        assertEquals("pcm", parameters.getValue("format").toString().trim('"'))
        assertEquals("16000", parameters.getValue("sample_rate").toString())
        assertEquals("400", parameters.getValue("max_sentence_silence").toString())
        assertEquals("0.0", parameters.getValue("speech_noise_threshold").toString())
        assertEquals("true", parameters.getValue("semantic_punctuation_enabled").toString())
        assertFalse(payload.containsKey("language_hints"))
        assertTrue(payload.getValue("input").jsonObject.isEmpty())
    }

    @Test
    fun generatedTranscriptIsDeserializedIntoDomainEvent() {
        val event = DashScopeProtocol.parseServerEvent(
            """
            {
              "header": {
                "task_id": "task-id",
                "event": "result-generated",
                "attributes": {}
              },
              "payload": {
                "output": {
                  "sentence": {
                    "text": "你好 Kotlin",
                    "sentence_end": false,
                    "heartbeat": false
                  }
                }
              }
            }
            """.trimIndent(),
        )

        assertEquals(
            DashScopeServerEvent.Transcript(
                text = "你好 Kotlin",
                sentenceEnd = false,
                heartbeat = false,
            ),
            event,
        )
    }

    @Test
    fun failedTaskIsDeserializedIntoDomainError() {
        val event = DashScopeProtocol.parseServerEvent(
            """
            {
              "header": {
                "event": "task-failed",
                "error_code": "CLIENT_ERROR",
                "error_message": "request timed out"
              },
              "payload": {}
            }
            """.trimIndent(),
        )

        assertEquals(
            DashScopeServerEvent.Error(
                code = "CLIENT_ERROR",
                message = "request timed out",
            ),
            event,
        )
    }
}
