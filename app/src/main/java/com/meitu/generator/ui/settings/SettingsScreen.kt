package com.meitu.generator.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val colors = LocalAppColors.current
    val cozePat by viewModel.cozePat.collectAsState()
    val cozeBotId by viewModel.cozeBotId.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val totalTokens by viewModel.totalTokens.collectAsState()
    val cozeTokens by viewModel.cozeTokens.collectAsState()
    val cozeInputTokens by viewModel.cozeInputTokens.collectAsState()
    val cozeOutputTokens by viewModel.cozeOutputTokens.collectAsState()
    val cozeMessages by viewModel.cozeMessages.collectAsState()
    val dsTokens by viewModel.dsTokens.collectAsState()
    val dsInputTokens by viewModel.dsInputTokens.collectAsState()
    val dsOutputTokens by viewModel.dsOutputTokens.collectAsState()
    val dsMessages by viewModel.dsMessages.collectAsState()
    val agentList by viewModel.agentList.collectAsState()
    val currentAgentId by viewModel.currentAgentId.collectAsState()
    val isCreatingAgent by viewModel.isCreatingAgent.collectAsState()
    val currentChannel by viewModel.currentChannel.collectAsState()
    val deepseekApiKey by viewModel.deepseekApiKey.collectAsState()
    val deepseekModel by viewModel.deepseekModel.collectAsState()
    val deepseekBalance by viewModel.deepseekBalance.collectAsState()
    val isLoadingBalance by viewModel.isLoadingBalance.collectAsState()

    var showPatDialog by remember { mutableStateOf(false) }
    var showBotIdDialog by remember { mutableStateOf(false) }
    var showCreateAgentDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<AgentConfig?>(null) }
    var showDeepseekDialog by remember { mutableStateOf(false) }
    var patInput by remember { mutableStateOf("") }
    var botIdInput by remember { mutableStateOf("") }
    var deepseekKeyInput by remember { mutableStateOf("") }

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
            // ============ 我的 Agent ============
            item { SectionTitle("我的 Agent") }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CornerRadius.Card))
                        .background(colors.surface)
                        .padding(Spacing.CardSpacing)
                ) {
                    // 当前 Agent 指示
                    val currentAgent = agentList.find { it.botId == currentAgentId }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("当前 Agent", fontSize = 13.sp, color = colors.textTertiary)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                currentAgent?.let { "${it.emoji} ${it.name}" } ?: "🧠 布老师（默认）",
                                fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary
                            )
                        }
                    }

                    // Agent 列表
                    if (agentList.isNotEmpty()) {
                        Box(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 10.dp).height(0.5.dp).background(colors.border))

                        agentList.forEachIndexed { index, agent ->
                            val isActive = agent.botId == currentAgentId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isActive) colors.accent.copy(alpha = 0.1f) else Color.Transparent)
                                    .clickable { if (!isActive) viewModel.switchAgent(agent) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Emoji 头像
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(colors.accent.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(agent.emoji, fontSize = 16.sp)
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            agent.name,
                                            fontSize = 14.sp,
                                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            if (isActive) "使用中" else "点击切换",
                                            fontSize = 11.sp,
                                            color = if (isActive) colors.accent else colors.textTertiary
                                        )
                                    }
                                }
                                // 删除按钮
                                IconButton(
                                    onClick = { showDeleteConfirm = agent },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "删除",
                                        tint = colors.error.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            if (index < agentList.lastIndex) {
                                Box(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp).height(0.5.dp).background(colors.border.copy(alpha = 0.5f)))
                            }
                        }
                    }

                    // 新建 Agent 按钮
                    Box(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 10.dp).height(0.5.dp).background(colors.border))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showCreateAgentDialog = true }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "新建", tint = colors.accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("新建 Agent", fontSize = 14.sp, color = colors.accent, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // ============ AI 通道切换 ============
            item { SectionTitle("AI 通道") }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CornerRadius.Card))
                        .background(colors.surface)
                        .padding(Spacing.CardSpacing)
                ) {
                    // Coze 通道
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (currentChannel == "coze") colors.accent.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable { viewModel.switchChannel("coze") }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier.size(28.dp).clip(CircleShape).background(colors.accent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) { Text("🧠", fontSize = 14.sp) }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Coze Bot", fontSize = 14.sp, fontWeight = if (currentChannel == "coze") FontWeight.SemiBold else FontWeight.Normal, color = colors.textPrimary)
                                Text("走 Coze 平台，消耗 Coze 积分", fontSize = 11.sp, color = colors.textTertiary)
                            }
                        }
                        if (currentChannel == "coze") {
                            Text("✓", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.accent)
                        }
                    }

                    Box(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp).height(0.5.dp).background(colors.border.copy(alpha = 0.5f)))

                    // DeepSeek 通道
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (currentChannel == "deepseek") colors.accent.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable { viewModel.switchChannel("deepseek") }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier.size(28.dp).clip(CircleShape).background(colors.accent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) { Text("🔍", fontSize = 14.sp) }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("DeepSeek", fontSize = 14.sp, fontWeight = if (currentChannel == "deepseek") FontWeight.SemiBold else FontWeight.Normal, color = colors.textPrimary)
                                Text("直连 DeepSeek API，消耗账户余额", fontSize = 11.sp, color = colors.textTertiary)
                            }
                        }
                        if (currentChannel == "deepseek") {
                            Text("✓", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.accent)
                        }
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
                                Spacer(Modifier.width(8.dp))
                                TextButton(
                                    onClick = { viewModel.queryDeepseekBalance() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text("查询", fontSize = 12.sp, color = colors.accent)
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

                    Box(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 10.dp).height(0.5.dp).background(colors.border))

                    // ===== Coze 通道 =====
                    Text("Coze 通道", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.accent)
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("总消耗", fontSize = 10.sp, color = colors.textTertiary)
                            Text(cozeTokens.formatTokenCount(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("输入", fontSize = 10.sp, color = colors.textTertiary)
                            Text(cozeInputTokens.formatTokenCount(), fontSize = 13.sp, color = colors.textSecondary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("输出", fontSize = 10.sp, color = colors.textTertiary)
                            Text(cozeOutputTokens.formatTokenCount(), fontSize = 13.sp, color = colors.textSecondary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("次数", fontSize = 10.sp, color = colors.textTertiary)
                            Text("$cozeMessages 次", fontSize = 13.sp, color = colors.textSecondary)
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.border))
                    Spacer(Modifier.height(12.dp))

                    // ===== DeepSeek 通道 =====
                    Text("DeepSeek 通道", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.success)
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("总消耗", fontSize = 10.sp, color = colors.textTertiary)
                            Text(dsTokens.formatTokenCount(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("输入", fontSize = 10.sp, color = colors.textTertiary)
                            Text(dsInputTokens.formatTokenCount(), fontSize = 13.sp, color = colors.textSecondary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("输出", fontSize = 10.sp, color = colors.textTertiary)
                            Text(dsOutputTokens.formatTokenCount(), fontSize = 13.sp, color = colors.textSecondary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("次数", fontSize = 10.sp, color = colors.textTertiary)
                            Text("$dsMessages 次", fontSize = 13.sp, color = colors.textSecondary)
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

        // Loading overlay when creating agent
        if (isCreatingAgent) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = colors.accent)
                        Spacer(Modifier.height(16.dp))
                        Text("正在创建 Agent...", fontSize = 15.sp, color = colors.textPrimary)
                    }
                }
            }
        }
    }

    // ============ 新建 Agent 弹窗 ============
    if (showCreateAgentDialog) {
        var agentName by remember { mutableStateOf("") }
        var agentPrompt by remember { mutableStateOf("") }
        var agentEmoji by remember { mutableStateOf("🤖") }
        val emojiOptions = listOf("🤖", "🧠", "🎯", "📚", "🔬", "💡", "🎨", "🌟", "🔥", "💎")

        AlertDialog(
            onDismissRequest = { showCreateAgentDialog = false },
            containerColor = colors.surface,
            title = { Text("新建 Agent", color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium) },
            text = {
                Column {
                    // Emoji 选择
                    Text("选择头像", fontSize = 13.sp, color = colors.textTertiary)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(emojiOptions) { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (emoji == agentEmoji) colors.accent.copy(alpha = 0.2f)
                                        else colors.surface
                                    )
                                    .clickable { agentEmoji = emoji }
                                    .border(
                                        width = if (emoji == agentEmoji) 2.dp else 0.dp,
                                        color = if (emoji == agentEmoji) colors.accent else Color.Transparent,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 20.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                    // 名称
                    OutlinedTextField(
                        value = agentName, onValueChange = { agentName = it },
                        label = { Text("Agent 名称", color = colors.textTertiary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = colors.accent, unfocusedBorderColor = colors.border, cursorColor = colors.accent
                        ),
                        shape = RoundedCornerShape(CornerRadius.Input), modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))

                    // 提示词
                    OutlinedTextField(
                        value = agentPrompt, onValueChange = { agentPrompt = it },
                        label = { Text("系统提示词", color = colors.textTertiary) },
                        placeholder = { Text("描述这个 Agent 的角色和能力...", color = colors.textTertiary.copy(alpha = 0.5f)) },
                        singleLine = false,
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = colors.accent, unfocusedBorderColor = colors.border, cursorColor = colors.accent
                        ),
                        shape = RoundedCornerShape(CornerRadius.Input), modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.createAgent(agentName.trim(), agentPrompt.trim(), agentEmoji)
                        showCreateAgentDialog = false
                    },
                    enabled = agentName.isNotBlank() && agentPrompt.isNotBlank(),
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.accent)
                ) { Text("创建") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateAgentDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.textTertiary)) { Text("取消") }
            }
        )
    }

    // ============ 删除确认弹窗 ============
    showDeleteConfirm?.let { agent ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            containerColor = colors.surface,
            title = { Text("删除 Agent", color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium) },
            text = { Text("确定要删除「${agent.name}」吗？删除后无法恢复。", color = colors.textSecondary, fontSize = 14.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAgent(agent)
                        showDeleteConfirm = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.error)
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }, colors = ButtonDefaults.textButtonColors(contentColor = colors.textTertiary)) { Text("取消") }
            }
        )
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
