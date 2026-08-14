package com.cy.app.ui.chat

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cy.app.ChuYiApplication
import com.cy.app.data.remote.ChatApiClient

class ChatViewModelFactory(private val app: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val app = app as ChuYiApplication
        return ChatViewModel(
            settingsRepo = app.settingsRepository,
            conversationRepo = app.conversationRepository,
            api = ChatApiClient(),
        ) as T
    }
}