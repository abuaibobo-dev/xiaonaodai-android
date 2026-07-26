package com.meitu.generator.ui.assistant

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.meitu.generator.data.agent.ThinkingChainManager
import com.meitu.generator.ui.AppEvents
import com.meitu.generator.ui.theme.LocalAppColors
import kotlinx.coroutines.delay

// ============ 消息段落类型 ============
private sealed class MessageSegment {
    data class TextSegment(val text: String) : MessageSegment()
    data class CodeSegment(val language: String, val code: String, val fileName: String?) : MessageSegment()
}

private fun parseMessageSegments(text: String): List<MessageSegment> {
    val segments = mutableListOf<MessageSegment>()
    val codeBlockPattern = Regex("```(\\w*)\\n(.*?)```", RegexOption.DOT_MATCHES_ALL)
    var lastIndex = 0
    for (match in codeBlockPattern.findAll(text)) {
        if (match.range.first > lastIndex) {
            val plainText = text.substring(lastIndex, match.range.first).trim()
            if (plainText.isNotEmpty()) segments.add(MessageSegment.TextSegment(cleanInlineMarkdown(plainText)))
        }
        val lang = match.groupValues[1].ifEmpty { "code" }
        val code = match.groupValues[2].trimEnd()
        val fileName = extractFileNameFromCode(code, lang)
        segments.add(MessageSegment.CodeSegment(lang, code, fileName))
        lastIndex = match.range.last + 1
    }
    if (lastIndex < text.length) {
        val remaining = text.substring(lastIndex).trim()
        if (remaining.isNotEmpty()) segments.add(MessageSegment.TextSegment(cleanInlineMarkdown(remaining)))
    }
    if (segments.isEmpty()) segments.add(MessageSegment.TextSegment(cleanInlineMarkdown(text)))
    return segments
}

private fun extractFileNameFromCode(code: String, lang: String): String? {
    val packageMatch = Regex("^package\\s+([\\w.]+)", RegexOption.MULTILINE).find(code)
    if (packageMatch != null) {
        val classMatch = Regex("(?:class|object|interface)\\s+(\\w+)").find(code)
        if (classMatch != null) return "${classMatch.groupValues[1]}.$lang"
        return "${packageMatch.groupValues[1].substringAfterLast(".")}.$lang"
    }
    return null
}

private fun cleanInlineMarkdown(text: String): String {
    return text
        .replace(Regex("`([^`]+)`"), "$1")
        .replace(Regex("\\*{1,3}([^*]+)\\*{1,3}"), "$1")
        .replace(Regex("_{1,2}([^_]+)_{1,2}"), "$1")
        .replace(Regex("^#{1,6}\\s+", RegexOption.MULTILINE), "")
        .replace(Regex("^\\s*[-]\\s+", RegexOption.MULTILINE), "• ")
        .replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
}

// ============ 代码块卡片 ============
@Composable
private fun CodeBlockCard(language: String, code: String, fileName: String?) {
    val codeBgColor = Color(0xFF1E1E1E)
    val codeTextColor = Color(0xFFD4D4D4)
    val codeBorderColor = Color(0xFF333333)
    val codeHeaderColor = Color(0xFF252525)
    val codeFooterColor = Color(0xFF8A8A8A)
    val accentColor = Color(0xFFC9A96E)
    val lineCount = code.lines().size
    val codeSizeBytes = code.toByteArray().size
    val codeSizeStr = if (codeSizeBytes >= 1024) "%.1fKB".format(codeSizeBytes / 1024.0) else "${codeSizeBytes}B"
    val displayFileName = fileName ?: "code.$language"
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp, max = 150.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(codeBgColor)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().background(codeHeaderColor).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📄", fontSize = 12.sp)
            Spacer(Modifier.width(6.dp))
            Text(displayFileName, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = codeTextColor)
            Spacer(Modifier.weight(1f))
            Text(codeSizeStr, fontSize = 10.sp, color = codeFooterColor)
        }
        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(codeBorderColor))
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp, vertical = 8.dp).verticalScroll(scrollState)
        ) {
            Text(text = code, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = codeTextColor, lineHeight = 18.sp)
        }
        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(codeBorderColor))
        Row(
            modifier = Modifier.fillMaxWidth().background(codeHeaderColor).padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("共 $lineCount 行", fontSize = 10.sp, color = codeFooterColor)
            Text(" · ", fontSize = 10.sp, color = codeFooterColor)
            Text(language.replaceFirstChar { it.uppercase() }, fontSize = 10.sp, color = accentColor, fontWeight = FontWeight.Medium)
        }
    }
}

