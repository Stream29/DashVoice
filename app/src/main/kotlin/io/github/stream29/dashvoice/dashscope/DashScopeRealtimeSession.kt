package io.github.stream29.dashvoice.dashscope

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import io.github.stream29.dashvoice.data.DashVoiceSettings
import io.github.stream29.dashvoice.recognition.TranscriptNormalizer
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.all
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import java.io.IOException
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

internal class DashScopeRealtimeSession(
    private val recordingContext: Context,
    private val settings: DashVoiceSettings,
) {
    private val commands = Channel<Command>(Channel.CONFLATED)

    val events: Flow<Event> = channelFlow {
        try {
            runRealtimeSession(this)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            send(
                Event.Failure(
                    kind = exception.failureKind(),
                    detail = exception.failureDetail(),
                ),
            )
        }
    }.flowOn(Dispatchers.IO)

    fun finish() {
        commands.trySend(Command.Finish)
    }

    fun cancel() {
        commands.trySend(Command.Cancel)
    }

    private suspend fun runRealtimeSession(events: ProducerScope<Event>) {
        coroutineScope {
            val audioCapture = startAudioCapture(events)
            try {
                val socket = httpClient.webSocketSession(
                    urlString = dashScopeEndpoint(settings.baseUrl).toString(),
                ) {
                    header(HttpHeaders.Authorization, "Bearer ${settings.apiKey}")
                    header(HttpHeaders.UserAgent, "DashVoice/0.1.0 Android")
                }
                try {
                    val taskId = DashScopeProtocol.newTaskId()
                    val outgoing = Channel<OutboundMessage>(Channel.UNLIMITED)
                    val writer = launch {
                        outgoing.receiveAsFlow().collect { message ->
                            when (message) {
                                is OutboundMessage.Command -> socket.sendSerialized(message.command)
                                is OutboundMessage.Audio -> {
                                    socket.send(Frame.Binary(fin = true, data = message.bytes))
                                }
                            }
                        }
                    }
                    try {
                        outgoing.send(
                            OutboundMessage.Command(
                                DashScopeProtocol.runTask(
                                    taskId = taskId,
                                    vadThreshold = settings.vadThreshold,
                                    silenceDurationMillis = settings.silenceDurationMillis,
                                ),
                            ),
                        )
                        when (runEventLoop(socket, events, outgoing, audioCapture, taskId)) {
                            LoopEnd.FINISHED -> {
                                outgoing.close()
                                writer.join()
                                socket.close(
                                    CloseReason(
                                        code = CloseReason.Codes.NORMAL,
                                        message = "session finished",
                                    ),
                                )
                            }

                            LoopEnd.CANCELED -> {
                                outgoing.cancel()
                                writer.cancelAndJoin()
                            }
                        }
                    } finally {
                        outgoing.cancel()
                        writer.cancelAndJoin()
                    }
                } finally {
                    socket.cancel()
                }
            } finally {
                audioCapture.cancel()
            }
        }
    }

    private fun CoroutineScope.startAudioCapture(
        events: SendChannel<Event>,
    ): AudioCapture {
        val frames = Channel<AudioFrame.Data>(
            capacity = PRE_ROLL_CHUNK_COUNT,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
        val job = launch {
            try {
                audioFrames().collect { frame ->
                    when (frame) {
                        AudioFrame.Ready -> events.send(Event.Ready)
                        is AudioFrame.Data -> {
                            events.trySend(Event.RmsChanged(frame.rms))
                            frames.send(frame)
                        }
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                throw AudioCaptureException(exception)
            } finally {
                frames.close()
            }
        }
        return AudioCapture(frames = frames, job = job)
    }

    private suspend fun CoroutineScope.runEventLoop(
        socket: DefaultClientWebSocketSession,
        events: SendChannel<Event>,
        outgoing: SendChannel<OutboundMessage>,
        audioCapture: AudioCapture,
        taskId: String,
    ): LoopEnd {
        var state = SessionState()
        var audioSenderJob: Job? = null
        var loopEnd = LoopEnd.CANCELED

        fun startAudioSender() {
            if (!state.configured || audioSenderJob != null) return
            audioSenderJob = launch {
                for (frame in audioCapture.frames) {
                    outgoing.send(
                        OutboundMessage.Audio(
                            bytes = frame.bytes,
                        ),
                    )
                }
            }
        }

        suspend fun requestFinish() {
            state = state.copy(finishRequested = true)
            audioCapture.stop()
            if (state.configured && !state.finishSent) {
                startAudioSender()
                audioSenderJob?.join()
                outgoing.send(
                    OutboundMessage.Command(
                        DashScopeProtocol.finishTask(taskId),
                    ),
                )
                state = state.copy(finishSent = true)
            }
        }

        try {
            merge(
                socket.serverInputs(),
                commands.receiveAsFlow().map(SessionInput::CommandReceived),
            ).all { input ->
                when (input) {
                    is SessionInput.CommandReceived -> {
                        when (input.command) {
                            Command.Finish -> requestFinish()
                            Command.Cancel -> {
                                loopEnd = LoopEnd.CANCELED
                                return@all false
                            }
                        }
                    }

                    is SessionInput.ServerEvent -> {
                        when (val event = input.event) {
                            DashScopeServerEvent.TaskStarted -> {
                                state = state.copy(configured = true)
                                startAudioSender()
                                if (state.finishRequested) {
                                    requestFinish()
                                }
                            }

                            is DashScopeServerEvent.Transcript -> {
                                if (event.heartbeat) return@all true
                                if (event.text.isNotBlank() && !state.speechDetected) {
                                    state = state.copy(speechDetected = true)
                                    events.send(Event.BeginningOfSpeech)
                                }
                                if (event.sentenceEnd) {
                                    state = state.appendCompleted(event.text)
                                }
                                events.send(
                                    Event.PartialTranscript(
                                        transcript = if (event.sentenceEnd) {
                                            state.completedText
                                        } else {
                                            TranscriptNormalizer.normalize(
                                                state.completedText + event.text,
                                            )
                                        },
                                        language = null,
                                    ),
                                )
                                if (event.sentenceEnd) {
                                    events.send(Event.EndOfSpeech)
                                    requestFinish()
                                }
                            }

                            is DashScopeServerEvent.Error -> {
                                val detail = listOfNotNull(event.code, event.message)
                                    .joinToString(": ")
                                    .ifBlank { "DashScope returned an error" }
                                throw DashScopeServerException(detail)
                            }

                            DashScopeServerEvent.TaskFinished -> {
                                audioCapture.stop()
                                audioSenderJob?.join()
                                events.send(
                                    Event.Completed(
                                        transcript = state.completedText
                                            .takeIf { it.isNotBlank() },
                                        language = null,
                                        speechDetected = state.speechDetected,
                                    ),
                                )
                                loopEnd = LoopEnd.FINISHED
                                return@all false
                            }

                            is DashScopeServerEvent.Other -> {
                                Log.d(TAG, "Ignoring DashScope event: ${event.type}")
                            }
                        }
                    }

                    SessionInput.SocketClosed -> {
                        throw IOException("WebSocket closed before session.finished")
                    }
                }
                true
            }
            return loopEnd
        } finally {
            audioCapture.cancel()
            audioSenderJob?.cancelAndJoin()
        }
    }

    private data class AudioCapture(
        val frames: ReceiveChannel<AudioFrame.Data>,
        val job: Job,
    ) {
        suspend fun stop() {
            job.cancelAndJoin()
        }

        suspend fun cancel() {
            frames.cancel()
            job.cancelAndJoin()
        }
    }

    private fun DefaultClientWebSocketSession.serverInputs(): Flow<SessionInput> =
        incoming
            .receiveAsFlow()
            .filterIsInstance<Frame.Text>()
            .map { frame ->
                val event = try {
                    DashScopeProtocol.parseServerEvent(frame.readText())
                } catch (exception: SerializationException) {
                    throw MalformedDashScopeEventException(exception)
                }
                val input: SessionInput = SessionInput.ServerEvent(event)
                input
            }
            .onCompletion { cause ->
                if (cause == null) emit(SessionInput.SocketClosed)
            }

    private fun audioFrames(): Flow<AudioFrame> = flow {
        val recorder = createAudioRecord()
        try {
            recorder.startRecording()
            check(recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                "AudioRecord did not enter the recording state"
            }
            emit(AudioFrame.Ready)

            val buffer = ByteArray(AUDIO_CHUNK_SIZE_BYTES)
            while (currentCoroutineContext().isActive) {
                val bytesRead = recorder.read(
                    buffer,
                    0,
                    buffer.size,
                    AudioRecord.READ_BLOCKING,
                )
                when {
                    bytesRead > 0 -> {
                        emit(
                            AudioFrame.Data(
                                bytes = buffer.copyOf(bytesRead),
                                rms = calculateRms(buffer, bytesRead),
                            ),
                        )
                    }

                    bytesRead == 0 -> Unit
                    else -> error("AudioRecord.read returned $bytesRead")
                }
            }
        } finally {
            runCatching {
                if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    recorder.stop()
                }
            }
            runCatching { recorder.release() }
        }
    }.flowOn(Dispatchers.IO)

    private fun createAudioRecord(): AudioRecord {
        check(
            recordingContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        ) {
            "Microphone permission is not granted"
        }
        val minimumBufferSize = AudioRecord.getMinBufferSize(
            DashScopeProtocol.SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        require(minimumBufferSize > 0) {
            "Unsupported AudioRecord configuration: $minimumBufferSize"
        }

        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(DashScopeProtocol.SAMPLE_RATE_HZ)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .build()
        val builder = AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            .setAudioFormat(format)
            .setBufferSizeInBytes(max(minimumBufferSize, INTERNAL_BUFFER_SIZE_BYTES))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setContext(recordingContext)
        }

        val recorder = builder.build()
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            error("AudioRecord initialization failed")
        }
        return recorder
    }

    private fun calculateRms(buffer: ByteArray, length: Int): Float {
        var sumOfSquares = 0.0
        var sampleCount = 0
        var index = 0
        while (index + 1 < length) {
            val sample = ((buffer[index + 1].toInt() shl 8) or
                (buffer[index].toInt() and 0xff)).toShort().toDouble()
            sumOfSquares += sample * sample
            sampleCount += 1
            index += 2
        }
        if (sampleCount == 0) return 0f

        val normalizedRms = sqrt(sumOfSquares / sampleCount) / Short.MAX_VALUE
        val decibels = 20.0 * log10(normalizedRms.coerceAtLeast(MINIMUM_NORMALIZED_RMS))
        return ((decibels + 60.0) / 6.0).coerceIn(0.0, 10.0).toFloat()
    }

    sealed interface Event {
        data object Ready : Event
        data object BeginningOfSpeech : Event
        data class RmsChanged(val rms: Float) : Event
        data class PartialTranscript(
            val transcript: String,
            val language: String?,
        ) : Event

        data object EndOfSpeech : Event
        data class Completed(
            val transcript: String?,
            val language: String?,
            val speechDetected: Boolean,
        ) : Event

        data class Failure(
            val kind: FailureKind,
            val detail: String,
        ) : Event
    }

    enum class FailureKind {
        NETWORK,
        AUTHENTICATION,
        SERVER,
        AUDIO,
        CLIENT,
    }

    private enum class Command {
        Finish,
        Cancel,
    }

    private enum class LoopEnd {
        FINISHED,
        CANCELED,
    }

    private sealed interface SessionInput {
        data class ServerEvent(
            val event: DashScopeServerEvent,
        ) : SessionInput

        data class CommandReceived(
            val command: Command,
        ) : SessionInput

        data object SocketClosed : SessionInput
    }

    private sealed interface AudioFrame {
        data object Ready : AudioFrame
        data class Data(
            val bytes: ByteArray,
            val rms: Float,
        ) : AudioFrame
    }

    private data class SessionState(
        val configured: Boolean = false,
        val finishRequested: Boolean = false,
        val finishSent: Boolean = false,
        val speechDetected: Boolean = false,
        val completedText: String = "",
    ) {
        fun appendCompleted(transcript: String): SessionState =
            transcript
                .takeIf { it.isNotBlank() }
                ?.let {
                    copy(
                        completedText = TranscriptNormalizer.normalize(
                            completedText + it,
                        ),
                    )
                }
                ?: this
    }

    private sealed interface OutboundMessage {
        data class Command(
            val command: DashScopeClientCommand,
        ) : OutboundMessage

        data class Audio(
            val bytes: ByteArray,
        ) : OutboundMessage
    }

    private class MalformedDashScopeEventException(
        cause: Throwable,
    ) : Exception(cause)

    private class DashScopeServerException(
        val detail: String,
    ) : Exception()

    private class AudioCaptureException(
        cause: Throwable,
    ) : Exception(cause)

    private fun Exception.failureKind(): FailureKind {
        val responseCode = (this as? ResponseException)?.response?.status?.value
        return when {
            responseCode == 401 || responseCode == 403 -> FailureKind.AUTHENTICATION
            responseCode != null -> FailureKind.SERVER
            this is MalformedDashScopeEventException ||
                this is DashScopeServerException -> FailureKind.SERVER

            this is AudioCaptureException -> FailureKind.AUDIO
            this is IOException || this is HttpRequestTimeoutException -> FailureKind.NETWORK
            else -> FailureKind.CLIENT
        }
    }

    private fun Exception.failureDetail(): String {
        val responseCode = (this as? ResponseException)?.response?.status?.value
        return when {
            responseCode != null -> "HTTP $responseCode"
            this is DashScopeServerException -> detail
            else -> javaClass.simpleName
        }
    }

    private companion object {
        const val TAG = "DashVoiceSession"
        const val AUDIO_CHUNK_SIZE_BYTES = 1_024
        const val INTERNAL_BUFFER_SIZE_BYTES = 4_096
        const val PRE_ROLL_CHUNK_COUNT = 64
        const val MINIMUM_NORMALIZED_RMS = 0.000_001

        val httpClient = HttpClient(CIO) {
            expectSuccess = true
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
            }
            install(WebSockets) {
                pingIntervalMillis = 20_000
                contentConverter = KotlinxWebsocketSerializationConverter(
                    DashScopeProtocol.json,
                )
            }
        }
    }
}

internal fun dashScopeEndpoint(baseUrl: String): Url =
    URLBuilder(baseUrl.trim()).apply {
        parameters.remove("model")
        encodedPathSegments = listOf("api-ws", "v1", "inference")
    }.build()
