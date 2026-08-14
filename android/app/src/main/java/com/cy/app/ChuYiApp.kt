package com.cy.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cy.app.data.model.ThemeMode
import com.cy.app.ui.chat.ChatScreen
import com.cy.app.ui.chat.ChatViewModel
import com.cy.app.ui.chat.ChatViewModelFactory
import com.cy.app.ui.creation.CreationScreen
import com.cy.app.ui.history.HistoryScreen
import com.cy.app.ui.settings.DataManagementScreen
import com.cy.app.ui.settings.SettingsScreen
import com.cy.app.ui.settings.SettingsViewModel
import com.cy.app.ui.settings.SettingsViewModelFactory
import com.cy.app.ui.theme.ChuYiTheme

internal enum class Dest(val label: String, val icon: ImageVector) {
    Chat("聊天", Icons.Filled.ChatBubbleOutline),
    History("历史", Icons.Filled.History),
    Creation("创作", Icons.Filled.Palette),
    Settings("设置", Icons.Filled.Settings),
    DataManagement("数据管理", Icons.Filled.Settings),
}

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
        val bottomBar: @Composable (() -> Unit) = {
            BottomTabBar(
                selected = dest,
                onSelect = { dest = it },
            )
        }

        if (dest == Dest.DataManagement) {
            DataManagementScreen(
                repo = app.conversationRepository,
                onBack = { dest = Dest.Settings },
            )
            return@ChuYiTheme
        }

        Column(modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)) {
            Box(Modifier.weight(1f)) {
                when (dest) {
                    Dest.Chat -> ChatScreen(
                        viewModel = chatViewModel,
                    )
                    Dest.History -> HistoryScreen(
                        conversations = chatViewModel.conversations.collectAsState().value,
                        onSelect = { id -> chatViewModel.openConversation(id); dest = Dest.Chat },
                        onDelete = { chatViewModel.deleteConversation(it) },
                    )
                    Dest.Creation -> CreationScreen()
                    Dest.Settings -> SettingsScreen(
                        viewModel = settingsViewModel,
                        onOpenDataManagement = { dest = Dest.DataManagement },
                    )
                    Dest.DataManagement -> Unit
                }
            }
            bottomBar()
        }
    }
}

/** Web 端玻璃质感底部 tab 栏（rgba(255,255,255,.65) + 上边框） */
@Composable
private fun BottomTabBar(selected: Dest, onSelect: (Dest) -> Unit) {
    val dark = isSystemInDarkTheme()
    val barColor = if (dark) Color(0xFF10141F).copy(alpha = 0.85f)
    else Color.White.copy(alpha = 0.65f)
    val borderColor = if (dark) Color(0xFF2A2D50) else Color.White.copy(alpha = 0.3f)
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(barColor)
            .border(width = 1.dp, color = borderColor)
            .navigationBarsPadding()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(Dest.Chat, Dest.History, Dest.Creation, Dest.Settings).forEach { d ->
            val active = selected == d
            Column(
                modifier = Modifier
                    .clickable { onSelect(d) }
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Icon(
                    imageVector = d.icon,
                    contentDescription = d.label,
                    tint = if (active) activeColor else inactiveColor,
                )
                Text(
                    text = d.label,
                    color = if (active) activeColor else inactiveColor,
                    fontSize = 10.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}