// ============ 深度思考展示卡片 ============
@Composable
private fun ReasoningCard(reasoning: String) {
    val colors = LocalAppColors.current
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1A1A2E))
            .border(0.5.dp, Color(0xFF3A3A5E), RoundedCornerShape(10.dp))
            .clickable { expanded = !expanded }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("💭", fontSize = 14.sp)
            Spacer(Modifier.width(6.dp))
            Text("深度思考过程", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF9B9BCC))
            Spacer(Modifier.weight(1f))
            Text(if (expanded) "收起" else "展开", fontSize = 11.sp, color = Color(0xFF7A7AAA))
        }
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = reasoning,
                    fontSize = 12.sp,
                    color = Color(0xFFB0B0D0),
                    lineHeight = 18.sp,
                    fontFamily = FontFamily.Serif
                )
            }
        }
    }
}

// ============ 主屏幕 ============
@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel = hiltViewModel()
) {
    val colors = LocalAppColors.current
    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val agentStatus by viewModel.agentStatus.collectAsState()
    val animatedMessageIds = viewModel.animatedMessageIds
    val pendingImageUri by viewModel.pendingImageUri.collectAsState()
    val deepThinking by viewModel.deepThinkingEnabled.collectAsState()
    val webSearch by viewModel.webSearchEnabled.collectAsState()
    val listState = rememberLazyListState()

    val thinkingChain by viewModel.thinkingChain.collectAsState()
    val isThinkingActive by viewModel.isThinkingChainActive.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    LaunchedEffect(Unit) {
        AppEvents.events.collect { event ->
            if (event == "clear_chat") viewModel.clearMessages()
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { viewModel.setPendingImage(it.toString()) } }

    val lastMessageId = messages.lastOrNull()?.id
    LaunchedEffect(lastMessageId, isLoading) {
        if (messages.isNotEmpty()) {
            delay(100)
            val targetIndex = messages.size - 1 + (if (isLoading) 1 else 0)
            try { listState.scrollToItem(index = targetIndex, scrollOffset = 0) } catch (_: Exception) {}
            delay(50)
            try { listState.animateScrollToItem(index = targetIndex, scrollOffset = 0) } catch (_: Exception) {}
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        // 消息列表 + 滚动到底部浮动按钮
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (messages.size <= 1 && !isLoading) { item { EmptyState() } }

                items(messages, key = { it.id }) { msg ->
                    when {
                        msg.isSystem -> SystemMessageBubble(msg.text)
                        msg.taskProgress != null -> TaskProgressBubble(msg)
                        else -> TextMessageBubble(msg, animatedMessageIds, viewModel)
                    }
                }

                if (agentStatus != null) {
                    item { AgentStatusBubble(status = agentStatus!!) }
                }

                if (isLoading && agentStatus == null) {
                    item { ThinkingChainIndicator(thinkingChain = thinkingChain, isActive = isThinkingActive) }
                }

                item { Spacer(Modifier.height(8.dp)) }
            }

            // 滚动到底部浮动按钮
            val canScrollDown by remember {
                derivedStateOf {
                    val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    val totalItems = listState.layoutInfo.totalItemsCount
                    lastVisible < totalItems - 2
                }
            }
            if (canScrollDown) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 8.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colors.surface.copy(alpha = 0.85f))
                        .border(0.5.dp, colors.border, CircleShape)
                        .clickable {
                            try {
                                val lastIndex = listState.layoutInfo.totalItemsCount - 1
                                listState.animateScrollToItem(lastIndex)
                            } catch (_: Exception) {}
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "滚动到底部",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // ============ 图片预览区 ============
        if (pendingImageUri != null) {
            ImagePreview(imageUri = pendingImageUri!!, onRemove = { viewModel.removePendingImage() })
        }

        // ============ 输入区域（集成模型切换+模式切换+发送） ============
        val brainModel by viewModel.brainModel.collectAsState()
        val availableBrainModels = viewModel.availableBrainModels
        var showModelDropdown by remember { mutableStateOf(false) }

        SmartInputBar(
            inputText = inputText,
            isLoading = isLoading,
            pendingImageUri = pendingImageUri,
            deepThinking = deepThinking,
            webSearch = webSearch,
            currentModel = brainModel,
            availableModels = availableBrainModels,
            showModelDropdown = showModelDropdown,
            onInputChange = { viewModel.setInput(it) },
            onSend = { viewModel.sendInput() },
            onPickImage = { imagePickerLauncher.launch("image/*") },
            onToggleDeepThinking = { viewModel.toggleDeepThinking() },
            onToggleWebSearch = { viewModel.toggleWebSearch() },
            onToggleModelDropdown = { showModelDropdown = !showModelDropdown },
            onSelectModel = { model ->
                viewModel.switchBrainModel(model)
                showModelDropdown = false
            }
        )
    }

    // P0修复：错误提示 Snackbar（不混入聊天流）
    if (errorMessage != null) {
        LaunchedEffect(errorMessage) {
            kotlinx.coroutines.delay(3500)
            viewModel.clearErrorMessage()
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFF4444).copy(alpha = 0.92f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(errorMessage ?: "", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ============ 模式 Chip（嵌入输入框工具栏） ============
@Composable
private fun SmartModeChip(
    icon: String,
    label: String,
    active: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val bgColor by animateColorAsState(
        if (active) activeColor.copy(alpha = 0.15f) else Color.Transparent,
        animationSpec = tween(200), label = "chipBg"
    )
    val borderColor by animateColorAsState(
        if (active) activeColor.copy(alpha = 0.4f) else colors.border.copy(alpha = 0.5f),
        animationSpec = tween(200), label = "chipBorder"
    )
    val textColor by animateColorAsState(
        if (active) activeColor else colors.textTertiary,
        animationSpec = tween(200), label = "chipText"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(0.5.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 11.sp)
            Spacer(Modifier.width(3.dp))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = textColor)
            if (active) {
                Spacer(Modifier.width(2.dp))
                Box(
                    modifier = Modifier.size(4.dp).clip(CircleShape).background(activeColor)
                )
            }
        }
    }
}

// ============ 空状态 ============
@Composable
private fun EmptyState() {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("\uD83D\uDCAC", fontSize = 36.sp)
            Text("开始对话，我会记住你的每一次提问", fontSize = 15.sp, color = colors.textTertiary)
        }
    }
}

// ============ 智能输入栏（所有控件集成在输入框内） ============
@Composable
private fun SmartInputBar(
    inputText: String,
    isLoading: Boolean,
    pendingImageUri: String?,
    deepThinking: Boolean,
    webSearch: Boolean,
    currentModel: String,
    availableModels: List<String>,
    showModelDropdown: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onPickImage: () -> Unit,
    onToggleDeepThinking: () -> Unit,
    onToggleWebSearch: () -> Unit,
    onToggleModelDropdown: () -> Unit,
    onSelectModel: (String) -> Unit
) {
    val colors = LocalAppColors.current
    val canSend = (inputText.isNotBlank() || pendingImageUri != null) && !isLoading

    Box(modifier = Modifier.fillMaxWidth()) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(colors.border))

            // 输入区域主体 - 统一的输入框
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.background)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                // 统一输入框容器（包含文本+所有控件）
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surface)
                        .border(0.5.dp, colors.border, RoundedCornerShape(16.dp))
                ) {
                    Column {
                        // 文本输入区域（更高）
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 72.dp, max = 160.dp)
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            androidx.compose.foundation.text.BasicTextField(
                                value = inputText,
                                onValueChange = onInputChange,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 15.sp, color = colors.textPrimary, lineHeight = 22.sp
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.accent),
                                decorationBox = { innerTextField ->
                                    Box {
                                        if (inputText.isEmpty() && pendingImageUri == null) {
                                            androidx.compose.foundation.text.BasicText(
                                                text = "说点什么...",
                                                style = androidx.compose.ui.text.TextStyle(
                                                    fontSize = 15.sp, color = colors.textTertiary, lineHeight = 22.sp
                                                )
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }

                        // 分割线
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .height(0.5.dp)
                                .background(colors.border.copy(alpha = 0.5f))
                        )

                        // 底部工具栏（所有控件在这一行）
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 左侧：模型选择 + 功能chips + 图片
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 模型选择器
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colors.background)
                                        .border(0.5.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                        .clickable { onToggleModelDropdown() }
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🤖", fontSize = 11.sp)
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            com.meitu.generator.data.agent.ModelRouter.getModelDisplayName(currentModel),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.textSecondary,
                                            maxLines = 1
                                        )
                                        Spacer(Modifier.width(2.dp))
                                        Text("▾", fontSize = 9.sp, color = colors.textTertiary)
                                    }
                                }

                                SmartModeChip(
                                    icon = "🧠",
                                    label = "思考",
                                    active = deepThinking,
                                    activeColor = Color(0xFF6C5CE7),
                                    onClick = onToggleDeepThinking
                                )
                                SmartModeChip(
                                    icon = "🌐",
                                    label = "搜索",
                                    active = webSearch,
                                    activeColor = Color(0xFF00B894),
                                    onClick = onToggleWebSearch
                                )
                                // 附件按钮（别针图标）
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(if (pendingImageUri != null) colors.accent.copy(alpha = 0.15f) else Color.Transparent)
                                        .border(0.5.dp, if (pendingImageUri != null) colors.accent.copy(alpha = 0.4f) else colors.border.copy(alpha = 0.5f), CircleShape)
                                        .clickable { onPickImage() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "📎",
                                        fontSize = 14.sp,
                                        color = if (pendingImageUri != null) colors.accent else colors.textTertiary
                                    )
                                }
                            }

                            // 右侧：发送按钮
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (canSend) colors.accent else colors.border.copy(alpha = 0.3f))
                                    .clickable(enabled = canSend) { onSend() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Send,
                                    contentDescription = "发送",
                                    tint = if (canSend) Color.White else colors.textTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 模型选择下拉菜单
        if (showModelDropdown) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 20.dp, top = 100.dp)
            ) {
                DropdownMenu(
                    expanded = showModelDropdown,
                    onDismissRequest = { onToggleModelDropdown() },
                    modifier = Modifier.background(colors.surface)
                ) {
                    availableModels.forEach { model ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(com.meitu.generator.data.agent.ModelRouter.getModelDisplayName(model), fontSize = 14.sp, color = colors.textPrimary)
                                    if (model == currentModel) {
                                        Spacer(Modifier.width(8.dp))
                                        Text("✓", fontSize = 14.sp, color = colors.accent, fontWeight = FontWeight.Bold)
                                    }
                                }
                            },
                            onClick = { onSelectModel(model) }
                        )
                    }
                }
            }
        }
    }
}

