package com.cy.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Web 版配色（Tailwind 提取）：
 * - 主色 blue-500 #3B82F6；用户气泡 rgba(92,124,250,.88) ≈ #5C7CFA
 * - 深色主题：背景 #0A0E1A，卡片/弹窗 #191C34，边线 #33365A
 */
private val WebBlue = Color(0xFF3B82F6)      // blue-500
private val WebBlueLight = Color(0xFF60A5FA) // blue-400
private val WebBlue100 = Color(0xFFDBEAFE)   // blue-100
private val WebBlue800 = Color(0xFF1E40AF)   // blue-800
private val WebBlue200 = Color(0xFFBFDBFE)   // blue-200
private val UserBubble = Color(0xFF5C7CFA)   // 用户消息气泡
private val DarkBg = Color(0xFF0A0E1A)       // 深色背景
private val DarkCard = Color(0xFF191C34)     // 深色卡片
private val DarkBorder = Color(0xFF33365A)   // 深色边线
private val DarkAiBubble = Color(0xFF1E2532) // 深色助手气泡 rgba(30,37,50,.75)
private val DarkAiText = Color(0xFFD1D5F0)   // 深色助手气泡文字
private val Gray500 = Color(0xFF6B7280)      // gray-500
private val Gray400 = Color(0xFF9CA3AF)      // gray-400
private val Gray200 = Color(0xFFE5E7EB)      // gray-200
private val Gray100 = Color(0xFFF3F4F6)      // gray-100
private val Gray800 = Color(0xFF1F2937)      // gray-800
private val Red500 = Color(0xFFEF4444)       // red-500
private val Red400 = Color(0xFFF87171)       // red-400
private val Amber500 = Color(0xFFF59E0B)     // amber-500

/** Web 端顶部 tab 栏与玻璃卡片使用的颜色（气泡/输入框直接取用） */
object WebColors {
    val userBubble = UserBubble
    val userBubbleDark = UserBubble.copy(alpha = 0.75f)
    val aiBubbleLight = Color.White
    val aiBubbleDark = DarkAiBubble
    val aiTextDark = DarkAiText
    val reasoningBgLight = WebBlue.copy(alpha = 0.06f)
    val reasoningBgDark = WebBlue.copy(alpha = 0.10f)
    val reasoningBorderLight = WebBlue.copy(alpha = 0.30f)
    val inputLight = Color.White.copy(alpha = 0.72f)
    val inputDark = Color(0xFF1A2032).copy(alpha = 0.78f) // rgba(26,32,50,.78)
    val amber = Amber500
}

private val LightColors = lightColorScheme(
    primary = WebBlue,
    onPrimary = Color.White,
    primaryContainer = WebBlue100,
    onPrimaryContainer = WebBlue800,
    secondary = Gray500,
    background = Color.White,
    onBackground = Gray800,
    surface = Color.White,
    onSurface = Gray800,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray500,
    error = Red500,
    outline = Gray200,
    outlineVariant = Gray100,
)

private val DarkColors = darkColorScheme(
    primary = WebBlueLight,
    onPrimary = Color(0xFF0A0E1A),
    primaryContainer = Color(0xFF1E3A5F),
    onPrimaryContainer = WebBlue200,
    secondary = Gray400,
    background = DarkBg,
    onBackground = Color(0xFFF3F4F6),
    surface = DarkCard,
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = DarkCard,
    onSurfaceVariant = Gray400,
    error = Red400,
    outline = DarkBorder,
    outlineVariant = Color(0xFF2A2D50),
)

@Composable
fun ChuYiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
