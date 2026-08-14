package com.cy.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cy.app.data.AiProviders
import com.cy.app.data.model.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val settings by viewModel.settings.collectAsState()
    var expandedProvider by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
            )
        },
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ===== 外观主题 =====
            SectionHeader("外观主题")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    val isSelected = settings.themeMode == mode
                    OutlinedButton(
                        onClick = { viewModel.setThemeMode(mode) },
                        colors = if (isSelected) ButtonDefaults.buttonColors()
                        else ButtonDefaults.outlinedButtonColors(),
                    ) {
                        Text(
                            when (mode) {
                                ThemeMode.Light -> "浅色"
                                ThemeMode.Dark -> "深色"
                                ThemeMode.System -> "跟随系统"
                            },
                        )
                    }
                }
            }

            // ===== 功能开关 =====
            SectionHeader("功能开关")
            ToggleRow("深度思考", "对问题进行深入分析和推理", settings.deepThink) {
                viewModel.setDeepThink(it)
            }
            ToggleRow("全模态识别", "支持图片、文本等多种输入理解", false, null)
            ToggleRow("长上下文记忆", "结合历史对话上下文生成回复", true, null)

            // ===== 模型 & API =====
            SectionHeader("模型 & API")
            AiProviders.all.forEach { provider ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(provider.displayName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.height(8.dp))

                        // API Key
                        ApiKeyField(
                            label = "${provider.displayName} API Key",
                            value = settings.apiKeys[provider.id].orEmpty(),
                            onValueChange = { viewModel.setApiKey(provider.id, it) },
                        )
                        Spacer(Modifier.height(6.dp))

                        // Base URL
                        OutlinedTextField(
                            value = settings.baseUrls[provider.id].orEmpty(),
                            onValueChange = { viewModel.setBaseUrl(provider.id, it) },
                            label = { Text("Base URL（可选，默认${provider.defaultBaseUrl}）") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                        )
                        Spacer(Modifier.height(6.dp))

                        // 模型选择
                        ExposedDropdownMenuBox(
                            expanded = expandedProvider,
                            onExpandedChange = { expandedProvider = it },
                        ) {
                            OutlinedTextField(
                                value = settings.modelOf(provider.id),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("模型") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedProvider) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(8.dp),
                            )
                            ExposedDropdownMenu(
                                expanded = expandedProvider,
                                onDismissRequest = { expandedProvider = false },
                            ) {
                                provider.models.forEach { model ->
                                    DropdownMenuItem(
                                        text = { Text(model) },
                                        onClick = {
                                            viewModel.setModel(provider.id, model)
                                            expandedProvider = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun ToggleRow(title: String, desc: String, checked: Boolean, onCheck: ((Boolean) -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (onCheck != null) {
            Switch(checked = checked, onCheckedChange = onCheck)
        }
    }
    HorizontalDivider()
}

@Composable
private fun ApiKeyField(label: String, value: String, onValueChange: (String) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (visible) androidx.compose.ui.text.input.VisualTransformation.None
        else androidx.compose.ui.text.input.PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, "显示/隐藏")
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
    )
}