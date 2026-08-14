package com.cy.app.data

import android.content.Context
import com.cy.app.data.model.ChatMessage
import com.cy.app.data.model.Conversation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/** 会话存储：内存态 + JSON 文件持久化 */
class ConversationRepository(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private val file: File
        get() = File(context.filesDir, "conversations.json")

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    init {
        scope.launch {
            _conversations.value = load()
        }
    }

    fun current(conversationId: String?): Flow<Conversation?> = conversations.map { list ->
        list.firstOrNull { it.id == conversationId }
    }

    fun create(title: String = "新对话"): Conversation {
        val conv = Conversation(
            id = UUID.randomUUID().toString(),
            title = title,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        _conversations.value = listOf(conv) + _conversations.value
        persist()
        return conv
    }

    fun remove(conversationId: String) {
        _conversations.value = _conversations.value.filterNot { it.id == conversationId }
        persist()
    }

    fun clear() {
        _conversations.value = emptyList()
        persist()
    }

    /** 在指定会话追加消息，并自动更新标题（取首条用户消息） */
    fun append(conversationId: String, message: ChatMessage) {
        _conversations.value = _conversations.value.map { conv ->
            if (conv.id != conversationId) return@map conv
            val updated = conv.copy(
                messages = conv.messages + message,
                updatedAt = System.currentTimeMillis(),
            )
            val title = if (conv.title == "新对话" && message.role == "user") {
                message.content.trim().lineSequence().firstOrNull().orEmpty().take(20)
            } else conv.title
            updated.copy(title = title.ifBlank { "新对话" })
        }
        persist()
    }

    /** 替换最后一条消息（用于流式增量更新 assistant 回复） */
    fun upsertLast(conversationId: String, message: ChatMessage) {
        _conversations.value = _conversations.value.map { conv ->
            if (conv.id != conversationId) return@map conv
            val messages = if (conv.messages.isEmpty()) listOf(message) else {
                conv.messages.dropLast(1) + message
            }
            conv.copy(messages = messages, updatedAt = System.currentTimeMillis())
        }
        persist()
    }

    suspend fun snapshot(): List<Conversation> = conversations.first()

    /** 导出全部对话为 JSON 字符串 */
    fun exportJson(): String = json.encodeToString(_conversations.value)

    private fun load(): List<Conversation> {
        if (!file.exists()) return emptyList()
        return runCatching {
            json.decodeFromString<List<Conversation>>(file.readText())
        }.getOrDefault(emptyList())
    }

    private fun persist() {
        runCatching {
            file.writeText(json.encodeToString(_conversations.value))
        }
    }
}