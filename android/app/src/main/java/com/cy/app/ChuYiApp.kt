package com.cy.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cy.app.data.model.ThemeMode
import com.cy.app.ui.chat.ChatScreen
import com.cy.app.ui.chat.ChatViewModel
import com.cy.app.ui.chat.ChatViewModelFactory
import com.cy.app.ui.history.HistoryScreen
import com.cy.app.ui.settings.DataManagementScreen
import com.cy.app.ui.settings.SettingsScreen
import com.cy.app.ui.settings.SettingsViewModel
import com.cy.app.ui.settings.SettingsViewModelFactory
import com.cy.app.ui.theme.ChuYiTheme

internal enum class Dest { Chat, History, Settings, DataManagement }

@Composable
fun ChuYiApp() {
    val context = LocalContext.current
    val app = context.applicationContext as ChuYiApplication

    val chatViewModel: ChatViewModel = viewModel(factory = ChatViewModelFactory(app))
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(app))

    var dest by remember { mutableStateOf(Dest.Chat) }

    // 订阅主题模式，切换明暗
    val themeMode by settingsViewModel.settings.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val dark = when (themeMode.themeMode) {
        ThemeMode.Dark -> true
        ThemeMode.Light -> false
        ThemeMode.System -> systemDark
    }

    ChuYiTheme(darkTheme = dark) {
        when (dest) {
            Dest.Chat -> ChatScreen(
                viewModel = chatViewModel,
                onOpenSettings = { dest = Dest.Settings },
            )
            Dest.History -> HistoryScreen(
                conversations = chatViewModel.conversations.collectAsState().value,
                onBack = { dest = Dest.Chat },
                onSelect = { id -> chatViewModel.openConversation(id); dest = Dest.Chat },
                onDelete = { chatViewModel.deleteConversation(it) },
            )
            Dest.Settings -> SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { dest = Dest.Chat },
            )
            Dest.DataManagement -> DataManagementScreen(
                repo = app.conversationRepository,
                onBack = { dest = Dest.Chat },
            )
        }
    }
}