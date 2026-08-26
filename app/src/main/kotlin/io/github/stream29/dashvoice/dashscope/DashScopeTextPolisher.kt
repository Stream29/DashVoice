package io.github.stream29.dashvoice.dashscope

import io.github.stream29.dashvoice.data.DashVoiceSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

internal object DashScopeTextPolisher {
    suspend fun polish(
        transcript: String,
        settings: DashVoiceSettings,
    ): String? {
        val request = DashScopeTextPolishRequest(
            messages = listOf(
                DashScopeChatMessage(
                    role = "system",
                    content = settings.textPolishPrompt,
                ),
                DashScopeChatMessage(
                    role = "user",
                    content = transcript,
                ),
            ),
        )
        val response = httpClient.post(dashScopeTextPolishEndpoint(settings.baseUrl)) {
            header(HttpHeaders.Authorization, "Bearer ${settings.apiKey}")
            contentType(ContentType.Application.Json)
            setBody(DashScopeProtocol.json.encodeToString(request))
        }
        val body = DashScopeProtocol.json.decodeFromString<DashScopeTextPolishResponse>(
            response.bodyAsText(),
        )
        val content = body.choices
            .firstOrNull()
            ?.message
            ?.content
            ?.trim()
            .orEmpty()
        if (content.isBlank()) return null

        return DashScopeProtocol.json
            .decodeFromString<DashScopeTextPolishOutput>(content)
            .text
            .trim()
            .takeIf(String::isNotBlank)
    }

    private val httpClient = HttpClient(CIO) {
        expectSuccess = true
        install(HttpTimeout) {
            connectTimeoutMillis = REQUEST_TIMEOUT_MILLIS
            requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
        }
    }

    private const val REQUEST_TIMEOUT_MILLIS = 5_000L

}

internal fun dashScopeTextPolishEndpoint(baseUrl: String): Url =
    URLBuilder(baseUrl.trim()).apply {
        protocol = URLProtocol.HTTPS
        parameters.clear()
        encodedPathSegments = listOf("compatible-mode", "v1", "chat", "completions")
    }.build()

@Serializable
private data class DashScopeTextPolishRequest(
    val model: String = DashVoiceSettings.TEXT_POLISH_MODEL,
    val stream: Boolean = false,
    val temperature: Double = 0.2,
    @SerialName("response_format")
    val responseFormat: DashScopeResponseFormat = DashScopeResponseFormat(),
    @SerialName("enable_thinking")
    val enableThinking: Boolean = false,
    val messages: List<DashScopeChatMessage>,
)

@Serializable
private data class DashScopeResponseFormat(
    val type: String = "json_object",
)

@Serializable
private data class DashScopeChatMessage(
    val role: String,
    val content: String,
)

@Serializable
private data class DashScopeTextPolishResponse(
    val choices: List<DashScopeTextPolishChoice> = emptyList(),
)

@Serializable
private data class DashScopeTextPolishChoice(
    val message: DashScopeTextPolishMessage = DashScopeTextPolishMessage(),
)

@Serializable
private data class DashScopeTextPolishMessage(
    val content: String = "",
)

@Serializable
private data class DashScopeTextPolishOutput(
    val text: String = "",
)
