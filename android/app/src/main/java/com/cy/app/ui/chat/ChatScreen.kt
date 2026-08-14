package com.cy.app.ui.chat

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cy.app.data.model.ChatMessage
import com.cy.app.ui.components.MarkdownText
import kotlinx.coroutines.flow.first

/** 随机轮换的快捷提示词（与原 Web 端一致） */
private val SUGGESTIONS = listOf(
    "帮我制定健身计划",
    "如何养成早起习惯",
    "讲一个有趣的笑话",
    "写一首关于夏天的诗",
    "推荐几部科幻电影",
    "怎么做一道简单的菜",
    "告诉我最新的科技新闻",
)

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onOpenSettings: () -> Unit,
) {
    val settings by viewModel.settings.collectAsState()
    val conv by viewModel.currentConversation.collectAsState()
    val chatState by viewModel.chatState.collectAsState()
    var input by remember { mutableStateOf("") }

    val provider = settings?.selectedProvider()
    val model = settings?.modelOf(provider?.id.orEmpty())
    val suggestions = remember { SUGGESTIONS.shuffled().take(4) }

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶栏：标题 + 模型标签 + 新对话
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("初忆AI助手", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = "${provider?.displayName ?: ""} · ${model.orEmpty()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            TextButton(onClick = onOpenSettings) { Text("设置") }
            IconButton(onClick = { viewModel.newChat() }) {
                Icon(Icons.Default.Add, contentDescription = "新对话")
            }
        }

        Box(Modifier.weight(1f)) {
            val messages = conv?.messages.orEmpty()
            val listState = rememberLazyListState()
            // 自动滚到底部
            LaunchedEffect(messages.size, chatState) {
                if (messages.isNotEmpty() || chatState is ChatUiState.Streaming) {
                    listState.animateScrollToItem(
                        (messages.size + if (chatState is ChatUiState.Streaming) 1 else 0).coerceAtLeast(0)
                    )
                }
            }

            if (messages.isEmpty() && chatState !is ChatUiState.Streaming) {
                EmptyChat(
                    suggestions = suggestions,
                    onPick = { input = it },
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(messages) { index, msg ->
                        MessageBubble(msg)
                    }
                    if (chatState is ChatUiState.Streaming) {
                        item(key = "__streaming__") {
                            StreamingBubble(
                                text = (chatState as ChatUiState.Streaming).text,
                                reasoning = (chatState as ChatUiState.Streaming).reasoning,
                            )
                        }
                    }
                }
            }

            if (chatState is ChatUiState.Error) {
                val err = (chatState as ChatUiState.Error).message
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }

        // 输入区
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("发送消息...") },
                maxLines = 5,
                shape = RoundedCornerShape(24.dp),
            )
            Spacer(Modifier.width(8.dp))
            val isStreaming = chatState is ChatUiState.Streaming
            androidx.compose.material3.FilledIconButton(
                onClick = {
                    if (isStreaming) viewModel.stop() else {
                        viewModel.send(input)
                        input = ""
                    }
                },
                modifier = Modifier.size(48.dp),
            ) {
                if (isStreaming) {
                    Icon(Icons.Default.Stop, contentDescription = "停止")
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "发送")
                }
            }
        }
    }
}

@Composable
private fun EmptyChat(suggestions: List<String>, onPick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("你好，我是初忆AI助手", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "全模态识别 · 图文代码解析 · 多语言编程",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        suggestions.forEach { s ->
            Text(
                text = s,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        RoundedCornerShape(16.dp),
                    )
                    .clickable { onPick(s) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MessageBubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    val bubbleColor = if (isUser) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    val contentColor = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(if (isUser) 0.82f else 0.92f)
                .animateContentSize(),
        ) {
            if (msg.reasoning.isNotBlank()) {
                var expanded by remember { mutableStateOf(false) }
                Column(
                    Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        if (expanded) "▾ 思考过程" else "▸ 思考过程",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    if (expanded) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            msg.reasoning,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            Text(
                text = msg.content,
                color = if (msg.error) MaterialTheme.colorScheme.error else contentColor,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                modifier = Modifier
                    .background(bubbleColor, RoundedCornerShape(if (isUser) 16.dp else 16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun StreamingBubble(text: String, reasoning: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Column(
            Modifier
                .fillMaxWidth(0.92f)
                .animateContentSize(),
        ) {
            if (reasoning.isNotBlank()) {
                Text(
                    "▾ 思考中…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                MarkdownText(
                    markdown = text.ifBlank { "…" },
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            RoundedCornerShape(16.dp),
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                )
                Spacer(Modifier.width(8.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}