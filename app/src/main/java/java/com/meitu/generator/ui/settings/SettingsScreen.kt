package com.meitu.generator.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meitu.generator.ui.theme.*
import com.meitu.generator.ui.components.Spacing
import com.meitu.generator.ui.components.CornerRadius

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val colors = LocalAppColors.current
    val cozePat by viewModel.cozePat.collectAsState()
    val cozeBotId by viewModel.cozeBotId.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    var showPatDialog by remember { mutableStateOf(false) }
    var showBotIdDialog by remember { mutableStateOf(false) }
    var patInput by remember { mutableStateOf("") }
    var botIdInput by remember { mutableStateOf("") }

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
            // ============ Coze API 配置 ============
            item { SectionTitle("Coze API 配置") }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CornerRadius.Card))
                        .background(colors.surface)
                        .padding(Spacing.CardSpacing)
                ) {
                    // Coze PAT
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("API 令牌 (PAT)", fontSize = 15.sp, color = colors.textPrimary)
                            if (cozePat.isNotBlank()) {
                                Text(
                                    "${cozePat.take(8)}...${cozePat.takeLast(4)}",
                                    fontSize = 11.sp, color = colors.success, fontFamily = FontFamily.Monospace
                                )
                            } else {
                                Text("未配置", fontSize = 11.sp, color = colors.textTertiary)
                            }
                        }
                        IconButton(onClick = { patInput = cozePat; showPatDialog = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑", tint = colors.accent, modifier = Modifier.size(18.dp))
                        }
                    }

                    Box(Modifier.fillMaxWidth().padding(horizontal = 4.dp).height(0.5.dp).background(colors.border))

                    // Coze Bot ID
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Bot ID", fontSize = 15.sp, color = colors.textPrimary)
                            if (cozeBotId.isNotBlank()) {
                                Text(
                                    cozeBotId,
                                    fontSize = 11.sp, color = colors.success, fontFamily = FontFamily.Monospace
                                )
                            } else {
                                Text("未配置", fontSize = 11.sp, color = colors.textTertiary)
                            }
                        }
                        IconButton(onClick = { botIdInput = cozeBotId; showBotIdDialog = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑", tint = colors.accent, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // ============ 使用说明 ============
            item { SectionTitle("使用说明") }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CornerRadius.Card))
                        .background(colors.surface)
                        .padding(Spacing.CardSpacing)
                ) {
                    Text("1. 打开 coze.cn 创建 Bot", fontSize = 13.sp, color = colors.textSecondary)
                    Spacer(Modifier.height(4.dp))
                    Text("2. 获取 Bot ID（Bot 设置页面）", fontSize = 13.sp, color = colors.textSecondary)
                    Spacer(Modifier.height(4.dp))
                    Text("3. 在 coze.cn/open/oauth/pats 生成 API 令牌", fontSize = 13.sp, color = colors.textSecondary)
                    Spacer(Modifier.height(4.dp))
                    Text("4. 将 Bot ID 和 PAT 填入上方配置", fontSize = 13.sp, color = colors.textSecondary)
                    Spacer(Modifier.height(4.dp))
                    Text("5. 配置完成后即可开始对话", fontSize = 13.sp, color = colors.textSecondary)
                }
            }

            // ============ 版本信息 ============
            item { SectionTitle("关于") }
            item {
                SettingRow("版本", "v6.0.0")
            }
        }

        // Toast
        toastMessage?.let { msg ->
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                containerColor = colors.success
            ) {
                Text(msg, color = Color.White)
            }
        }
    }

    // ============ PAT 编辑 弹窗 ============
    if (showPatDialog) {
        AlertDialog(
            onDismissRequest = { showPatDialog = false },
            containerColor = colors.surface,
            title = { Text("设置 Coze API 令牌", color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium) },
            text = {
                Column {
                    Text("从 coze.cn/open/oauth/pats 获取", fontSize = 13.sp, color = colors.textTertiary)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = patInput, onValueChange = { patInput = it },
                        label = { Text("pat_xxx", color = colors.textTertiary) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = colors.accent, unfocusedBorderColor = colors.border, cursorColor = colors.accent
                        ),
                        shape = RoundedCornerShape(CornerRadius.Input), modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.saveCozePat(patInput.trim()); showPatDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.accent)) { Text("保存") } },
            dismissButton = { TextButton(onClick = { showPatDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.textTertiary)) { Text("取消") } }
        )
    }

    // ============ Bot ID 编辑弹窗 ============
    if (showBotIdDialog) {
        AlertDialog(
            onDismissRequest = { showBotIdDialog = false },
            containerColor = colors.surface,
            title = { Text("设置 Bot ID", color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium) },
            text = {
                Column {
                    Text("从 Coze Bot 设置页面获取", fontSize = 13.sp, color = colors.textTertiary)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = botIdInput, onValueChange = { botIdInput = it },
                        label = { Text("Bot ID", color = colors.textTertiary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = colors.accent, unfocusedBorderColor = colors.border, cursorColor = colors.accent
                        ),
                        shape = RoundedCornerShape(CornerRadius.Input), modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.saveCozeBotId(botIdInput.trim()); showBotIdDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.accent)) { Text("保存") } },
            dismissButton = { TextButton(onClick = { showBotIdDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.textTertiary)) { Text("取消") } }
        )
    }
}

@Composable
fun SettingRow(title: String, value: String, color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified, onClick: (() -> Unit)? = null) {
    val colors = LocalAppColors.current
    val textColor = if (color != androidx.compose.ui.graphics.Color.Unspecified) color else colors.textPrimary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(CornerRadius.Card))
            .background(colors.surface)
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
}

@Composable
fun SectionTitle(text: String) {
    val colors = LocalAppColors.current
    Text(text = text, fontSize = 13.sp, color = colors.textTertiary, letterSpacing = 1.sp, modifier = Modifier.padding(vertical = 8.dp))
}
