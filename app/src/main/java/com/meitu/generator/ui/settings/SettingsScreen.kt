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
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.meitu.generator.ui.theme.GlassColors
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
    val githubToken by viewModel.githubToken.collectAsState()
    val githubUser by viewModel.githubUser.collectAsState()
    val githubRepos by viewModel.githubRepos.collectAsState()
    val githubNotifications by viewModel.githubNotifications.collectAsState()
    val selectedRepoCommits by viewModel.selectedRepoCommits.collectAsState()
    val isLoadingGitHub by viewModel.isLoadingGitHub.collectAsState()
    val githubError by viewModel.githubError.collectAsState()

    // HuggingFace
    val hfToken by viewModel.hfToken.collectAsState()
    val hfModels by viewModel.hfModels.collectAsState()
    val isLoadingHf by viewModel.isLoadingHf.collectAsState()
    val hfError by viewModel.hfError.collectAsState()
    val hfTrendingTag by viewModel.hfTrendingTag.collectAsState()
    val context = LocalContext.current

    // 推送
    val serverchanKey by viewModel.serverchanKey.collectAsState()
    val pushplusToken by viewModel.pushplusToken.collectAsState()
    val pushTestResult by viewModel.pushTestResult.collectAsState()
    val isTestingPush by viewModel.isTestingPush.collectAsState()

    // 通用余额查询
    val balanceServices by viewModel.balanceServices.collectAsState()
    val balanceResults by viewModel.balanceResults.collectAsState()
    val isLoadingBalanceCheck by viewModel.isLoadingBalanceCheck.collectAsState()

    var showPatDialog by remember { mutableStateOf(false) }
    var showBotIdDialog by remember { mutableStateOf(false) }
    var showDeepseekDialog by remember { mutableStateOf(false) }
    var patInput by remember { mutableStateOf("") }
    var botIdInput by remember { mutableStateOf("") }
    var deepseekKeyInput by remember { mutableStateOf("") }
    var showCustomApiDialog by remember { mutableStateOf(false) }
    var editingCustomApi by remember { mutableStateOf<CustomApiConfig?>(null) }
    var showGitHubTokenDialog by remember { mutableStateOf(false) }
    var githubTokenInput by remember { mutableStateOf("") }
    var showGitHubCommits by remember { mutableStateOf<String?>(null) }
    var showHfTokenDialog by remember { mutableStateOf(false) }
    var hfTokenInput by remember { mutableStateOf("") }
    var hfSearchInput by remember { mutableStateOf("") }
    var showServerchanDialog by remember { mutableStateOf(false) }
    var serverchanInput by remember { mutableStateOf("") }
    var showPushplusDialog by remember { mutableStateOf(false) }
    var pushplusInput by remember { mutableStateOf("") }
    var showAddBalanceServiceDialog by remember { mutableStateOf(false) }
    var balanceServiceNameInput by remember { mutableStateOf("") }
    var balanceServiceUrlInput by remember { mutableStateOf("") }
    var balanceServiceKeyInput by remember { mutableStateOf("") }

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

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF000000))) {
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
                                        context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://platform.deepseek.com/usage")))
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

            // ============ GitHub 关联 ============
            item { SectionTitle("GitHub 关联") }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CornerRadius.Card))
                        .background(colors.surface)
                        .padding(Spacing.CardSpacing)
                ) {
                    // Token 输入
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Token", fontSize = 14.sp, color = colors.textPrimary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (githubToken.isNotBlank()) "已设置" else "未设置",
                                fontSize = 12.sp,
                                color = if (githubToken.isNotBlank()) colors.success else colors.textTertiary
                            )
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = { showGitHubTokenDialog = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "编辑", tint = colors.accent, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    if (githubUser != null) {
                        Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.border).padding(vertical = 4.dp))
                        Spacer(Modifier.height(8.dp))

                        // 用户信息
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(githubUser!!.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text("@${githubUser!!.login}", fontSize = 12.sp, color = colors.textTertiary)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row {
                            Text("${githubUser!!.publicRepos} 仓库", fontSize = 12.sp, color = colors.textSecondary)
                            Spacer(Modifier.width(12.dp))
                            Text("${githubUser!!.followers} 关注者", fontSize = 12.sp, color = colors.textSecondary)
                        }

                        Spacer(Modifier.height(8.dp))
                        Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.border))
                        Spacer(Modifier.height(8.dp))

                        // 刷新按钮
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("仓库列表", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                            if (isLoadingGitHub) {
                                Text("加载中...", fontSize = 12.sp, color = colors.textTertiary)
                            } else {
                                Text(
                                    "刷新",
                                    fontSize = 12.sp, color = colors.accent,
                                    modifier = Modifier.clickable { viewModel.refreshGitHubData() }
                                )
                            }
                        }

                        // 仓库列表
                        if (githubRepos.isEmpty() && !isLoadingGitHub) {
                            Spacer(Modifier.height(4.dp))
                            Text("暂无仓库", fontSize = 12.sp, color = colors.textTertiary)
                        } else {
                            githubRepos.take(5).forEach { repo ->
                                Spacer(Modifier.height(6.dp))
                                Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.border.copy(alpha = 0.3f)))
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showGitHubCommits = repo.fullName; viewModel.loadRepoCommits(repo.fullName) },
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(repo.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                        if (repo.description.isNotBlank()) {
                                            Text(repo.description, fontSize = 11.sp, color = colors.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        Row {
                                            if (repo.language != null) {
                                                Text("${repo.language}", fontSize = 10.sp, color = colors.textSecondary)
                                                Spacer(Modifier.width(8.dp))
                                            }
                                            Text("⭐${repo.stars}", fontSize = 10.sp, color = colors.textSecondary)
                                        }
                                    }
                                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(18.dp).align(Alignment.CenterVertically))
                                }
                            }
                            if (githubRepos.size > 5) {
                                Spacer(Modifier.height(4.dp))
                                Text("还有 ${githubRepos.size - 5} 个仓库...", fontSize = 11.sp, color = colors.textTertiary)
                            }
                        }

                        // 通知
                        if (githubNotifications.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.border))
                            Spacer(Modifier.height(8.dp))
                            Text("通知 (${githubNotifications.size})", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                            githubNotifications.take(5).forEach { n ->
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(if (n.type == "Issue") "🐛" else "🔄", fontSize = 12.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(n.title, fontSize = 12.sp, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${n.repoName} · ${n.updatedAt}", fontSize = 10.sp, color = colors.textTertiary)
                                    }
                                }
                            }
                        }

                        // 提交记录
                        if (showGitHubCommits != null && selectedRepoCommits.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.border))
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("最近提交 · ${showGitHubCommits}", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                Text("收起", fontSize = 12.sp, color = colors.accent, modifier = Modifier.clickable { showGitHubCommits = null })
                            }
                            selectedRepoCommits.forEach { c ->
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(c.sha, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = colors.accent)
                                    Spacer(Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(c.message, fontSize = 12.sp, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${c.author} · ${c.date}", fontSize = 10.sp, color = colors.textTertiary)
                                    }
                                }
                            }
                        }
                    }

                    if (githubError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text("⚠️ ${githubError}", fontSize = 12.sp, color = colors.error ?: colors.textTertiary)
                    }
                }
            }

            // ============ HuggingFace ============
            item { SectionTitle("HuggingFace 模型搜索") }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(CornerRadius.Card)).background(colors.surface).padding(Spacing.CardSpacing)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Token", fontSize = 14.sp, color = colors.textPrimary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (hfToken.isNotBlank()) "已设置" else "未设置", fontSize = 12.sp, color = if (hfToken.isNotBlank()) colors.success else colors.textTertiary)
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = { hfTokenInput = hfToken; showHfTokenDialog = true }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "编辑", tint = colors.accent, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.border))
                    Spacer(Modifier.height(8.dp))

                    // 搜索框
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = hfSearchInput, onValueChange = { hfSearchInput = it },
                            placeholder = { Text("搜索模型...", fontSize = 13.sp, color = colors.textTertiary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary,
                                focusedBorderColor = colors.accent, unfocusedBorderColor = colors.border, cursorColor = colors.accent
                            ),
                            shape = RoundedCornerShape(CornerRadius.Input),
                            modifier = Modifier.weight(1f).height(48.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                        )
                        Spacer(Modifier.width(8.dp))
                        TextButton(
                            onClick = { viewModel.searchHfModels(hfSearchInput) },
                            enabled = hfSearchInput.isNotBlank() && !isLoadingHf,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) { Text("搜索", fontSize = 13.sp, color = colors.accent) }
                    }

                    // 分类标签
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("text-generation", "image-generation", "voice", "embedding").forEach { tag ->
                            val label = when (tag) { "text-generation" -> "文本生成"; "image-generation" -> "图像生成"; "voice" -> "语音"; else -> "嵌入" }
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (hfTrendingTag == tag) colors.accent.copy(alpha = 0.15f) else colors.background)
                                    .clickable { viewModel.loadHfTrending(tag) }.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(label, fontSize = 11.sp, color = if (hfTrendingTag == tag) colors.accent else colors.textSecondary, fontWeight = if (hfTrendingTag == tag) FontWeight.SemiBold else FontWeight.Normal)
                            }
                        }
                    }

                    // 结果
                    if (isLoadingHf) {
                        Spacer(Modifier.height(8.dp))
                        Text("加载中...", fontSize = 12.sp, color = colors.textTertiary)
                    }
                    if (hfError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text("⚠️ ${hfError}", fontSize = 12.sp, color = colors.error ?: colors.textTertiary)
                    }
                    hfModels.take(5).forEach { model ->
                        Spacer(Modifier.height(6.dp))
                        Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.border.copy(alpha = 0.3f)))
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(model.modelId.substringAfterLast("/"), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                Text(model.author, fontSize = 10.sp, color = colors.textTertiary)
                                Row {
                                    Text("📥 ${(model.downloads.toInt()).formatTokenCount()}", fontSize = 10.sp, color = colors.textSecondary)
                                    Spacer(Modifier.width(8.dp))
                                    Text("❤️ ${model.likes}", fontSize = 10.sp, color = colors.textSecondary)
                                }
                            }
                            Text(model.pipelineTag, fontSize = 10.sp, color = colors.accent)
                        }
                    }
                }
            }

            // ============ 消息推送 ============
            item { SectionTitle("消息推送") }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(CornerRadius.Card)).background(colors.surface).padding(Spacing.CardSpacing)
                ) {
                    // Server酱
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Server酱 SendKey", fontSize = 14.sp, color = colors.textPrimary)
                            Text(if (serverchanKey.isNotBlank()) "已设置" else "未设置", fontSize = 11.sp, color = if (serverchanKey.isNotBlank()) colors.success else colors.textTertiary)
                        }
                        Row {
                            if (serverchanKey.isNotBlank()) {
                                TextButton(onClick = { viewModel.testServerchan() }, enabled = !isTestingPush, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                                    Text(if (isTestingPush) "测试中..." else "测试", fontSize = 12.sp, color = colors.accent)
                                }
                            }
                            IconButton(onClick = { serverchanInput = serverchanKey; showServerchanDialog = true }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "编辑", tint = colors.accent, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Box(Modifier.fillMaxWidth().padding(vertical = 6.dp).height(0.5.dp).background(colors.border))

                    // PushPlus
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("PushPlus Token", fontSize = 14.sp, color = colors.textPrimary)
                            Text(if (pushplusToken.isNotBlank()) "已设置" else "未设置", fontSize = 11.sp, color = if (pushplusToken.isNotBlank()) colors.success else colors.textTertiary)
                        }
                        Row {
                            if (pushplusToken.isNotBlank()) {
                                TextButton(onClick = { viewModel.testPushplus() }, enabled = !isTestingPush, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                                    Text(if (isTestingPush) "测试中..." else "测试", fontSize = 12.sp, color = colors.accent)
                                }
                            }
                            IconButton(onClick = { pushplusInput = pushplusToken; showPushplusDialog = true }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "编辑", tint = colors.accent, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // 测试结果
                    if (pushTestResult != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(pushTestResult!!, fontSize = 12.sp, color = if (pushTestResult!!.startsWith("✅")) colors.success else (colors.error ?: colors.textTertiary))
                    }
                }
            }

            // ============ 通用余额查询 ============
            item { SectionTitle("通用余额查询") }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(CornerRadius.Card)).background(colors.surface).padding(Spacing.CardSpacing)
                ) {
                    if (balanceServices.isEmpty()) {
                        Text("暂无余额查询服务", fontSize = 13.sp, color = colors.textTertiary)
                    } else {
                        balanceServices.forEachIndexed { index, service ->
                            if (index > 0) { Spacer(Modifier.height(6.dp)); Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.border)); Spacer(Modifier.height(6.dp)) }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(service.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                    Text(service.baseUrl, fontSize = 10.sp, color = colors.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (balanceResults.containsKey(service.id)) {
                                        Text("余额: ${balanceResults[service.id]}", fontSize = 12.sp, color = colors.success)
                                    }
                                }
                                Row {
                                    TextButton(onClick = { viewModel.queryBalance(service.id) }, enabled = !isLoadingBalanceCheck, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                                        Text("查询", fontSize = 12.sp, color = colors.accent)
                                    }
                                    IconButton(onClick = { viewModel.deleteBalanceService(service.id) }, modifier = Modifier.size(28.dp)) {
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
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable {
                            balanceServiceNameInput = ""; balanceServiceUrlInput = ""; balanceServiceKeyInput = ""; showAddBalanceServiceDialog = true
                        }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("＋ 添加余额查询", fontSize = 14.sp, color = colors.accent)
                    }

                    // 一键查询
                    if (balanceServices.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        TextButton(
                            onClick = { viewModel.queryAllBalances() },
                            enabled = !isLoadingBalanceCheck,
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)
                        ) {
                            Text(if (isLoadingBalanceCheck) "查询中..." else "🔄 一键查询全部", fontSize = 13.sp, color = colors.accent)
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
        val uriHandler = LocalUriHandler.current
        AlertDialog(
            onDismissRequest = { showPatDialog = false },
            containerColor = colors.surface,
            title = { Text("设置 Coze API 令牌", color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium) },
            text = {
                Column {
                    Text("从 Coze 获取 PAT 令牌", fontSize = 13.sp, color = colors.textTertiary)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { uriHandler.openUri("https://www.coze.cn/open/oauth/pats") },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("一键直达 获取PAT", fontSize = 14.sp) }
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
        val uriHandler = LocalUriHandler.current
        AlertDialog(
            onDismissRequest = { showDeepseekDialog = false },
            containerColor = colors.surface,
            title = { Text("设置 DeepSeek API Key", color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium) },
            text = {
                Column {
                    Text("从 DeepSeek 平台获取 API Key", fontSize = 13.sp, color = colors.textTertiary)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { uriHandler.openUri("https://platform.deepseek.com/api_keys") },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("一键直达 获取API Key", fontSize = 14.sp) }
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

        data class ProviderPreset(val name: String, val baseUrl: String, val model: String, val emoji: String)
        val presets = listOf(
            ProviderPreset("OpenAI", "https://api.openai.com/v1", "gpt-4o", "🤖"),
            ProviderPreset("DeepSeek", "https://api.deepseek.com", "deepseek-v4-flash", "🧠"),
            ProviderPreset("硅基流动", "https://api.siliconflow.cn/v1", "Qwen/Qwen2.5-72B-Instruct", "⚡"),
            ProviderPreset("通义千问", "https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen-turbo", "🌐"),
            ProviderPreset("月之暗面 Moonshot", "https://api.moonshot.cn/v1", "moonshot-v1-8k", "🌙"),
            ProviderPreset("智谱 GLM", "https://open.bigmodel.cn/api/paas/v4", "glm-4-plus", "🧪"),
            ProviderPreset("Anthropic Claude", "https://api.anthropic.com/v1", "claude-3-5-sonnet-20241022", "🎯"),
            ProviderPreset("Groq", "https://api.groq.com/openai/v1", "mixtral-8x7b-32768", "⚡"),
            ProviderPreset("Together AI", "https://api.together.xyz/v1", "mistralai/Mixtral-8x22B-Instruct-v0.1", "🔗"),
            ProviderPreset("Perplexity", "https://api.perplexity.ai", "sonar-pro", "🔍"),
            ProviderPreset("Agnes AI", "https://apihub.agnes-ai.com/v1", "agnes-2.0-flash", "🌿"),
        )

        var selectedPresetIndex by remember { mutableStateOf(-1) }
        var showDropdown by remember { mutableStateOf(false) }
        var nameInput by remember { mutableStateOf(editingCustomApi?.name ?: "") }
        var urlInput by remember { mutableStateOf(editingCustomApi?.baseUrl ?: "") }
        var keyInput by remember { mutableStateOf(editingCustomApi?.apiKey ?: "") }
        var modelInput by remember { mutableStateOf(editingCustomApi?.model ?: "") }
        var emojiInput by remember { mutableStateOf(editingCustomApi?.emoji ?: "🔌") }
        var showCustomFields by remember { mutableStateOf(isEditing) }

        AlertDialog(
            onDismissRequest = { showCustomApiDialog = false },
            containerColor = colors.surface,
            title = { Text(if (isEditing) "编辑自定义 API" else "添加自定义 API", color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium) },
            text = {
                Column {
                    Text("选择服务商，只需填写 API Key", fontSize = 13.sp, color = colors.textTertiary)
                    Spacer(Modifier.height(12.dp))

                    // 下拉选择服务商
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val selectedText = if (selectedPresetIndex >= 0) presets[selectedPresetIndex].emoji + " " + presets[selectedPresetIndex].name else "选择服务商..."
                        OutlinedTextField(
                            value = selectedText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("服务商", color = colors.textTertiary) },
                            trailingIcon = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, modifier = Modifier.clickable { showDropdown = !showDropdown }) },
                            modifier = Modifier.fillMaxWidth().clickable { showDropdown = !showDropdown },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary,
                                focusedBorderColor = colors.accent, unfocusedBorderColor = colors.border, cursorColor = colors.accent
                            ),
                            shape = RoundedCornerShape(CornerRadius.Input)
                        )
                        DropdownMenu(
                            expanded = showDropdown,
                            onDismissRequest = { showDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.9f).background(colors.surface)
                        ) {
                            presets.forEachIndexed { index, preset ->
                                DropdownMenuItem(
                                    text = { Text("${preset.emoji} ${preset.name}", fontSize = 14.sp, color = colors.textPrimary) },
                                    onClick = {
                                        selectedPresetIndex = index
                                        showDropdown = false
                                        nameInput = preset.name
                                        urlInput = preset.baseUrl
                                        modelInput = preset.model
                                        emojiInput = preset.emoji
                                        showCustomFields = false
                                    }
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), color = colors.border)
                            DropdownMenuItem(
                                text = { Text("🔧 自定义（手动填写全部）", fontSize = 14.sp, color = colors.textTertiary) },
                                onClick = {
                                    selectedPresetIndex = -1
                                    showDropdown = false
                                    showCustomFields = true
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // API Key 输入（始终显示）
                    OutlinedTextField(
                        value = keyInput, onValueChange = { keyInput = it },
                        label = { Text("API Key", color = colors.textTertiary) },
                        placeholder = { Text("sk-xxx", color = colors.textTertiary.copy(alpha = 0.5f)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = colors.accent, unfocusedBorderColor = colors.border, cursorColor = colors.accent
                        ),
                        shape = RoundedCornerShape(CornerRadius.Input), modifier = Modifier.fillMaxWidth()
                    )

                    // 自定义模式显示额外字段
                    if (showCustomFields) {
                        Spacer(Modifier.height(8.dp))
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
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (keyInput.isNotBlank() && (showCustomFields || selectedPresetIndex >= 0)) {
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

    // ============ HuggingFace Token 编辑弹窗 ============
    if (showHfTokenDialog) {
        val uriHandler = LocalUriHandler.current
        AlertDialog(
            onDismissRequest = { showHfTokenDialog = false },
            containerColor = colors.surface,
            title = { Text("HuggingFace Token", color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium) },
            text = {
                Column {
                    Text("从 HuggingFace 获取 Token（可选）", fontSize = 13.sp, color = colors.textTertiary)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { uriHandler.openUri("https://huggingface.co/settings/tokens") },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("一键直达 获取Token", fontSize = 14.sp) }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = hfTokenInput, onValueChange = { hfTokenInput = it },
                        label = { Text("Token", color = colors.textTertiary) },
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
            confirmButton = { TextButton(onClick = { viewModel.saveHfToken(hfTokenInput.trim()); showHfTokenDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.accent)) { Text("保存") } },
            dismissButton = { TextButton(onClick = { showHfTokenDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.textTertiary)) { Text("取消") } }
        )
    }

    // ============ GitHub Token 编辑弹窗 ============
    if (showGitHubTokenDialog) {
        val uriHandler = LocalUriHandler.current
        AlertDialog(
            onDismissRequest = { showGitHubTokenDialog = false },
            containerColor = colors.surface,
            title = { Text("GitHub Token", color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium) },
            text = {
                Column {
                    Text("从 GitHub Settings 获取 Personal Access Token", fontSize = 13.sp, color = colors.textTertiary)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { uriHandler.openUri("https://github.com/settings/tokens") },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("一键直达 生成Token", fontSize = 14.sp) }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = githubTokenInput, onValueChange = { githubTokenInput = it },
                        label = { Text("Token", color = colors.textTertiary) },
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
            confirmButton = { TextButton(onClick = { viewModel.saveGitHubToken(githubTokenInput.trim()); showGitHubTokenDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.accent)) { Text("保存") } },
            dismissButton = { TextButton(onClick = { showGitHubTokenDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.textTertiary)) { Text("取消") } }
        )
    }

    // ============ Server酱 Key 编辑弹窗 ============
    if (showServerchanDialog) {
        val uriHandler = LocalUriHandler.current
        AlertDialog(
            onDismissRequest = { showServerchanDialog = false },
            containerColor = colors.surface,
            title = { Text("Server酱 SendKey", color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium) },
            text = {
                Column {
                    Text("从 Server酱 获取 SendKey", fontSize = 13.sp, color = colors.textTertiary)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { uriHandler.openUri("https://sct.ftqq.com") },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("一键直达 获取SendKey", fontSize = 14.sp) }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = serverchanInput, onValueChange = { serverchanInput = it },
                        label = { Text("SendKey", color = colors.textTertiary) },
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
            confirmButton = { TextButton(onClick = { viewModel.saveServerchanKey(serverchanInput.trim()); showServerchanDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.accent)) { Text("保存") } },
            dismissButton = { TextButton(onClick = { showServerchanDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.textTertiary)) { Text("取消") } }
        )
    }

    // ============ PushPlus Token 编辑弹窗 ============
    if (showPushplusDialog) {
        val uriHandler = LocalUriHandler.current
        AlertDialog(
            onDismissRequest = { showPushplusDialog = false },
            containerColor = colors.surface,
            title = { Text("PushPlus Token", color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium) },
            text = {
                Column {
                    Text("从 PushPlus 获取 Token", fontSize = 13.sp, color = colors.textTertiary)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { uriHandler.openUri("https://www.pushplus.plus") },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("一键直达 获取Token", fontSize = 14.sp) }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pushplusInput, onValueChange = { pushplusInput = it },
                        label = { Text("Token", color = colors.textTertiary) },
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
            confirmButton = { TextButton(onClick = { viewModel.savePushplusToken(pushplusInput.trim()); showPushplusDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.accent)) { Text("保存") } },
            dismissButton = { TextButton(onClick = { showPushplusDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.textTertiary)) { Text("取消") } }
        )
    }

    // ============ 添加余额查询服务弹窗 ============
    if (showAddBalanceServiceDialog) {
        AlertDialog(
            onDismissRequest = { showAddBalanceServiceDialog = false },
            containerColor = colors.surface,
            title = { Text("添加余额查询", color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium) },
            text = {
                Column {
                    Text("支持 DeepSeek、硅基流动等常见 AI 服务商", fontSize = 13.sp, color = colors.textTertiary)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = balanceServiceNameInput, onValueChange = { balanceServiceNameInput = it },
                        label = { Text("名称", color = colors.textTertiary) },
                        placeholder = { Text("如：硅基流动", color = colors.textTertiary.copy(alpha = 0.5f)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = colors.accent, unfocusedBorderColor = colors.border, cursorColor = colors.accent
                        ),
                        shape = RoundedCornerShape(CornerRadius.Input), modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = balanceServiceUrlInput, onValueChange = { balanceServiceUrlInput = it },
                        label = { Text("Base URL", color = colors.textTertiary) },
                        placeholder = { Text("https://api.deepseek.com", color = colors.textTertiary.copy(alpha = 0.5f)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = colors.accent, unfocusedBorderColor = colors.border, cursorColor = colors.accent
                        ),
                        shape = RoundedCornerShape(CornerRadius.Input), modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = balanceServiceKeyInput, onValueChange = { balanceServiceKeyInput = it },
                        label = { Text("API Key", color = colors.textTertiary) },
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
            confirmButton = {
                TextButton(
                    onClick = {
                        if (balanceServiceNameInput.isNotBlank() && balanceServiceUrlInput.isNotBlank() && balanceServiceKeyInput.isNotBlank()) {
                            viewModel.addBalanceService(balanceServiceNameInput.trim(), balanceServiceUrlInput.trim(), balanceServiceKeyInput.trim())
                            showAddBalanceServiceDialog = false
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.accent),
                    enabled = balanceServiceNameInput.isNotBlank() && balanceServiceUrlInput.isNotBlank() && balanceServiceKeyInput.isNotBlank()
                ) { Text("添加") }
            },
            dismissButton = { TextButton(onClick = { showAddBalanceServiceDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = colors.textTertiary)) { Text("取消") } }
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
