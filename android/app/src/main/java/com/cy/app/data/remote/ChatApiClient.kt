package com.cy.app.data.remote

import com.cy.app.data.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/** 流式令牌结束标记 */
object ChatApi {
    const val END = "\u0000END\u0000"
}

/** 流式输出事件 */
sealed interface ChatStreamEvent {
    /** 正常内容增量 */
    data class Delta(val text: String) : ChatStreamEvent
    /** 推理内容增量（reasoning_content） */
    data class Reasoning(val text: String) : ChatStreamEvent
    data class Done(val fullText: String, val reasoning: String) : ChatStreamEvent
    data class Error(val message: String) : ChatStreamEvent
}

@Serializable
private data class ChatRequest(
    val model: String,
    val messages: List<OutMessage>,
    val stream: Boolean = true,
)

@Serializable
private data class OutMessage(val role: String, val content: String)

@Serializable
private data class SseChunk(
    val choices: List<SseChoice> = emptyList(),
    val error: SseError? = null,
)

@Serializable
private data class SseChoice(val delta: SseDelta? = null)

@Serializable
private data class SseDelta(val content: String? = null, val reasoning_content: String? = null)

@Serializable
private data class SseError(val message: String = "")

private val json = Json { ignoreUnknownKeys = true }

/** 以 OpenAI 兼容的 SSE 流式方式调用 /chat/completions */
class ChatApiClient(
    private val client: OkHttpClient = defaultClient(),
) {
    fun streamChat(
        baseUrl: String,
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
    ): Flow<ChatStreamEvent> {
        val channel = Channel<ChatStreamEvent>(Channel.UNLIMITED)
        val bodyJson = json.encodeToString(
            ChatRequest.serializer(),
            ChatRequest(
                model = model,
                messages = messages.map { OutMessage(it.role, it.content.takeIf { c -> c.isNotBlank() } ?: " ") },
            ),
        )
        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .post(bodyJson.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                channel.trySend(ChatStreamEvent.Error(e.message ?: "网络错误"))
                channel.close()
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    if (!response.isSuccessful) {
                        val body = response.body?.string().orEmpty()
                        val reason = runCatching {
                            json.decodeFromString<SseError>(body).message
                        }.getOrNull() ?: "HTTP ${response.code} $body"
                        channel.trySend(ChatStreamEvent.Error(reason))
                        channel.close()
                        return
                    }
                    val source = response.body?.source()
                    if (source == null) {
                        channel.trySend(ChatStreamEvent.Error("空响应"))
                        channel.close()
                        return
                    }
                    val full = StringBuilder()
                    val reasoning = StringBuilder()
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        val trimmed = line.trim()
                        if (!trimmed.startsWith("data:")) continue
                        val data = trimmed.removePrefix("data:").trim()
                        if (data == "[DONE]") break
                        val chunk = runCatching { json.decodeFromString<SseChunk>(data) }.getOrNull()
                            ?: continue
                        chunk.error?.let { err ->
                            channel.trySend(ChatStreamEvent.Error(err.message))
                            channel.close()
                            return
                        }
                        val delta = chunk.choices.firstOrNull()?.delta
                        delta?.reasoning_content?.let { r ->
                            if (r.isNotEmpty()) {
                                reasoning.append(r)
                                channel.trySend(ChatStreamEvent.Reasoning(r))
                            }
                        }
                        delta?.content?.let { c ->
                            if (c.isNotEmpty()) {
                                full.append(c)
                                channel.trySend(ChatStreamEvent.Delta(c))
                            }
                        }
                    }
                    channel.trySend(ChatStreamEvent.Done(full.toString(), reasoning.toString()))
                    channel.close()
                } catch (e: Exception) {
                    channel.trySend(ChatStreamEvent.Error(e.message ?: "解析失败"))
                    channel.close()
                } finally {
                    response.close()
                }
            }
        })
        return channel.receiveAsFlow().flowOn(Dispatchers.IO)
    }

    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }
}