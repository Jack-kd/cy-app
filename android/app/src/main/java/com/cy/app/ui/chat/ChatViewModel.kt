package com.cy.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cy.app.data.AppSettings
import com.cy.app.data.ConversationRepository
import com.cy.app.data.SettingsRepository
import com.cy.app.data.model.ChatMessage
import com.cy.app.data.model.Conversation
import com.cy.app.data.remote.ChatApiClient
import com.cy.app.data.remote.ChatStreamEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface ChatUiState {
    data object Idle : ChatUiState
    data class Streaming(val text: String, val reasoning: String) : ChatUiState
    data class Error(val message: String) : ChatUiState
}

class ChatViewModel(
    private val settingsRepo: SettingsRepository,
    private val conversationRepo: ConversationRepository,
    private val api: ChatApiClient,
) : ViewModel() {

    private val _settings = MutableStateFlow<AppSettings?>(null)
    val settings: StateFlow<AppSettings?> = _settings.asStateFlow()

    val conversations: StateFlow<List<Conversation>> = conversationRepo.conversations

    private val _currentConversation = MutableStateFlow<Conversation?>(null)
    val currentConversation: StateFlow<Conversation?> = _currentConversation.asStateFlow()

    private val _chatState = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
    val chatState: StateFlow<ChatUiState> = _chatState.asStateFlow()

    private var streamJob: Job? = null
    private var assistantText = StringBuilder()
    private var assistantReasoning = StringBuilder()

    init {
        viewModelScope.launch {
            settingsRepo.settings.collect { _settings.value = it }
        }
    }

    fun openConversation(id: String?) {
        if (id == null) {
            _currentConversation.value = null
            return
        }
        viewModelScope.launch {
            _currentConversation.value = conversationRepo.current(id).first()
        }
    }

    fun newChat() {
        val conv = conversationRepo.create()
        _currentConversation.value = conv
        _chatState.value = ChatUiState.Idle
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            conversationRepo.remove(id)
            if (_currentConversation.value?.id == id) _currentConversation.value = null
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            conversationRepo.clear()
            _currentConversation.value = null
            _chatState.value = ChatUiState.Idle
        }
    }

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _chatState.value is ChatUiState.Streaming) return
        val s = _settings.value ?: return

        val conv = _currentConversation.value ?: run {
            val c = conversationRepo.create()
            _currentConversation.value = c
            c
        }
        val userMsg = ChatMessage(role = "user", content = trimmed)
        conversationRepo.append(conv.id, userMsg)

        val systemPrompt = if (s.deepThink) {
            "你是初忆AI助手。请进行深入分析，条理清晰地解答。"
        } else {
            "你是初忆AI助手，一个友好、全面的中文AI助手。"
        }
        val history = listOf(ChatMessage(role = "system", content = systemPrompt)) +
            conv.messages.filter { it.role != "system" } + userMsg

        val provider = s.selectedProvider()
        val apiKey = s.apiKeyOf(provider.id)
        if (apiKey.isBlank()) {
            _chatState.value = ChatUiState.Error("请先在「设置」中填写 ${provider.displayName} 的 API Key")
            return
        }

        assistantText = StringBuilder()
        assistantReasoning = StringBuilder()
        _chatState.value = ChatUiState.Streaming("", "")

        streamJob = viewModelScope.launch {
            api.streamChat(
                baseUrl = s.baseUrlOf(provider.id),
                apiKey = apiKey,
                model = s.modelOf(provider.id),
                messages = history,
            ).collect { event ->
                when (event) {
                    is ChatStreamEvent.Delta -> {
                        assistantText.append(event.text)
                        _chatState.value = ChatUiState.Streaming(
                            assistantText.toString(), assistantReasoning.toString()
                        )
                    }
                    is ChatStreamEvent.Reasoning -> {
                        assistantReasoning.append(event.text)
                        _chatState.value = ChatUiState.Streaming(
                            assistantText.toString(), assistantReasoning.toString()
                        )
                    }
                    is ChatStreamEvent.Done -> {
                        conversationRepo.append(
                            conv.id,
                            ChatMessage(
                                role = "assistant",
                                content = event.fullText,
                                reasoning = event.reasoning,
                            ),
                        )
                        _chatState.value = ChatUiState.Idle
                    }
                    is ChatStreamEvent.Error -> {
                        conversationRepo.append(
                            conv.id,
                            ChatMessage(role = "assistant", content = event.message, error = true),
                        )
                        _chatState.value = ChatUiState.Error(event.message)
                    }
                }
            }
        }
    }

    fun stop() {
        streamJob?.cancel()
        streamJob = null
        _chatState.value = ChatUiState.Idle
    }

    override fun onCleared() {
        streamJob?.cancel()
    }
}