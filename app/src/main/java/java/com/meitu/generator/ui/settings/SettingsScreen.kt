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
    val deepseekApiKey by viewModel.deepseekApiKey.collectAsState()
    val googleApiKey by viewModel.googleApiKey.collectAsState()
    val openaiApiKey by viewModel.openaiApiKey.collectAsState()
    val groqApiKey by viewModel.groqApiKey.collectAsState()
    val siliconflowApiKey by viewModel.siliconflowApiKey.collectAsState()
    val moonshotApiKey by viewModel.moonshotApiKey.collectAsState()
    val zhipuApiKey by viewModel.zhipuApiKey.collectAsState()
    val showClearConfirm by viewModel.showClearConfirm.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val privacyEnabled by viewModel.privacyModeEnabled.collectAsState()

    var showTokenDialog by remember { mutableStateOf(false) }
    var showDeepSeekKeyDialog by remember { mutableStateOf(false) }
    var showGoogleKeyDialog by remember { mutableStateOf(false) }
    var showOpenAIKeyDialog by remember { mutableStateOf(false) }
    var showGroqKeyDialog by remember { mutableStateOf(false) }
    var showSiliconFlowKeyDialog by remember { mutableStateOf(false) }
    var showMoonshotKeyDialog by remember { mutableStateOf(false) }
    var showZhipuKeyDialog by remember { mutableStateOf(false) }
    var tokenInput by remember { mutableStateOf(githubToken) }
    var deepseekKeyInput by remember { mutableStateOf(deepseekApiKey) }
    var googleKeyInput by remember { mutableStateOf(googleApiKey) }
    var openaiKeyInput by remember { mutableStateOf(openaiApiKey) }
    var groqKeyInput by remember { mutableStateOf(groqApiKey) }
    var siliconflowKeyInput by remember { mutableStateOf(siliconflowApiKey) }
    var moonshotKeyInput by remember { mutableStateOf(moonshotApiKey) }
    var zhipuKeyInput by remember { mutableStateOf(zhipuApiKey) }

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
                    // DeepSeek
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("DeepSeek", fontSize = 15.sp, color = colors.textPrimary)
                            if (deepseekApiKey.isNotBlank()) {
                                Text(
                                    "${deepseekApiKey.take(8)}...${deepseekApiKey.takeLast(4)}",
                                    fontSize = 11.sp, color = colors.success, fontFamily = FontFamily.Monospace
                                )
                            } else {
                                Text("已配置默认 Key", fontSize = 11.sp, color = colors.textTertiary)
                            }
                        }
                        IconButton(onClick = { deepseekKeyInput = deepseekApiKey; showDeepSeekKeyDialog = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑", tint = colors.accent, modifier = Modifier.size(18.dp))
                        }
                    }

                    Box(Modifier.fillMaxWidth().padding(horizontal = 4.dp).height(0.5.dp).background(colors.border))

                    // Google AI (Gemini)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Google AI (Gemini)", fontSize = 15.sp, color = colors.textPrimary)
                            if (googleApiKey.isNotBlank()) {
                                Text(
                                    "${googleApiKey.take(8)}...${googleApiKey.takeLast(4)}",
                                    fontSize = 11.sp, color = colors.success, fontFamily = FontFamily.Monospace
                                )
                            } else {
                                Text("已配置默认 Key", fontSize = 11.sp, color = colors.textTertiary)
                            }
                        }
                        IconButton(onClick = { googleKeyInput = googleApiKey; showGoogleKeyDialog = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑", tint = colors.accent, modifier = Modifier.size(18.dp))
                        }
                    }

                    Box(Modifier.fillMaxWidth().padding(horizontal = 4.dp).height(0.5.dp).background(colors.border))

                    // OpenAI
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("OpenAI", fontSize = 15.sp, color = colors.textPrimary)
                            if (openaiApiKey.isNotBlank()) {
                                Text(
                                    "${openaiApiKey.take(8)}...${openaiApiKey.takeLast(4)}",
                                    fontSize = 11.sp, color = colors.success, fontFamily = FontFamily.Monospace
                                )
                            } else {
                                Text("未配置（使用时回退 DeepSeek）", fontSize = 11.sp, color = colors.textTertiary)
                            }
                        }
                        IconButton(onClick = { openaiKeyInput = openaiApiKey; showOpenAIKeyDialog = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑", tint = colors.accent, modifier = Modifier.size(18.dp))
                        }
                    }

                    Box(Modifier.fillMaxWidth().padding(horizontal = 4.dp).height(0.5.dp).background(colors.border))

                    // Groq
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Groq", fontSize = 15.sp, color = colors.textPrimary)
                            if (groqApiKey.isNotBlank()) {
                                Text(
                                    "${groqApiKey.take(8)}...${groqApiKey.takeLast(4)}",
                                    fontSize = 11.sp, color = colors.success, fontFamily = FontFamily.Monospace
                                )
                            } else {
                                Text("未配置（使用时回退 DeepSeek）", fontSize = 11.sp, color = colors.textTertiary)
                            }
                        }
                        IconButton(onClick = { groqKeyInput = groqApiKey; showGroqKeyDialog = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑", tint = colors.accent, modifier = Modifier.size(18.dp))
                        }
                    }

                    Box(Modifier.fillMaxWidth().padding(horizontal = 4.dp).height(0.5.dp).background(colors.border))

                    // 硅基流动 (SiliconFlow)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("硅基流动 (SiliconFlow)", fontSize = 15.sp, color = colors.textPrimary)
                            if (siliconflowApiKey.isNotBlank()) {
                                Text(
                                    "${siliconflowApiKey.take(8)}...${siliconflowApiKey.takeLast(4)}",
                                    fontSize = 11.sp, color = colors.success, fontFamily = FontFamily.Monospace
                                )
                            } else {
                                Text("未配置（使用时回退 DeepSeek）", fontSize = 11.sp, color = colors.textTertiary)
                            }
                        }
                        IconButton(onClick = { siliconflowKeyInput = siliconflowApiKey; showSiliconFlowKeyDialog = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑", tint = colors.accent, modifier = Modifier.size(18.dp))
                        }
                    }

                    Box(Modifier.fillMaxWidth().padding(horizontal = 4.dp).height(0.5.dp).background(colors.border))

                    // Moonshot (Kimi)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Moonshot (Kimi)", fontSize = 15.sp, color = colors.textPrimary)
                            if (moonshotApiKey.isNotBlank()) {
                                Text(
                                    "${moonshotApiKey.take(8)}...${moonshotApiKey.takeLast(4)}",
                                    fontSize = 11.sp, color = colors.success, fontFamily = FontFamily.Monospace
                                )
                            } else {
                                Text("未配置（使用时回退 DeepSeek）", fontSize = 11.sp, color = colors.textTertiary)
                            }
                        }
                        IconButton(onClick = { moonshotKeyInput = moonshotApiKey; showMoonshotKeyDialog = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑", tint = colors.accent, modifier = Modifier.size(18.dp))
                        }
                    }

                    Box(Modifier.fillMaxWidth().padding(horizontal = 4.dp).height(0.5.dp).background(colors.border))

                    // 智谱AI (Zhipu)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("智谱AI (Zhipu)", fontSize = 15.sp, color = colors.textPrimary)
                            if (zhipuApiKey.isNotBlank()) {
                                Text(
                                    "${zhipuApiKey.take(8)}...${zhipuApiKey.takeLast(4)}",
                                    fontSize = 11.sp, color = colors.success, fontFamily = FontFamily.Monospace
                                )
                            } else {
                                Text("未配置（使用时回退 DeepSeek）", fontSize = 11.sp, color = colors.textTertiary)
                            }
                        }
                        IconButton(onClick = { zhipuKeyInput = zhipuApiKey; showZhipuKeyDialog = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑", tint = colors.accent, modifier = Modifier.size(18.dp))
                        }
                    }
                }
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
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CornerRadius.Card))
                        .background(colors.surface)
                ) {
                    SettingRow("清理缓存", "", onClick = { viewModel.clearCache() })
                    Box(Modifier.fillMaxWidth().padding(horizontal = 4.dp).height(0.5.dp).background(colors.border))
                    SettingRow("清空对话历史", "", onClick = { viewModel.clearChatHistory() })
                    Box(Modifier.fillMaxWidth().padding(horizontal = 4.dp).height(0.5.dp).background(colors.border))
                    SettingRow("清空所有数据", "", color = colors.error, onClick = { viewModel.showClearAllConfirm() })
                }
            }

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

    // DeepSeek Key dialog
    if (showDeepSeekKeyDialog) {
        AlertDialog(
            onDismissRequest = { showDeepSeekKeyDialog = false },
            containerColor = colors.surface,
            title = { Text("设置 DeepSeek API Key", color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium) },
            text = {
                Column {
                    Text("从 platform.deepseek.com 获取，用于主力模型", fontSize = 13.sp, color = colors.textTertiary)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = deepseekKeyInput, onValueChange = { deepseekKeyInput = it },
                        label = { Text("DeepSeek API Key", color = colors.textTertiary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = colors.accent, unfocusedBorderColor = colors.border, cursorColor = colors.accent
                        ),
                        shape = RoundedCornerShape(CornerRadius.Input), modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.saveDeepSeekApiKey(deepseekKeyInput.trim()); showDeepSeekKeyDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.accent)) { Text("保存") } },
            dismissButton = { TextButton(onClick = { showDeepSeekKeyDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.textTertiary)) { Text("取消") } }
        )
    }



    // Google AI Key dialog
    if (showGoogleKeyDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleKeyDialog = false },
            containerColor = colors.surface,
            title = { Text("设置 Google AI API Key", color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium) },
            text = {
                Column {
                    Text("从 aistudio.google.com/apikey 获取，用于 Gemini 模型", fontSize = 13.sp, color = colors.textTertiary)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = googleKeyInput, onValueChange = { googleKeyInput = it },
                        label = { Text("Google AI API Key", color = colors.textTertiary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = colors.accent, unfocusedBorderColor = colors.border, cursorColor = colors.accent
                        ),
                        shape = RoundedCornerShape(CornerRadius.Input), modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.saveGoogleApiKey(googleKeyInput.trim()); showGoogleKeyDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.accent)) { Text("保存") } },
            dismissButton = { TextButton(onClick = { showGoogleKeyDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.textTertiary)) { Text("取消") } }
        )
    }

    // OpenAI Key dialog
    if (showOpenAIKeyDialog) {
        AlertDialog(
            onDismissRequest = { showOpenAIKeyDialog = false },
            containerColor = colors.surface,
            title = { Text("设置 OpenAI API Key", color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium) },
            text = {
                Column {
                    Text("从 platform.openai.com 获取，用于 GPT-4o 等模型", fontSize = 13.sp, color = colors.textTertiary)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = openaiKeyInput, onValueChange = { openaiKeyInput = it },
                        label = { Text("sk-xxx", color = colors.textTertiary) },
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
            confirmButton = { TextButton(onClick = { viewModel.saveOpenAIApiKey(openaiKeyInput.trim()); showOpenAIKeyDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.accent)) { Text("保存") } },
            dismissButton = { TextButton(onClick = { showOpenAIKeyDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.textTertiary)) { Text("取消") } }
        )
    }

    // Groq Key dialog
    if (showGroqKeyDialog) {
        AlertDialog(
            onDismissRequest = { showGroqKeyDialog = false },
            containerColor = colors.surface,
            title = { Text("设置 Groq API Key", color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium) },
            text = {
                Column {
                    Text("从 console.groq.com 获取，用于 Llama 等模型", fontSize = 13.sp, color = colors.textTertiary)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = groqKeyInput, onValueChange = { groqKeyInput = it },
                        label = { Text("gsk_xxx", color = colors.textTertiary) },
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
            confirmButton = { TextButton(onClick = { viewModel.saveGroqApiKey(groqKeyInput.trim()); showGroqKeyDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.accent)) { Text("保存") } },
            dismissButton = { TextButton(onClick = { showGroqKeyDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.textTertiary)) { Text("取消") } }
        )
    }

    // SiliconFlow Key dialog
    if (showSiliconFlowKeyDialog) {
        AlertDialog(
            onDismissRequest = { showSiliconFlowKeyDialog = false },
            containerColor = colors.surface,
            title = { Text("设置硅基流动 API Key", color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium) },
            text = {
                Column {
                    Text("从 cloud.siliconflow.cn 获取，用于 DeepSeek-V3/Qwen 等模型", fontSize = 13.sp, color = colors.textTertiary)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = siliconflowKeyInput, onValueChange = { siliconflowKeyInput = it },
                        label = { Text("SiliconFlow API Key", color = colors.textTertiary) },
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
            confirmButton = { TextButton(onClick = { viewModel.saveSiliconFlowApiKey(siliconflowKeyInput.trim()); showSiliconFlowKeyDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.accent)) { Text("保存") } },
            dismissButton = { TextButton(onClick = { showSiliconFlowKeyDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.textTertiary)) { Text("取消") } }
        )
    }

    // Moonshot Key dialog
    if (showMoonshotKeyDialog) {
        AlertDialog(
            onDismissRequest = { showMoonshotKeyDialog = false },
            containerColor = colors.surface,
            title = { Text("设置 Moonshot API Key", color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium) },
            text = {
                Column {
                    Text("从 platform.moonshot.cn 获取，用于 Kimi 模型", fontSize = 13.sp, color = colors.textTertiary)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = moonshotKeyInput, onValueChange = { moonshotKeyInput = it },
                        label = { Text("sk-xxx", color = colors.textTertiary) },
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
            confirmButton = { TextButton(onClick = { viewModel.saveMoonshotApiKey(moonshotKeyInput.trim()); showMoonshotKeyDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.accent)) { Text("保存") } },
            dismissButton = { TextButton(onClick = { showMoonshotKeyDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.textTertiary)) { Text("取消") } }
        )
    }

    // Zhipu Key dialog
    if (showZhipuKeyDialog) {
        AlertDialog(
            onDismissRequest = { showZhipuKeyDialog = false },
            containerColor = colors.surface,
            title = { Text("设置智谱AI API Key", color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium) },
            text = {
                Column {
                    Text("从 open.bigmodel.cn 获取，用于 GLM-4 等模型", fontSize = 13.sp, color = colors.textTertiary)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = zhipuKeyInput, onValueChange = { zhipuKeyInput = it },
                        label = { Text("智谱AI API Key", color = colors.textTertiary) },
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
            confirmButton = { TextButton(onClick = { viewModel.saveZhipuApiKey(zhipuKeyInput.trim()); showZhipuKeyDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.accent)) { Text("保存") } },
            dismissButton = { TextButton(onClick = { showZhipuKeyDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.textTertiary)) { Text("取消") } }
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
