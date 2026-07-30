package com.meitu.generator.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
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
import com.meitu.generator.util.Constants

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val colors = LocalAppColors.current
    val cozePat by viewModel.cozePat.collectAsState()
    val cozeBotId by viewModel.cozeBotId.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val totalTokens by viewModel.totalTokens.collectAsState()
    val channelTokenStats by viewModel.channelTokenStats.collectAsState()
    val deepseekApiKey by viewModel.deepseekApiKey.collectAsState()
    val deepseekModel by viewModel.deepseekModel.collectAsState()
    val deepseekBalance by viewModel.deepseekBalance.collectAsState()
    val isLoadingBalance by viewModel.isLoadingBalance.collectAsState()
    val customApiList by viewModel.customApiList.collectAsState()

    var showPatDialog by remember { mutableStateOf(false) }
    var showBotIdDialog by remember { mutableStateOf(false) }
    var showDeepseekDialog by remember { mutableStateOf(false) }
    var patInput by remember { mutableStateOf("") }
    var botIdInput by remember { mutableStateOf("") }
    var deepseekKeyInput by remember { mutableStateOf("") }
    var showCustomApiDialog by remember { mutableStateOf(false) }
    var editingCustomApi by remember { mutableStateOf<CustomApiConfig?>(null) }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearToast()
        }
    }

    // 每次进入设置页刷新 token 统计
    LaunchedEffect(Unit) {
        viewModel.refreshTokenUsage()
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = Spacing.PagePadding, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.ElementSpacing)
        ) {
            // ============ 自定义 API 配置 ============
            item { SectionTitle("自定义 API") }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CornerRadius.Card))
                        .background(colors.surface)
                        .padding(Spacing.CardSpacing)
                ) {
                    if (customApiList.isEmpty()) {
                        Text("暂无自定义 API", fontSize = 13.sp, color = colors.textTertiary)
                    } else {
                        customApiList.forEachIndexed { index, config ->
                            if (index > 0) Box(Modifier.fillMaxWidth().padding(vertical = 6.dp).height(0.5.dp).background(colors.border))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${config.emoji} ${config.name}", fontSize = 15.sp, color = colors.textPrimary)
                                    Text(config.baseUrl, fontSize = 11.sp, color = colors.textTertiary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                }
                                Row {
                                    IconButton(onClick = { editingCustomApi = config; showCustomApiDialog = true }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Edit, contentDescription = "编辑", tint = colors.accent, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { viewModel.deleteCustomApi(config.id) }, modifier = Modifier.size(32.dp)) {
                                        Text("✕", fontSize = 14.sp, color = colors.error ?: colors.textTertiary)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.border))
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { editingCustomApi = null; showCustomApiDialog = true }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("＋ 添加自定义 API", fontSize = 14.sp, color = colors.accent)
                    }
                }
            }

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

            // ============ DeepSeek API 配置 ============
            item { SectionTitle("DeepSeek API 配置") }
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
                            Text("DeepSeek API Key", fontSize = 15.sp, color = colors.textPrimary)
                            if (deepseekApiKey.isNotBlank()) {
                                Text(
                                    "${deepseekApiKey.take(6)}...${deepseekApiKey.takeLast(4)}",
                                    fontSize = 11.sp, color = colors.success, fontFamily = FontFamily.Monospace
                                )
                            } else {
                                Text("未配置", fontSize = 11.sp, color = colors.textTertiary)
                            }
                        }
                        IconButton(onClick = { deepseekKeyInput = deepseekApiKey; showDeepseekDialog = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑", tint = colors.accent, modifier = Modifier.size(18.dp))
                        }
                    }

                    if (deepseekApiKey.isNotBlank()) {
                        Box(Modifier.fillMaxWidth().padding(vertical = 8.dp).height(0.5.dp).background(colors.border))

                        // 模型选择
                        Text("模型选择", fontSize = 13.sp, color = colors.textTertiary)
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Constants.DEEPSEEK_MODELS.forEach { model ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (model == deepseekModel) colors.accent.copy(alpha = 0.15f) else colors.background)
                                        .clickable { viewModel.saveDeepseekModel(model) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        model,
                                        fontSize = 12.sp,
                                        color = if (model == deepseekModel) colors.accent else colors.textSecondary,
                                        fontWeight = if (model == deepseekModel) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                            }
                        }

                        // 余额查询
                        Box(Modifier.fillMaxWidth().padding(vertical = 8.dp).height(0.5.dp).background(colors.border))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("账户余额", fontSize = 14.sp, color = colors.textPrimary)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isLoadingBalance) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = colors.accent)
                                } else if (deepseekBalance != null) {
                                    Text(deepseekBalance!!, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.success)
                                }
                                Spacer(Modifier.width(4.dp))
                                TextButton(
                                    onClick = { viewModel.queryDeepseekBalance() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text("查询", fontSize = 12.sp, color = colors.accent)
                                }
                                TextButton(
                                    onClick = {
                                        val ctx = androidx.compose.ui.platform.LocalContext.current
                                        ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://platform.deepseek.com/usage")))
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text("直达", fontSize = 12.sp, color = colors.textTertiary)
                                }
                            }
                        }
                    }
                }
            }

            // ============ Token 消耗统计 ============
            item { SectionTitle("Token 消耗统计") }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CornerRadius.Card))
                        .background(colors.surface)
                        .padding(Spacing.CardSpacing)
                ) {
                    // 总消耗
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("累计消耗", fontSize = 15.sp, color = colors.textPrimary)
                        Text(
                            "${totalTokens.formatTokenCount()}",
                            fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.accent
                        )
                    }

                    // 各通道动态渲染
                    var isFirst = true
                    channelTokenStats.forEach { (channelKey, stats) ->
                        if (isFirst) {
                            isFirst = false
                            Box(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 10.dp).height(0.5.dp).background(colors.border))
                        } else {
                            Spacer(Modifier.height(12.dp))
                            Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.border))
                            Spacer(Modifier.height(12.dp))
                        }

                        val displayName = viewModel.getChannelDisplayName(channelKey)
                        Text(displayName, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (channelKey == "coze") colors.accent else if (channelKey == "deepseek") colors.success else colors.textPrimary)
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("总消耗", fontSize = 10.sp, color = colors.textTertiary)
                                Text(stats.total.formatTokenCount(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("输入", fontSize = 10.sp, color = colors.textTertiary)
                                Text(stats.input.formatTokenCount(), fontSize = 13.sp, color = colors.textSecondary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("输出", fontSize = 10.sp, color = colors.textTertiary)
                                Text(stats.output.formatTokenCount(), fontSize = 13.sp, color = colors.textSecondary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("次数", fontSize = 10.sp, color = colors.textTertiary)
                                Text("${stats.messages} 次", fontSize = 13.sp, color = colors.textSecondary)
                            }
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
                SettingRow("版本", "v6.1.0")
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

        // Loading overlay removed
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

    // ============ DeepSeek API Key 编辑弹窗 ============
    if (showDeepseekDialog) {
        AlertDialog(
            onDismissRequest = { showDeepseekDialog = false },
            containerColor = colors.surface,
            title = { Text("设置 DeepSeek API Key", color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium) },
            text = {
                Column {
                    Text("从 platform.deepseek.com 获取", fontSize = 13.sp, color = colors.textTertiary)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = deepseekKeyInput, onValueChange = { deepseekKeyInput = it },
                        label = { Text("sk-xxx", color = colors.textTertiary) },
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
            confirmButton = { TextButton(onClick = { viewModel.saveDeepseekApiKey(deepseekKeyInput.trim()); showDeepseekDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.accent)) { Text("保存") } },
            dismissButton = { TextButton(onClick = { showDeepseekDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.textTertiary)) { Text("取消") } }
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

    // ============ 自定义 API 编辑弹窗 ============
    if (showCustomApiDialog) {
        val isEditing = editingCustomApi != null
        var nameInput by remember { mutableStateOf(editingCustomApi?.name ?: "") }
        var urlInput by remember { mutableStateOf(editingCustomApi?.baseUrl ?: "") }
        var keyInput by remember { mutableStateOf(editingCustomApi?.apiKey ?: "") }
        var modelInput by remember { mutableStateOf(editingCustomApi?.model ?: "") }
        var emojiInput by remember { mutableStateOf(editingCustomApi?.emoji ?: "🔌") }

        AlertDialog(
            onDismissRequest = { showCustomApiDialog = false },
            containerColor = colors.surface,
            title = { Text(if (isEditing) "编辑自定义 API" else "添加自定义 API", color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium) },
            text = {
                Column {
                    Text("支持任何 OpenAI 兼容的 API", fontSize = 13.sp, color = colors.textTertiary)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = nameInput, onValueChange = { nameInput = it },
                        label = { Text("名称", color = colors.textTertiary) },
                        placeholder = { Text("如：OpenAI / 硅基流动", color = colors.textTertiary.copy(alpha = 0.5f)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = colors.accent, unfocusedBorderColor = colors.border, cursorColor = colors.accent
                        ),
                        shape = RoundedCornerShape(CornerRadius.Input), modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = urlInput, onValueChange = { urlInput = it },
                        label = { Text("API Base URL", color = colors.textTertiary) },
                        placeholder = { Text("https://api.openai.com/v1", color = colors.textTertiary.copy(alpha = 0.5f)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = colors.accent, unfocusedBorderColor = colors.border, cursorColor = colors.accent
                        ),
                        shape = RoundedCornerShape(CornerRadius.Input), modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = keyInput, onValueChange = { keyInput = it },
                        label = { Text("API Key", color = colors.textTertiary) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = colors.accent, unfocusedBorderColor = colors.border, cursorColor = colors.accent
                        ),
                        shape = RoundedCornerShape(CornerRadius.Input), modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = modelInput, onValueChange = { modelInput = it },
                        label = { Text("模型", color = colors.textTertiary) },
                        placeholder = { Text("如：gpt-4o / deepseek-v4-flash", color = colors.textTertiary.copy(alpha = 0.5f)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = colors.accent, unfocusedBorderColor = colors.border, cursorColor = colors.accent
                        ),
                        shape = RoundedCornerShape(CornerRadius.Input), modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = emojiInput, onValueChange = { emojiInput = it },
                        label = { Text("图标 (emoji)", color = colors.textTertiary) },
                        placeholder = { Text("🔌", color = colors.textTertiary.copy(alpha = 0.5f)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = colors.accent, unfocusedBorderColor = colors.border, cursorColor = colors.accent
                        ),
                        shape = RoundedCornerShape(CornerRadius.Input), modifier = Modifier.width(80.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (nameInput.isNotBlank() && urlInput.isNotBlank() && keyInput.isNotBlank() && modelInput.isNotBlank()) {
                            val config = CustomApiConfig(
                                id = editingCustomApi?.id ?: java.util.UUID.randomUUID().toString(),
                                name = nameInput.trim(),
                                baseUrl = urlInput.trim(),
                                apiKey = keyInput.trim(),
                                model = modelInput.trim(),
                                emoji = emojiInput.trim().ifBlank { "🔌" }
                            )
                            if (isEditing) viewModel.updateCustomApi(config) else viewModel.addCustomApi(config)
                            showCustomApiDialog = false
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.accent),
                    enabled = nameInput.isNotBlank() && urlInput.isNotBlank() && keyInput.isNotBlank() && modelInput.isNotBlank()
                ) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showCustomApiDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.textTertiary)) { Text("取消") } }
        )
    }
}

@Composable
private fun ChannelRow(
    emoji: String,
    name: String,
    desc: String,
    isActive: Boolean,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) colors.accent.copy(alpha = 0.1f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier.size(24.dp).clip(CircleShape).background(colors.accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) { Text(emoji, fontSize = 12.sp) }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(name, fontSize = 14.sp, fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal, color = colors.textPrimary)
                Text(desc, fontSize = 11.sp, color = colors.textTertiary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onEdit != null) {
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑", tint = colors.textTertiary, modifier = Modifier.size(14.dp))
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Text("✕", fontSize = 14.sp, color = colors.error ?: colors.textTertiary)
                }
            }
            if (isActive) {
                Spacer(Modifier.width(4.dp))
                Text("✓", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.accent)
            }
        }
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

private fun Int.formatTokenCount(): String = when {
    this == 0 -> "0"
    this < 1000 -> "$this"
    this < 10000 -> String.format("%.1fK", this / 1000.0)
    this < 1000000 -> "${this / 1000}K"
    else -> String.format("%.1fM", this / 1000000.0)
}