// ============ Agent 实时状态气泡 ============
@Composable
private fun AgentStatusBubble(status: String) {
    val colors = LocalAppColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "status")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(800, easing = EaseInOutSine), repeatMode = RepeatMode.Reverse),
        label = "pulse"
    )

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Box(
            modifier = Modifier
                .widthIn(max = 240.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface.copy(alpha = 0.6f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(6.dp).clip(CircleShape).alpha(pulseAlpha).background(colors.accent)
                )
                Spacer(Modifier.width(8.dp))
                Text(status, fontSize = 13.sp, color = colors.textSecondary)
            }
        }
    }
}

// ============ 思维链指示器 ============
@Composable
private fun ThinkingChainIndicator(
    thinkingChain: List<ThinkingChainManager.ThinkingStep>,
    isActive: Boolean
) {
    val colors = LocalAppColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(800, easing = EaseInOutSine), repeatMode = RepeatMode.Reverse),
        label = "pulse"
    )

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                .background(colors.surface)
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).alpha(if (isActive) pulseAlpha else 0f).background(colors.accent))
                    Spacer(Modifier.width(8.dp))
                    Text("思考中", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textSecondary)
                }
                if (thinkingChain.isNotEmpty()) {
                    thinkingChain.takeLast(3).forEach { step ->
                        val icon = when (step.type) {
                            ThinkingChainManager.StepType.UNDERSTANDING -> "\uD83E\uDDE0"
                            ThinkingChainManager.StepType.PLANNING -> "\uD83D\uDCCB"
                            ThinkingChainManager.StepType.RETRIEVING -> "\uD83D\uDD0D"
                            ThinkingChainManager.StepType.REASONING -> "\uD83D\uDCAD"
                            ThinkingChainManager.StepType.DECIDING -> "⚖️"
                            ThinkingChainManager.StepType.EXECUTING -> "⚡"
                            ThinkingChainManager.StepType.VERIFYING -> "✅"
                            ThinkingChainManager.StepType.RESPONDING -> "\uD83D\uDCAC"
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(icon, fontSize = 11.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(step.content.take(50), fontSize = 12.sp, color = colors.textTertiary, maxLines = 1)
                        }
                    }
                } else {
                    Text("正在分析你的问题...", fontSize = 12.sp, color = colors.textTertiary)
                }
            }
        }
    }
}

