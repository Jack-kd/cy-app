package com.cy.app.ui.chat

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cy.app.data.model.ChatMessage
import com.cy.app.ui.components.MarkdownText
import com.cy.app.ui.theme.WebColors

/** 随机轮换的快捷提示词（与原 Web 端一致） */
private val SUGGESTIONS = listOf(
    "帮我制定健身计划",
    "如何养成早起习惯",
    "讲一个有趣的笑话",
    "写一首关于夏天的诗",
    "推荐几本必读的书",
    "怎么做一道简单的菜",
    "解释量子纠缠原理",
    "推荐周末好去处",
)

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
) {
    val settings by viewModel.settings.collectAsState()
    val conv by viewModel.currentConversation.collectAsState()
    val chatState by viewModel.chatState.collectAsState()
    var input by remember { mutableStateOf("") }

    val dark = isSystemInDarkTheme()
    val provider = settings?.selectedProvider()
    val model = settings?.modelOf(provider?.id.orEmpty())
    val deepThink = settings?.deepThink == true
    val suggestions = remember { SUGGESTIONS.shuffled().take(4) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // 顶栏：模型名 + 深度思考徽章 + 新对话
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    provider?.displayName ?: "初忆AI助手",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = model.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (deepThink) {
                    Text(
                        "深度思考",
                        fontSize = 9.sp,
                        color = WebColors.amber,
                        modifier = Modifier
                            .background(
                                WebColors.amber.copy(alpha = if (dark) 0.10f else 0.12f),
                                RoundedCornerShape(4.dp),
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            IconButton(onClick = { viewModel.newChat() }) {
                Icon(Icons.Default.Add, contentDescription = "新对话", tint = MaterialTheme.colorScheme.primary)
            }
        }

        // 流式状态条（对应 Web 端「深度推理中…/生成回答中…」）
        if (chatState is ChatUiState.Streaming) {
            val streaming = chatState as ChatUiState.Streaming
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = if (dark) 0.10f else 0.06f),
                    )
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.5.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = if (streaming.reasoning.isNotBlank()) "深度推理中…" else "生成回答中…",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        // 消息列表
        Box(Modifier.weight(1f)) {
            val messages = conv?.messages.orEmpty()
            val listState = rememberLazyListState()
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
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    itemsIndexed(messages) { index, msg ->
                        MessageBubble(msg, dark)
                    }
                    if (chatState is ChatUiState.Streaming) {
                        item(key = "__streaming__") {
                            StreamingBubble(
                                text = (chatState as ChatUiState.Streaming).text,
                                reasoning = (chatState as ChatUiState.Streaming).reasoning,
                                dark = dark,
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

        // 输入区：玻璃圆角卡片 + 圆形发送/停止按钮
        InputBar(
            value = input,
            onValueChange = { input = it },
            isStreaming = chatState is ChatUiState.Streaming,
            onSend = {
                viewModel.send(input)
                input = ""
            },
            onStop = { viewModel.stop() },
        )
    }
}

/** 空状态：标题 + 副标题 + 2 列快捷建议（对应 Web 端 grid-cols-2） */
@Composable
private fun EmptyChat(suggestions: List<String>, onPick: (String) -> Unit) {
    val dark = isSystemInDarkTheme()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "你好，我是初忆AI助手",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "全模态识别 · 图文代码解析 · 多语言编程",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))
        val left = suggestions.filterIndexed { i, _ -> i % 2 == 0 }
        val right = suggestions.filterIndexed { i, _ -> i % 2 == 1 }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                left.forEach { SuggestionChip(it, dark, onPick) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                right.forEach { SuggestionChip(it, dark, onPick) }
            }
        }
    }
}

@Composable
private fun SuggestionChip(text: String, dark: Boolean, onClick: (String) -> Unit) {
    Text(
        text = text,
        color = if (dark) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
        fontSize = 13.sp,
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                RoundedCornerShape(12.dp),
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                RoundedCornerShape(12.dp),
            )
            .clickable { onClick(text) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

/** 单条消息：用户右对齐蓝色气泡；助手左对齐玻璃气泡；推理块可展开 */
@Composable
private fun MessageBubble(msg: ChatMessage, dark: Boolean) {
    val isUser = msg.role == "user"

    if (isUser) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                text = msg.content,
                color = Color.White,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .animateContentSize()
                    .background(
                        if (dark) WebColors.userBubbleDark else WebColors.userBubble,
                        RoundedCornerShape(16.dp),
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
        return
    }

    // 助手消息
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
    ) {
        if (msg.reasoning.isNotBlank()) {
            ReasoningBlock(msg.reasoning, dark)
            Spacer(Modifier.height(6.dp))
        }
        MarkdownText(
            markdown = msg.content,
            color = if (dark) WebColors.aiTextDark else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .background(
                    if (dark) WebColors.aiBubbleDark else WebColors.aiBubbleLight,
                    RoundedCornerShape(16.dp),
                )
                .border(
                    1.dp,
                    if (dark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f),
                    RoundedCornerShape(16.dp),
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

/** 推理过程块：蓝底 + 左侧 3dp 蓝色边条，点击展开/收起 */
@Composable
private fun ReasoningBlock(reasoning: String, dark: Boolean) {
    var expanded by remember { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxWidth(0.92f)
            .background(
                if (dark) WebColors.reasoningBgDark else WebColors.reasoningBgLight,
                RoundedCornerShape(8.dp),
            )
            .clickable { expanded = !expanded }
            .padding(start = 0.dp, top = 6.dp, end = 10.dp, bottom = 6.dp),
    ) {
        Row {
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(
                        if (dark) WebColors.reasoningBorderLight else WebColors.reasoningBorderLight,
                        RoundedCornerShape(2.dp),
                    ),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (expanded) "▾ 推理过程" else "▸ 推理过程",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                if (expanded) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        reasoning,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/** 流式输出气泡：与助手一致，末尾带加载指示 */
@Composable
private fun StreamingBubble(text: String, reasoning: String, dark: Boolean) {
    Column(Modifier.fillMaxWidth()) {
        if (reasoning.isNotBlank()) {
            ReasoningBlock(reasoning, dark)
            Spacer(Modifier.height(6.dp))
        }
        Row(verticalAlignment = Alignment.Bottom) {
            MarkdownText(
                markdown = text.ifBlank { "…" },
                color = if (dark) WebColors.aiTextDark else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (dark) WebColors.aiBubbleDark else WebColors.aiBubbleLight,
                        RoundedCornerShape(16.dp),
                    )
                    .border(
                        1.dp,
                        if (dark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f),
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

/** 输入区：玻璃圆角卡片，内含无边框文本框 + 圆形发送/停止按钮 */
@Composable
private fun InputBar(
    value: String,
    onValueChange: (String) -> Unit,
    isStreaming: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
) {
    val dark = isSystemInDarkTheme()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .background(
                    if (dark) WebColors.inputDark else WebColors.inputLight,
                    RoundedCornerShape(20.dp),
                )
                .border(
                    1.dp,
                    if (dark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f),
                    RoundedCornerShape(20.dp),
                )
                .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                maxLines = 5,
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                "发送消息...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 15.sp,
                            )
                        }
                        inner()
                    }
                },
            )
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))
                    .clickable { if (isStreaming) onStop() else onSend() },
                contentAlignment = Alignment.Center,
            ) {
                if (isStreaming) {
                    Icon(Icons.Default.Stop, contentDescription = "停止", tint = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "发送", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
