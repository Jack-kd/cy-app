package com.cy.app.data.model

import kotlinx.serialization.Serializable

/** 单条聊天消息，role 遵循 OpenAI 约定（user / assistant / system） */
@Serializable
data class ChatMessage(
    val role: String,
    val content: String = "",
    /** 推理模型的思考内容（如 deepseek-reasoner） */
    val reasoning: String = "",
    /** 是否错误占位消息 */
    val error: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
)

/** 一次完整对话（对应原 H5 的 conversations） */
@Serializable
data class Conversation(
    val id: String,
    val title: String,
    val messages: List<ChatMessage> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

/** 一个 OpenAI 兼容的 AI 提供方 */
@Serializable
data class AiProvider(
    val id: String,
    val displayName: String,
    val defaultBaseUrl: String,
    val models: List<String>,
)

/** 主题模式 */
enum class ThemeMode(val key: String) {
    System("system"), Light("light"), Dark("dark");

    companion object {
        fun from(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: System
    }
}
