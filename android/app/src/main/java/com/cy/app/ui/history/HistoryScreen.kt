package com.cy.app.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cy.app.data.model.Conversation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 历史会话列表，与 Web 端风格一致：玻璃卡片 + 消息数 + 时间戳 */
@Composable
fun HistoryScreen(
    conversations: List<Conversation>,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    val dark = isSystemInDarkTheme()

    if (conversations.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "暂无对话记录",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "开始你的第一次对话吧",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
        ) {
            item {
                Text(
                    "历史记录",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            items(conversations, key = { it.id }) { conv ->
                ConversationCard(conv, dark, onClick = { onSelect(conv.id) }, onDelete = { onDelete(conv.id) })
            }
        }
    }
}

@Composable
private fun ConversationCard(conv: Conversation, dark: Boolean, onClick: () -> Unit, onDelete: () -> Unit) {
    val dateStr = remember(conv.updatedAt) {
        SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(conv.updatedAt))
    }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (dark) Color(0xFF191C34) else Color.White.copy(alpha = 0.72f),
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (dark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    conv.title,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "${conv.messages.size} 条消息 · $dateStr",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    "删除",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}