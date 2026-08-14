package com.cy.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private sealed interface MdBlock {
    data class Paragraph(val text: String) : MdBlock
    data class Code(val code: String) : MdBlock
}

/** 极简 Markdown 解析：代码块、标题、加粗、列表符号 */
private fun parseMarkdown(md: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    val lines = md.split("\n")
    var inCode = false
    val codeLines = mutableListOf<String>()
    val paraLines = mutableListOf<String>()

    fun flushParagraph() {
        if (paraLines.isNotEmpty()) {
            blocks += MdBlock.Paragraph(paraLines.joinToString("\n"))
            paraLines.clear()
        }
    }

    for (line in lines) {
        if (line.trimStart().startsWith("```")) {
            if (inCode) {
                blocks += MdBlock.Code(codeLines.joinToString("\n"))
                codeLines.clear()
                inCode = false
            } else {
                flushParagraph()
                inCode = true
            }
        } else if (inCode) {
            codeLines += line
        } else {
            paraLines += line
        }
    }
    if (inCode) blocks += MdBlock.Code(codeLines.joinToString("\n"))
    flushParagraph()
    return blocks
}

/** 行内加粗解析：**xxx** */
private fun buildRich(text: String, base: Color): AnnotatedString {
    return buildAnnotatedString {
        val regex = Regex("\\*\\*(.+?)\\*\\*")
        var last = 0
        for (m in regex.findAll(text)) {
            append(text.substring(last, m.range.first))
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(m.groupValues[1])
            }
            last = m.range.last + 1
        }
        append(text.substring(last))
    }
}

/** 将 AI 回复渲染为简单的富文本（支持代码块 / 加粗） */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
) {
    val blocks = remember(markdown) { parseMarkdown(markdown) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Paragraph -> {
                    val text = block.text.trim()
                    if (text.isNotEmpty()) {
                        if (text.startsWith("#")) {
                            Text(
                                text = text.trimStart('#').trim(),
                                color = color,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                            )
                        } else {
                            Text(
                                text = buildRich(text, color),
                                color = color,
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                            )
                        }
                    }
                }
                is MdBlock.Code -> {
                    Text(
                        text = block.code,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .verticalScroll(rememberScrollState())
                            .padding(10.dp),
                    )
                }
            }
        }
    }
}