package com.meitu.generator.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meitu.generator.ui.theme.*
import com.meitu.generator.ui.components.Spacing
import com.meitu.generator.ui.components.CornerRadius
import com.meitu.generator.util.Constants

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val colors = LocalAppColors.current
    val githubToken by viewModel.githubToken.collectAsState()
    val openrouterApiKey by viewModel.openrouterApiKey.collectAsState()
    val sambanovaApiKey by viewModel.sambanovaApiKey.collectAsState()
    val showClearConfirm by viewModel.showClearConfirm.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val privacyEnabled by viewModel.privacyModeEnabled.collectAsState()

    var showTokenDialog by remember { mutableStateOf(false) }
    var showOpenRouterKeyDialog by remember { mutableStateOf(false) }
    var showSambanovaKeyDialog by remember { mutableStateOf(false) }
    var tokenInput by remember { mutableStateOf(githubToken) }
    var openrouterKeyInput by remember { mutableStateOf(openrouterApiKey) }
    var sambanovaKeyInput by remember { mutableStateOf(sambanovaApiKey) }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearToast()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = Spacing.PagePadding, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.ElementSpacing)
        ) {
            // ============ API Key 配置 ============
            item { SectionTitle("AI 模型配置") }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CornerRadius.Card))
                        .background(colors.surface)
                        .padding(Spacing.CardSpacing)
                ) {
                    // OpenRouter（主力）
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("OpenRouter（主力）", fontSize = 15.sp, color = colors.textPrimary)
                            if (openrouterApiKey.isNotBlank()) {
                                Text(
                                    "${openrouterApiKey.take(8)}...${openrouterApiKey.takeLast(4)}",
                                    fontSize = 11.sp, color = colors.success, fontFamily = FontFamily.Monospace
                                )
                            } else {
                                Text("已配置默认 Key", fontSize = 11.sp, color = colors.success)
                            }
                        }
                        IconButton(onClick = { openrouterKeyInput = openrouterApiKey; showOpenRouterKeyDialog = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑", tint = colors.accent, modifier = Modifier.size(18.dp))
                        }
                    }

                    Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.border))

                    // SambaNova（备用）
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("SambaNova（备用）", fontSize = 15.sp, color = colors.textPrimary)
                            if (sambanovaApiKey.isNotBlank()) {
                                Text(
                                    "${sambanovaApiKey.take(8)}...${sambanovaApiKey.takeLast(4)}",
                                    fontSize = 11.sp, color = colors.success, fontFamily = FontFamily.Monospace
                                )
                            } else {
                                Text("已配置默认 Key", fontSize = 11.sp, color = colors.success)
                            }
                        }
                        IconButton(onClick = { sambanovaKeyInput = sambanovaApiKey; showSambanovaKeyDialog = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑", tint = colors.accent, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // ============ 隐私与安全 ============
            item { SectionTitle("隐私与安全") }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(colors.surface)
                        .padding(horizontal = Spacing.CardSpacing),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("隐私对话模式", fontSize = 15.sp, color = colors.textPrimary)
                        Text("开启后敏感内容仅本地处理", fontSize = 12.sp, color = colors.textTertiary)
                    }
                    Switch(
                        checked = privacyEnabled,
                        onCheckedChange = { viewModel.togglePrivacyMode() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.accent,
                            checkedTrackColor = colors.accent.copy(alpha = 0.3f),
                            uncheckedThumbColor = colors.textTertiary,
                            uncheckedTrackColor = colors.border
                        )
                    )
                }
                Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.border))
            }

            // ============ GitHub Token ============
            item { SectionTitle("GitHub 配置") }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CornerRadius.Card))
                        .background(colors.surface)
                        .padding(Spacing.CardSpacing)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Personal Access Token", fontSize = 15.sp, color = colors.textSecondary)
                        IconButton(onClick = { tokenInput = githubToken; showTokenDialog = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑Token", tint = colors.accent, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    if (githubToken.isNotBlank()) {
                        val masked = "${githubToken.take(4)}****${githubToken.takeLast(4)}"
                        Text(masked, fontSize = 13.sp, color = colors.success, fontFamily = FontFamily.Monospace)
                    } else {
                        Text("未配置", fontSize = 13.sp, color = colors.error)
                    }
                }
            }

            // ============ 数据管理 ============
            item { SectionTitle("数据管理") }
            item { SettingRow("清理缓存", "", onClick = { viewModel.clearCache() }) }
            item { SettingRow("清空对话历史", "", onClick = { viewModel.clearChatHistory() }) }
            item { SettingRow("清空所有数据", "", color = colors.error, onClick = { viewModel.showClearAllConfirm() }) }

            // ============ 关于 ============
            item { SectionTitle("关于") }
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("布老师", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text("v${Constants.APP_VERSION}", fontSize = 13.sp, color = colors.textTertiary)
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }

        // Toast
        if (toastMessage != null) {
            Box(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(horizontal = Spacing.PagePadding, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(CornerRadius.Button)).background(colors.success.copy(alpha = 0.9f)).padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(toastMessage ?: "", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    // Token dialog
    if (showTokenDialog) {
        AlertDialog(
            onDismissRequest = { showTokenDialog = false },
            containerColor = colors.surface,
            title = { Text("设置 GitHub Token", color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium) },
            text = {
                Column {
                    Text("需要 Classic PAT（ghp_ 开头），勾选 repo + workflow 权限", fontSize = 13.sp, color = colors.textTertiary)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tokenInput, onValueChange = { tokenInput = it },
                        label = { Text("ghp_xxxx", color = colors.textTertiary) },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = colors.accent, unfocusedBorderColor = colors.border, cursorColor = colors.accent
                        ),
                        shape = RoundedCornerShape(CornerRadius.Input), modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.saveGithubToken(tokenInput.trim()); showTokenDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.accent)) { Text("保存") } },
            dismissButton = { TextButton(onClick = { showTokenDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.textTertiary)) { Text("取消") } }
        )
    }

    // OpenRouter Key dialog
    if (showOpenRouterKeyDialog) {
        AlertDialog(
            onDismissRequest = { showOpenRouterKeyDialog = false },
            containerColor = colors.surface,
            title = { Text("设置 OpenRouter API Key", color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium) },
            text = {
                Column {
                    Text("从 openrouter.ai 获取，用于主力模型", fontSize = 13.sp, color = colors.textTertiary)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = openrouterKeyInput, onValueChange = { openrouterKeyInput = it },
                        label = { Text("OpenRouter API Key", color = colors.textTertiary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = colors.accent, unfocusedBorderColor = colors.border, cursorColor = colors.accent
                        ),
                        shape = RoundedCornerShape(CornerRadius.Input), modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.saveOpenRouterApiKey(openrouterKeyInput.trim()); showOpenRouterKeyDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.accent)) { Text("保存") } },
            dismissButton = { TextButton(onClick = { showOpenRouterKeyDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.textTertiary)) { Text("取消") } }
        )
    }

    // SambaNova Key dialog
    if (showSambanovaKeyDialog) {
        AlertDialog(
            onDismissRequest = { showSambanovaKeyDialog = false },
            containerColor = colors.surface,
            title = { Text("设置 SambaNova API Key", color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium) },
            text = {
                Column {
                    Text("从 sambanova.ai 获取，用于备用模型", fontSize = 13.sp, color = colors.textTertiary)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = sambanovaKeyInput, onValueChange = { sambanovaKeyInput = it },
                        label = { Text("SambaNova API Key", color = colors.textTertiary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = colors.accent, unfocusedBorderColor = colors.border, cursorColor = colors.accent
                        ),
                        shape = RoundedCornerShape(CornerRadius.Input), modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.saveSambaNovaApiKey(sambanovaKeyInput.trim()); showSambanovaKeyDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.accent)) { Text("保存") } },
            dismissButton = { TextButton(onClick = { showSambanovaKeyDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.textTertiary)) { Text("取消") } }
        )
    }

    // Clear all confirm
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissClearAll() },
            containerColor = colors.surface,
            title = { Text("确认清空", color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium) },
            text = { Text("此操作将清除所有日志和设置，不可恢复！", fontSize = 15.sp, color = colors.textSecondary) },
            confirmButton = { TextButton(onClick = { viewModel.clearAllData() }, colors = ButtonDefaults.textButtonColors(contentColor = colors.error)) { Text("确认清空") } },
            dismissButton = { TextButton(onClick = { viewModel.dismissClearAll() }, colors = ButtonDefaults.textButtonColors(contentColor = colors.textTertiary)) { Text("取消") } }
        )
    }
}

@Composable
fun SettingRow(title: String, value: String, color: Color = Color.Unspecified, onClick: (() -> Unit)? = null) {
    val colors = LocalAppColors.current
    val textColor = if (color != Color.Unspecified) color else colors.textPrimary
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().height(52.dp).background(colors.surface)
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                .padding(horizontal = Spacing.CardSpacing),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 15.sp, color = textColor)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value.isNotEmpty()) Text(value, fontSize = 13.sp, color = colors.textTertiary)
                if (onClick != null) {
                    Spacer(Modifier.width(4.dp))
                    Text("›", fontSize = 18.sp, color = colors.textTertiary)
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.border))
    }
}

@Composable
fun SectionTitle(text: String) {
    val colors = LocalAppColors.current
    Text(text = text, fontSize = 13.sp, color = colors.textTertiary, letterSpacing = 1.sp, modifier = Modifier.padding(vertical = 8.dp))
}