// ============ 图片预览 ============
@Composable
private fun ImagePreview(imageUri: String, onRemove: () -> Unit) {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier.fillMaxWidth().background(colors.background).padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).border(0.5.dp, colors.border, RoundedCornerShape(12.dp))
        ) {
            AsyncImage(model = Uri.parse(imageUri), contentDescription = "待发送图片", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(
                modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(18.dp).clip(CircleShape).background(colors.background.copy(alpha = 0.9f)).clickable { onRemove() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, contentDescription = "移除", tint = colors.textSecondary, modifier = Modifier.size(12.dp))
            }
        }
    }
}

// ============ 消息气泡 ============
@Composable
private fun TextMessageBubble(
    msg: ChatMessage,
    animatedMessageIds: Set<Long>,
    viewModel: AssistantViewModel
) {
    val colors = LocalAppColors.current
    val alreadyAnimated = animatedMessageIds.contains(msg.id)

    val segments = remember(msg.id, msg.text) {
        if (msg.isUser || msg.isSystem) listOf(MessageSegment.TextSegment(msg.text))
        else parseMessageSegments(msg.text)
    }

    val fullTextLength = segments.filterIsInstance<MessageSegment.TextSegment>().sumOf { it.text.length }
    var visibleTextLength by remember(msg.id) {
        mutableIntStateOf(if (alreadyAnimated) fullTextLength else 0)
    }

    LaunchedEffect(msg.id) {
        if (!msg.isUser && !msg.isSystem && !alreadyAnimated) {
            visibleTextLength = 0
            val charDelay = when { fullTextLength > 500 -> 5L; fullTextLength > 200 -> 10L; else -> 15L }
            for (i in 1..fullTextLength) { delay(charDelay); visibleTextLength = i }
            viewModel.markMessageAnimated(msg.id)
        } else { visibleTextLength = fullTextLength }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start) {
        val maxWidth = if (segments.any { it is MessageSegment.CodeSegment }) 340.dp else 280.dp
        Box(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (msg.isUser) 16.dp else 4.dp, bottomEnd = if (msg.isUser) 4.dp else 16.dp))
                .background(if (msg.isUser) colors.messageUserBg else colors.messageAiBg)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // 深度思考内容（如果有）
                if (!msg.reasoningContent.isNullOrBlank() && !msg.isUser) {
                    ReasoningCard(reasoning = msg.reasoningContent!!)
                }

                if (msg.imageUri != null) {
                    AsyncImage(
                        model = Uri.parse(msg.imageUri), contentDescription = "用户图片", contentScale = ContentScale.Fit,
                        modifier = Modifier.sizeIn(maxWidth = 200.dp, maxHeight = 200.dp).clip(RoundedCornerShape(8.dp))
                    )
                }

                // 检测是否为错误消息
                val isErrorMsg = msg.text.contains("❌") || msg.text.contains("引擎异常") || msg.text.contains("引擎错误") || msg.text.contains("调用失败")
                val errorColor = Color(0xFFFF4444)

                SelectionContainer {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        var renderedTextLen = 0
                        segments.forEach { segment ->
                            when (segment) {
                                is MessageSegment.TextSegment -> {
                                    val remaining = (visibleTextLength - renderedTextLen).coerceIn(0, segment.text.length)
                                    if (remaining > 0) {
                                        val textColor = if (isErrorMsg) errorColor
                                            else if (msg.isUser) colors.messageUserText
                                            else colors.messageAiText
                                        Text(text = segment.text.take(remaining), fontSize = 15.sp, color = textColor, lineHeight = 22.sp)
                                    }
                                    renderedTextLen += segment.text.length
                                }
                                is MessageSegment.CodeSegment -> {
                                    if (visibleTextLength >= fullTextLength || msg.isUser || msg.isSystem) {
                                        CodeBlockCard(language = segment.language, code = segment.code, fileName = segment.fileName)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============ 系统消息 ============
@Composable
private fun SystemMessageBubble(text: String) {
    val colors = LocalAppColors.current
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 13.sp, color = colors.textTertiary)
    }
}

// ============ 编译进度气泡 ============
@Composable
private fun TaskProgressBubble(msg: ChatMessage) {
    val colors = LocalAppColors.current
    val progress = msg.taskProgress ?: return
    val context = androidx.compose.ui.platform.LocalContext.current

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Column(
            modifier = Modifier.widthIn(max = 300.dp).clip(RoundedCornerShape(16.dp)).background(colors.surface).padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val icon = when (progress.status) { "completed" -> "✅"; "failed" -> "❌"; else -> "🔄" }
                Text(icon, fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                val titleColor = when (progress.status) {
                    "completed" -> colors.success
                    "failed" -> Color(0xFFFF4444)
                    else -> colors.textPrimary
                }
                Text(
                    when (progress.status) { "completed" -> "编译完成"; "failed" -> "编译失败"; else -> "编译进度" },
                    fontSize = 15.sp, fontWeight = FontWeight.Medium, color = titleColor
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(msg.text, fontSize = 13.sp, color = colors.textSecondary, lineHeight = 20.sp)
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress.progress },
                color = when (progress.status) { "completed" -> colors.success; "failed" -> colors.error; else -> colors.accent },
                trackColor = colors.border,
                modifier = Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(1.dp))
            )
            Spacer(Modifier.height(8.dp))
            val msgColor = when (progress.status) {
                "failed" -> Color(0xFFFF4444)
                "completed" -> colors.success
                else -> colors.textTertiary
            }
            Text(progress.message, fontSize = 12.sp, color = msgColor)

            if (progress.status == "completed" && progress.downloadUrl != null) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(colors.accent).clickable {
                        try {
                            val apkUri = Uri.parse(progress.downloadUrl)
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                setDataAndType(apkUri, "application/vnd.android.package-archive")
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }.padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📱 安装 APK", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}
