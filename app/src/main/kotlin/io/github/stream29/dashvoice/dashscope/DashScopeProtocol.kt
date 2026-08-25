package io.github.stream29.dashvoice.dashscope

import io.github.stream29.dashvoice.data.DashVoiceSettings
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

    fun newTaskId(): String = UUID.randomUUID().toString()

    fun runTask(
        taskId: String,
        vadThreshold: Double,
        silenceDurationMillis: Int,
    ) = DashScopeClientCommand(
        header = DashScopeRequestHeader(
            action = "run-task",
            taskId = taskId,
            streaming = "duplex",
        ),
        payload = DashScopeRequestPayload(
            taskGroup = "audio",
            task = "asr",
            function = "recognition",
            model = DashVoiceSettings.MODEL,
            parameters = DashScopeRecognitionParameters(
                format = "pcm",
                sampleRate = SAMPLE_RATE_HZ,
                maxSentenceSilence = silenceDurationMillis,
                speechNoiseThreshold = vadThreshold,
            ),
            input = DashScopeTaskInput(),
        ),
    )

    fun finishTask(taskId: String) = DashScopeClientCommand(
        header = DashScopeRequestHeader(
            action = "finish-task",
            taskId = taskId,
            streaming = "duplex",
        ),
        payload = DashScopeRequestPayload(input = DashScopeTaskInput()),
    )

    fun parseServerEvent(message: String): DashScopeServerEvent {
        val event = json.decodeFromString<DashScopeServerEventEnvelope>(message)
        return when (event.header.event) {
            "task-started" -> DashScopeServerEvent.TaskStarted
            "result-generated" -> {
                val sentence = event.payload.output?.sentence
                    ?: return DashScopeServerEvent.Other(event.header.event)
                DashScopeServerEvent.Transcript(
                    text = sentence.text,
                    sentenceEnd = sentence.sentenceEnd,
                    heartbeat = sentence.heartbeat,
                )
            }

            "task-finished" -> DashScopeServerEvent.TaskFinished
            "task-failed" -> DashScopeServerEvent.Error(
                code = event.header.errorCode,
                message = event.header.errorMessage,
            )

            else -> DashScopeServerEvent.Other(event.header.event)
        }
    }

    const val SAMPLE_RATE_HZ = 16_000
}

@Serializable
internal data class DashScopeClientCommand(
    val header: DashScopeRequestHeader,
    val payload: DashScopeRequestPayload,
)

@Serializable
internal data class DashScopeRequestHeader(
    val action: String,
    @SerialName("task_id")
    val taskId: String,
    val streaming: String,
)

@Serializable
internal data class DashScopeRequestPayload(
    @SerialName("task_group")
    val taskGroup: String = "",
    val task: String = "",
    val function: String = "",
    val model: String = "",
    val parameters: DashScopeRecognitionParameters? = null,
    val input: DashScopeTaskInput,
)

@Serializable
internal class DashScopeTaskInput

@Serializable
internal data class DashScopeRecognitionParameters(
    val format: String,
    @SerialName("sample_rate")
    val sampleRate: Int,
    @SerialName("max_sentence_silence")
    val maxSentenceSilence: Int,
    @SerialName("speech_noise_threshold")
    val speechNoiseThreshold: Double,
)

@Serializable
private data class DashScopeServerEventEnvelope(
    val header: DashScopeResponseHeader,
    val payload: DashScopeResponsePayload = DashScopeResponsePayload(),
)

@Serializable
private data class DashScopeResponseHeader(
    val event: String = "",
    @SerialName("error_code")
    val errorCode: String? = null,
    @SerialName("error_message")
    val errorMessage: String? = null,
)

@Serializable
private data class DashScopeResponsePayload(
    val output: DashScopeRecognitionOutput? = null,
)

@Serializable
private data class DashScopeRecognitionOutput(
    val sentence: DashScopeSentence? = null,
)

@Serializable
private data class DashScopeSentence(
    val text: String = "",
    val heartbeat: Boolean = false,
    @SerialName("sentence_end")
    val sentenceEnd: Boolean = false,
)

internal sealed interface DashScopeServerEvent {
    data object TaskStarted : DashScopeServerEvent
    data class Transcript(
        val text: String,
        val sentenceEnd: Boolean,
        val heartbeat: Boolean,
    ) : DashScopeServerEvent

    data object TaskFinished : DashScopeServerEvent
    data class Error(
        val code: String?,
        val message: String?,
    ) : DashScopeServerEvent

    data class Other(val type: String) : DashScopeServerEvent
}
