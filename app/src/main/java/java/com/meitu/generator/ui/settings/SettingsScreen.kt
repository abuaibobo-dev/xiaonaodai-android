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
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val colors = LocalAppColors.current
    val currentModel by viewModel.currentBrainModel.collectAsState()
    val githubToken by viewModel.githubToken.collectAsState()
    val aiApiKey by viewModel.aiApiKey.collectAsState()
    val geminiApiKey by viewModel.geminiApiKey.collectAsState()
    val groqApiKey by viewModel.groqApiKey.collectAsState()
    val sambanovaApiKey by viewModel.sambanovaApiKey.collectAsState()
    val hfApiKey by viewModel.hfApiKey.collectAsState()
    val openrouterApiKey by viewModel.openrouterApiKey.collectAsState()
    val cerebrasApiKey by viewModel.cerebrasApiKey.collectAsState()
    val nvidiaApiKey by viewModel.nvidiaApiKey.collectAsState()
    val showClearConfirm by viewModel.showClearConfirm.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val privacyEnabled by viewModel.privacyModeEnabled.collectAsState()

    var showModelDialog by remember { mutableStateOf(false) }
    var showTokenDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showGeminiKeyDialog by remember { mutableStateOf(false) }
    var showGroqKeyDialog by remember { mutableStateOf(false) }
    var showSambanovaKeyDialog by remember { mutableStateOf(false) }
    var showHfKeyDialog by remember { mutableStateOf(false) }
    var showOpenRouterKeyDialog by remember { mutableStateOf(false) }
    var showCerebrasKeyDialog by remember { mutableStateOf(false) }
    var showNvidiaKeyDialog by remember { mutableStateOf(false) }
    var tokenInput by remember { mutableStateOf(githubToken) }
    var apiKeyInput by remember { mutableStateOf(aiApiKey) }
    var geminiKeyInput by remember { mutableStateOf(geminiApiKey) }
    var groqKeyInput by remember { mutableStateOf(groqApiKey) }
    var sambanovaKeyInput by remember { mutableStateOf(sambanovaApiKey) }
    var hfKeyInput by remember { mutableStateOf(hfApiKey) }
    var openrouterKeyInput by remember { mutableStateOf(openrouterApiKey) }
    var cerebrasKeyInput by remember { mutableStateOf(cerebrasApiKey) }
    var nvidiaKeyInput by remember { mutableStateOf(nvidiaApiKey) }
    var isTokenVisible by remember { mutableStateOf(false) }
    var isApiKeyVisible by remember { mutableStateOf(false) }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearToast()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = Spacing.PagePadding, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.SmallSpacing)
        ) {
            // ============ AI模型配置 ============
            item { SectionTitle("AI 模型配置") }
            item {
                SettingRow("AI 模型", currentModel, onClick = { showModelDialog = true })
            }
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
                        Text("API Key", fontSize = 15.sp, color = colors.textSecondary)
                        IconButton(onClick = { apiKeyInput = aiApiKey; showApiKeyDialog = true }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "编辑API Key",
                                tint = colors.accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    if (aiApiKey.isNotBlank()) {
                        Text(
                            "${aiApiKey.take(8)}...${aiApiKey.takeLast(4)}",
                            fontSize = 13.sp,
                            color = colors.success,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        Text("未配置 - 点击编辑添加 DeepSeek API Key", fontSize = 13.sp, color = colors.error)
                    }
                }
            }

            // ============ Google Gemini 配置（免费备用模型） ============
            item { SectionTitle("Google Gemini（免费备用）") }
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Google API Key", fontSize = 15.sp, color = colors.textSecondary)
                            Text("免费模型，DeepSeek 余额不足时自动切换", fontSize = 11.sp, color = colors.textTertiary)
                        }
                        IconButton(onClick = { geminiKeyInput = geminiApiKey; showGeminiKeyDialog = true }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "编辑 Google API Key",
                                tint = colors.accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    if (geminiApiKey.isNotBlank()) {
                        Text(
                            "${geminiApiKey.take(8)}...${geminiApiKey.takeLast(4)}",
                            fontSize = 13.sp,
                            color = colors.success,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        Text("未配置 - 点击编辑添加 Google API Key（从 aistudio.google.com 免费获取）", fontSize = 13.sp, color = colors.error)
                    }
                }
            }


            // ============ Groq 配置（免费备用模型） ============
            item { SectionTitle("Groq（免费备用）") }
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Groq API Key", fontSize = 15.sp, color = colors.textSecondary)
                            Text("免费模型，Gemini 也不可用时自动切换", fontSize = 11.sp, color = colors.textTertiary)
                        }
                        IconButton(onClick = { groqKeyInput = groqApiKey; showGroqKeyDialog = true }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "编辑 Groq API Key",
                                tint = colors.accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    if (groqApiKey.isNotBlank()) {
                        Text(
                            "${groqApiKey.take(8)}...${groqApiKey.takeLast(4)}",
                            fontSize = 13.sp,
                            color = colors.success,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        Text("未配置 - 点击编辑添加 Groq API Key（从 console.groq.com 免费获取）", fontSize = 13.sp, color = colors.error)
                    }
                }
            }

            // ============ SambaNova 配置（免费备用模型） ============
            item { SectionTitle("SambaNova（免费备用）") }
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text("SambaNova API Key", fontSize = 15.sp, color = colors.textSecondary)
                            Text("免费模型 Llama 3.1 405B/Qwen2.5 等", fontSize = 11.sp, color = colors.textTertiary)
                        }
                        IconButton(onClick = { sambanovaKeyInput = sambanovaApiKey; showSambanovaKeyDialog = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑 SambaNova API Key", tint = colors.accent, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    if (sambanovaApiKey.isNotBlank()) {
                        Text("${sambanovaApiKey.take(8)}...${sambanovaApiKey.takeLast(4)}", fontSize = 13.sp, color = colors.success, fontFamily = FontFamily.Monospace)
                    } else {
                        Text("未配置 - 点击编辑添加 SambaNova API Key（从 sambanova.ai 免费获取）", fontSize = 13.sp, color = colors.error)
                    }
                }
            }

            // ============ HuggingFace 配置（免费备用模型） ============
            item { SectionTitle("HuggingFace（免费备用）") }
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text("HuggingFace Token", fontSize = 15.sp, color = colors.textSecondary)
                            Text("免费额度 \$0.10/月，开源模型", fontSize = 11.sp, color = colors.textTertiary)
                        }
                        IconButton(onClick = { hfKeyInput = hfApiKey; showHfKeyDialog = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑 HuggingFace Token", tint = colors.accent, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    if (hfApiKey.isNotBlank()) {
                        Text("${hfApiKey.take(8)}...${hfApiKey.takeLast(4)}", fontSize = 13.sp, color = colors.success, fontFamily = FontFamily.Monospace)
                    } else {
                        Text("未配置 - 点击编辑添加 HuggingFace Token（从 huggingface.co 免费获取）", fontSize = 13.sp, color = colors.error)
                    }
                }
            }

            // ============ OpenRouter 配置（免费备用模型） ============
            item { SectionTitle("OpenRouter（免费备用）") }
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text("OpenRouter API Key", fontSize = 15.sp, color = colors.textSecondary)
                            Text("聚合 43+ 免费模型，GitHub 登录即可", fontSize = 11.sp, color = colors.textTertiary)
                        }
                        IconButton(onClick = { openrouterKeyInput = openrouterApiKey; showOpenRouterKeyDialog = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑 OpenRouter API Key", tint = colors.accent, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    if (openrouterApiKey.isNotBlank()) {
                        Text("${openrouterApiKey.take(8)}...${openrouterApiKey.takeLast(4)}", fontSize = 13.sp, color = colors.success, fontFamily = FontFamily.Monospace)
                    } else {
                        Text("未配置 - 点击编辑添加 OpenRouter API Key（从 openrouter.ai 免费获取）", fontSize = 13.sp, color = colors.error)
                    }
                }
            }

            // ============ Cerebras 配置（免费备用模型） ============
            item { SectionTitle("Cerebras（免费备用）") }
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Cerebras API Key", fontSize = 15.sp, color = colors.textSecondary)
                            Text("免费 30 请求/分钟，极速推理", fontSize = 11.sp, color = colors.textTertiary)
                        }
                        IconButton(onClick = { cerebrasKeyInput = cerebrasApiKey; showCerebrasKeyDialog = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑 Cerebras API Key", tint = colors.accent, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    if (cerebrasApiKey.isNotBlank()) {
                        Text("${cerebrasApiKey.take(8)}...${cerebrasApiKey.takeLast(4)}", fontSize = 13.sp, color = colors.success, fontFamily = FontFamily.Monospace)
                    } else {
                        Text("未配置 - 点击编辑添加 Cerebras API Key（从 cloud.cerebras.ai 免费获取）", fontSize = 13.sp, color = colors.error)
                    }
                }
            }

            // ============ NVIDIA NIM 配置（免费备用模型） ============
            item { SectionTitle("NVIDIA NIM（免费备用）") }
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text("NVIDIA API Key", fontSize = 15.sp, color = colors.textSecondary)
                            Text("免费 40 请求/分钟，129+ 模型", fontSize = 11.sp, color = colors.textTertiary)
                        }
                        IconButton(onClick = { nvidiaKeyInput = nvidiaApiKey; showNvidiaKeyDialog = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑 NVIDIA API Key", tint = colors.accent, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    if (nvidiaApiKey.isNotBlank()) {
                        Text("${nvidiaApiKey.take(8)}...${nvidiaApiKey.takeLast(4)}", fontSize = 13.sp, color = colors.success, fontFamily = FontFamily.Monospace)
                    } else {
                        Text("未配置 - 点击编辑添加 NVIDIA API Key（从 build.nvidia.com 免费获取）", fontSize = 13.sp, color = colors.error)
                    }
                }
            }

            // ============ 用量查询 ============
            item { SectionTitle("用量查询") }
            item {
                val balance by viewModel.balance.collectAsState()
                val balanceLoading by viewModel.balanceLoading.collectAsState()
                val context = LocalContext.current

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
                        Text("DeepSeek API 余额", fontSize = 15.sp, color = colors.textPrimary)
                        if (balanceLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = colors.accent,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        BalanceItem("已充值", "¥${balance.toppedUp}", colors.textPrimary)
                        BalanceItem("已使用", "¥${balance.used}", colors.error)
                        BalanceItem("剩余", "¥${balance.totalBalance}", colors.success)
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(colors.border))
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(CornerRadius.Button))
                            .background(colors.accent.copy(alpha = 0.1f))
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://platform.deepseek.com/usage"))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🌐", fontSize = 14.sp)
                        Spacer(Modifier.width(6.dp))
                        Text("一键直达官网查询用量", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.accent)
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
                        IconButton(onClick = { showTokenDialog = true }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "编辑Token",
                                tint = colors.accent,
                                modifier = Modifier.size(18.dp)
                            )
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

            // Toast message
            if (toastMessage != null) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(CornerRadius.Button))
                            .background(colors.accent.copy(alpha = 0.15f))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(toastMessage ?: "", fontSize = 15.sp, color = colors.accent)
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // Model selection dialog
    if (showModelDialog) {
        AlertDialog(
            onDismissRequest = { showModelDialog = false },
            containerColor = colors.surface,
            title = { Text("选择 AI 模型", color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium) },
            text = {
                Column {
                    Constants.AVAILABLE_MODELS.forEach { model ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(CornerRadius.Input))
                                .clickable {
                                    viewModel.setBrainModel(model)
                                    showModelDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(model, fontSize = 15.sp, color = colors.textPrimary)
                            if (model == currentModel) {
                                Text("✓", fontSize = 16.sp, color = colors.accent, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showModelDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.textTertiary)
                ) { Text("关闭") }
            }
        )
    }

    // Token edit dialog
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
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        label = { Text("ghp_xxxx", color = colors.textTertiary) },
                        singleLine = true,
                        visualTransformation = if (isTokenVisible)
                            androidx.compose.ui.text.input.VisualTransformation.None
                        else
                            androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isTokenVisible = !isTokenVisible }) {
                                Icon(
                                    if (isTokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = colors.textTertiary
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.border,
                            cursorColor = colors.accent
                        ),
                        shape = RoundedCornerShape(CornerRadius.Input),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.saveGithubToken(tokenInput.trim())
                        showTokenDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.accent)
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showTokenDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.textTertiary)
                ) { Text("取消") }
            }
        )
    }

    // API Key edit dialog
    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            containerColor = colors.surface,
            title = { Text("设置 AI API Key", color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium) },
            text = {
                Column {
                    Text("填入 DeepSeek API Key（从 platform.deepseek.com 获取）", fontSize = 13.sp, color = colors.textTertiary)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("API Key", color = colors.textTertiary) },
                        singleLine = true,
                        visualTransformation = if (isApiKeyVisible)
                            androidx.compose.ui.text.input.VisualTransformation.None
                        else
                            androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                Icon(
                                    if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = colors.textTertiary
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.border,
                            cursorColor = colors.accent
                        ),
                        shape = RoundedCornerShape(CornerRadius.Input),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.saveApiKey(apiKeyInput.trim())
                        showApiKeyDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.accent)
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showApiKeyDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.textTertiary)
                ) { Text("取消") }
            }
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

    // Gemini API Key edit dialog
    if (showGeminiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showGeminiKeyDialog = false },
            containerColor = colors.surface,
            title = { Text("设置 Google API Key", color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium) },
            text = {
                Column {
                    Text("从 aistudio.google.com 免费获取，用于 Gemini 备用模型", fontSize = 13.sp, color = colors.textTertiary)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = geminiKeyInput,
                        onValueChange = { geminiKeyInput = it },
                        label = { Text("Google API Key", color = colors.textTertiary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.border,
                            cursorColor = colors.accent
                        ),
                        shape = RoundedCornerShape(CornerRadius.Input),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.saveGeminiApiKey(geminiKeyInput.trim())
                        showGeminiKeyDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.accent)
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showGeminiKeyDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.textTertiary)
                ) { Text("取消") }
            }
        )
    }

    // Groq API Key edit dialog
    if (showGroqKeyDialog) {
        AlertDialog(
            onDismissRequest = { showGroqKeyDialog = false },
            containerColor = colors.surface,
            title = { Text("设置 Groq API Key", color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium) },
            text = {
                Column {
                    Text("从 console.groq.com 免费获取，用于 Groq 备用模型（Llama 3.3 70B）", fontSize = 13.sp, color = colors.textTertiary)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = groqKeyInput,
                        onValueChange = { groqKeyInput = it },
                        label = { Text("Groq API Key", color = colors.textTertiary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.border,
                            cursorColor = colors.accent
                        ),
                        shape = RoundedCornerShape(CornerRadius.Input),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.saveGroqApiKey(groqKeyInput.trim())
                        showGroqKeyDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.accent)
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showGroqKeyDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.textTertiary)
                ) { Text("取消") }
            }
        )
    }

    // SambaNova API Key dialog
    if (showSambanovaKeyDialog) {
        AlertDialog(
            onDismissRequest = { showSambanovaKeyDialog = false },
            containerColor = colors.surface,
            title = { Text("设置 SambaNova API Key", color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium) },
            text = {
                Column {
                    Text("从 sambanova.ai 免费获取", fontSize = 13.sp, color = colors.textTertiary)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = sambanovaKeyInput, onValueChange = { sambanovaKeyInput = it },
                        label = { Text("SambaNova API Key", color = colors.textTertiary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary, focusedBorderColor = colors.accent, unfocusedBorderColor = colors.border, cursorColor = colors.accent),
                        shape = RoundedCornerShape(CornerRadius.Input), modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.saveSambaNovaApiKey(sambanovaKeyInput.trim()); showSambanovaKeyDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.accent)) { Text("保存") } },
            dismissButton = { TextButton(onClick = { showSambanovaKeyDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.textTertiary)) { Text("取消") } }
        )
    }

    // HuggingFace Token dialog
    if (showHfKeyDialog) {
        AlertDialog(
            onDismissRequest = { showHfKeyDialog = false },
            containerColor = colors.surface,
            title = { Text("设置 HuggingFace Token", color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium) },
            text = {
                Column {
                    Text("从 huggingface.co 免费获取", fontSize = 13.sp, color = colors.textTertiary)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = hfKeyInput, onValueChange = { hfKeyInput = it },
                        label = { Text("HuggingFace Token", color = colors.textTertiary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary, focusedBorderColor = colors.accent, unfocusedBorderColor = colors.border, cursorColor = colors.accent),
                        shape = RoundedCornerShape(CornerRadius.Input), modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.saveHfApiKey(hfKeyInput.trim()); showHfKeyDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.accent)) { Text("保存") } },
            dismissButton = { TextButton(onClick = { showHfKeyDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.textTertiary)) { Text("取消") } }
        )
    }

    // OpenRouter API Key dialog
    if (showOpenRouterKeyDialog) {
        AlertDialog(
            onDismissRequest = { showOpenRouterKeyDialog = false },
            containerColor = colors.surface,
            title = { Text("设置 OpenRouter API Key", color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium) },
            text = {
                Column {
                    Text("从 openrouter.ai 免费获取，43+ 免费模型", fontSize = 13.sp, color = colors.textTertiary)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = openrouterKeyInput, onValueChange = { openrouterKeyInput = it },
                        label = { Text("OpenRouter API Key", color = colors.textTertiary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary, focusedBorderColor = colors.accent, unfocusedBorderColor = colors.border, cursorColor = colors.accent),
                        shape = RoundedCornerShape(CornerRadius.Input), modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.saveOpenRouterApiKey(openrouterKeyInput.trim()); showOpenRouterKeyDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.accent)) { Text("保存") } },
            dismissButton = { TextButton(onClick = { showOpenRouterKeyDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.textTertiary)) { Text("取消") } }
        )
    }

    // Cerebras API Key dialog
    if (showCerebrasKeyDialog) {
        AlertDialog(
            onDismissRequest = { showCerebrasKeyDialog = false },
            containerColor = colors.surface,
            title = { Text("设置 Cerebras API Key", color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium) },
            text = {
                Column {
                    Text("从 cloud.cerebras.ai 免费获取", fontSize = 13.sp, color = colors.textTertiary)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = cerebrasKeyInput, onValueChange = { cerebrasKeyInput = it },
                        label = { Text("Cerebras API Key", color = colors.textTertiary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary, focusedBorderColor = colors.accent, unfocusedBorderColor = colors.border, cursorColor = colors.accent),
                        shape = RoundedCornerShape(CornerRadius.Input), modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.saveCerebrasApiKey(cerebrasKeyInput.trim()); showCerebrasKeyDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.accent)) { Text("保存") } },
            dismissButton = { TextButton(onClick = { showCerebrasKeyDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.textTertiary)) { Text("取消") } }
        )
    }

    // NVIDIA API Key dialog
    if (showNvidiaKeyDialog) {
        AlertDialog(
            onDismissRequest = { showNvidiaKeyDialog = false },
            containerColor = colors.surface,
            title = { Text("设置 NVIDIA API Key", color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium) },
            text = {
                Column {
                    Text("从 build.nvidia.com 免费获取", fontSize = 13.sp, color = colors.textTertiary)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = nvidiaKeyInput, onValueChange = { nvidiaKeyInput = it },
                        label = { Text("NVIDIA API Key", color = colors.textTertiary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary, focusedBorderColor = colors.accent, unfocusedBorderColor = colors.border, cursorColor = colors.accent),
                        shape = RoundedCornerShape(CornerRadius.Input), modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.saveNvidiaApiKey(nvidiaKeyInput.trim()); showNvidiaKeyDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.accent)) { Text("保存") } },
            dismissButton = { TextButton(onClick = { showNvidiaKeyDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.textTertiary)) { Text("取消") } }
        )
    }
}

@Composable
fun SettingRow(title: String, value: String, color: Color = Color.Unspecified, onClick: (() -> Unit)? = null) {
    val colors = LocalAppColors.current
    val textColor = if (color != Color.Unspecified) color else colors.textPrimary
    
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth().height(52.dp)
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
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.border))
    }
}

@Composable
private fun BalanceItem(label: String, value: String, valueColor: Color) {
    val colors = LocalAppColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 12.sp, color = colors.textTertiary)
    }
}

@Composable
fun SectionTitle(text: String) {
    val colors = LocalAppColors.current
    Text(
        text = text,
        fontSize = 13.sp,
        color = colors.textTertiary,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}
