package io.github.stream29.dashvoice.dashscope

import android.util.Base64
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

internal object DashScopeProtocol {
    val json = Json {
        encodeDefaults = false
        explicitNulls = false
        ignoreUnknownKeys = true
    }

    fun sessionUpdate(
        vadThreshold: Double,
        silenceDurationMillis: Int,
    ) = DashScopeClientEvent(
        eventId = eventId(),
        type = "session.update",
        session = DashScopeSessionConfiguration(
            inputAudioFormat = "pcm",
            sampleRate = SAMPLE_RATE_HZ,
            inputAudioTranscription = DashScopeInputAudioTranscription(),
            turnDetection = DashScopeTurnDetection(
                type = "server_vad",
                threshold = vadThreshold,
                silenceDurationMillis = silenceDurationMillis,
            ),
        ),
    )

    fun appendAudio(
        audio: ByteArray,
        length: Int,
    ) = DashScopeClientEvent(
        eventId = eventId(),
        type = "input_audio_buffer.append",
        audio = Base64.encodeToString(
            audio,
            0,
            length,
            Base64.NO_WRAP,
        ),
    )

    fun finish() = DashScopeClientEvent(
        eventId = eventId(),
        type = "session.finish",
    )

    fun parseServerEvent(message: String): DashScopeServerEvent {
        val event = json.decodeFromString<DashScopeServerEventEnvelope>(message)
        return when (event.type) {
            "session.created" -> DashScopeServerEvent.SessionCreated
            "session.updated" -> DashScopeServerEvent.SessionUpdated
            "input_audio_buffer.speech_started" -> DashScopeServerEvent.SpeechStarted
            "input_audio_buffer.speech_stopped" -> DashScopeServerEvent.SpeechStopped
            "conversation.item.input_audio_transcription.text" -> {
                DashScopeServerEvent.PartialTranscript(
                    text = event.text,
                    stash = event.stash,
                    language = event.language,
                )
            }

            "conversation.item.input_audio_transcription.completed" -> {
                DashScopeServerEvent.CompletedTranscript(
                    transcript = event.transcript,
                    language = event.language,
                )
            }

            "conversation.item.input_audio_transcription.failed",
            "error",
                -> {
                DashScopeServerEvent.Error(
                    code = event.error?.code,
                    message = event.error?.message,
                )
            }

            "session.finished" -> DashScopeServerEvent.SessionFinished
            else -> DashScopeServerEvent.Other(event.type)
        }
    }

    private fun eventId(): String =
        "event_${UUID.randomUUID().toString().replace("-", "")}"

    const val SAMPLE_RATE_HZ = 16_000
}

@Serializable
internal data class DashScopeClientEvent(
    @SerialName("event_id")
    val eventId: String,
    val type: String,
    val session: DashScopeSessionConfiguration? = null,
    val audio: String? = null,
)

@Serializable
internal data class DashScopeSessionConfiguration(
    @SerialName("input_audio_format")
    val inputAudioFormat: String,
    @SerialName("sample_rate")
    val sampleRate: Int,
    @SerialName("input_audio_transcription")
    val inputAudioTranscription: DashScopeInputAudioTranscription,
    @SerialName("turn_detection")
    val turnDetection: DashScopeTurnDetection,
)

@Serializable
internal class DashScopeInputAudioTranscription

@Serializable
internal data class DashScopeTurnDetection(
    val type: String,
    val threshold: Double,
    @SerialName("silence_duration_ms")
    val silenceDurationMillis: Int,
)

@Serializable
private data class DashScopeServerEventEnvelope(
    val type: String,
    val text: String = "",
    val stash: String = "",
    val transcript: String = "",
    val language: String? = null,
    val error: DashScopeErrorPayload? = null,
)

@Serializable
private data class DashScopeErrorPayload(
    val code: String? = null,
    val message: String? = null,
)

internal sealed interface DashScopeServerEvent {
    data object SessionCreated : DashScopeServerEvent
    data object SessionUpdated : DashScopeServerEvent
    data object SpeechStarted : DashScopeServerEvent
    data object SpeechStopped : DashScopeServerEvent

    data class PartialTranscript(
        val text: String,
        val stash: String,
        val language: String?,
    ) : DashScopeServerEvent

    data class CompletedTranscript(
        val transcript: String,
        val language: String?,
    ) : DashScopeServerEvent

    data class Error(
        val code: String?,
        val message: String?,
    ) : DashScopeServerEvent

    data object SessionFinished : DashScopeServerEvent
    data class Other(val type: String) : DashScopeServerEvent
}
