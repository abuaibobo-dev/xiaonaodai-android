package com.meitu.generator.ui.assistant

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import kotlinx.coroutines.launch
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
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
        return "${packageMatch.groupValues[1].substringAfterLast(".")}. $lang"
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

// ============ 主屏幕 ============
@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel = hiltViewModel()
) {
    val colors = LocalAppColors.current
    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val pendingImageUri by viewModel.pendingImageUri.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isCozeConfigured by viewModel.isCozeConfigured.collectAsState()
    val currentChannel by viewModel.currentChannel.collectAsState()
    val listState = rememberLazyListState()

    // 判断当前通道是否可用
    val isChannelReady by remember(isCozeConfigured, currentChannel) {
        derivedStateOf {
            // Coze 通道用 isCozeConfigured；DeepSeek 通道需要单独判断（在 viewModel 中已处理）
            if (currentChannel == "deepseek") true // DeepSeek 的 key 检查在 sendMessage 中做
            else isCozeConfigured
        }
    }

    LaunchedEffect(Unit) {
        AppEvents.events.collect { event ->
            when (event) {
                "clear_chat" -> viewModel.clearCurrentChat()
                "new_chat" -> viewModel.newConversation()
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { viewModel.setPendingImageUri(it.toString()) } }

    val lastMessageId = messages.lastOrNull()?.id
    val lastMessageText = messages.lastOrNull()?.text
    val lastMessageLen = lastMessageText?.length ?: 0

    // 综合滚动触发器
    val scrollTrigger by remember {
        derivedStateOf {
            Triple(lastMessageId, isLoading, lastMessageLen)
        }
    }

    LaunchedEffect(scrollTrigger) {
        if (messages.isNotEmpty()) {
            delay(80)
            val totalItems = listState.layoutInfo.totalItemsCount
            if (totalItems > 0) {
                try { listState.animateScrollToItem(totalItems - 1, scrollOffset = 0) } catch (_: Exception) {}
            }
        }
    }

    // 判断是否为空对话
    val isEmptyConversation = (messages.isEmpty() || (messages.size <= 1 && messages.all { !it.isUser })) && !isLoading

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        if (isEmptyConversation) {
            // ============ 空对话：居中布局 ============
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                EmptyState()
            }
        } else {
            // ============ 有消息：正常列表布局 ============
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        TextMessageBubble(msg)
                    }

                    if (isLoading) {
                        item { LoadingIndicator(statusMessage) }
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
                    val coroutineScope = rememberCoroutineScope()
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.surface.copy(alpha = 0.85f))
                            .border(0.5.dp, colors.border, CircleShape)
                            .clickable {
                                coroutineScope.launch {
                                    try {
                                        val lastIndex = listState.layoutInfo.totalItemsCount - 1
                                        listState.animateScrollToItem(lastIndex)
                                    } catch (_: Exception) {}
                                }
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
        }

        // ============ 图片预览区 ============
        if (pendingImageUri != null) {
            ImagePreview(imageUri = pendingImageUri!!, onRemove = { viewModel.setPendingImageUri(null) })
        }

        // ============ 输入区域 ============
        Box(modifier = Modifier.imePadding()) {
            Column {
                ChatInputBar(
                    inputText = inputText,
                    isLoading = isLoading,
                    pendingImageUri = pendingImageUri,
                    isChannelReady = isChannelReady,
                    currentChannel = currentChannel,
                    onInputChange = { viewModel.setInputText(it) },
                    onSend = { viewModel.sendMessage() },
                    onPickImage = { imagePickerLauncher.launch("image/*") }
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("🧠", fontSize = 48.sp)

            Spacer(Modifier.height(8.dp))

            Text("布老师", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            Text("你的专属 AI 助手", fontSize = 14.sp, color = colors.textTertiary)

            Spacer(Modifier.height(24.dp))

            Text("有什么想法直接说，我来帮你 💡", fontSize = 12.sp, color = colors.textTertiary)
        }
    }
}

// ============ 加载指示器 ============
@Composable
private fun LoadingIndicator(statusMessage: String?) {
    val colors = LocalAppColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(800, easing = EaseInOutSine), repeatMode = RepeatMode.Reverse),
        label = "pulse"
    )

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Box(
            modifier = Modifier
                .widthIn(max = 240.dp)
                .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                .background(colors.messageAiBg)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(6.dp).clip(CircleShape).alpha(pulseAlpha).background(colors.accent)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    statusMessage ?: "思考中...",
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
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

// ============ 聊天输入栏 ============
@Composable
private fun ChatInputBar(
    inputText: String,
    isLoading: Boolean,
    pendingImageUri: String?,
    isChannelReady: Boolean,
    currentChannel: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onPickImage: () -> Unit
) {
    val colors = LocalAppColors.current
    val canSend = (inputText.isNotBlank() || pendingImageUri != null) && !isLoading && isChannelReady

    Box(modifier = Modifier.fillMaxWidth()) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(colors.border))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.background)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.surface)
                        .border(0.5.dp, colors.border, RoundedCornerShape(20.dp))
                ) {
                    Column {
                        // 文本输入区域
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp, max = 140.dp)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            androidx.compose.foundation.text.BasicTextField(
                                value = inputText,
                                onValueChange = onInputChange,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 15.sp,
                                    color = colors.textPrimary,
                                    lineHeight = 22.sp
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.accent),
                                decorationBox = { innerTextField ->
                                    Box {
                                        if (inputText.isEmpty() && pendingImageUri == null) {
                                            androidx.compose.foundation.text.BasicText(
                                                text = when {
                                                    !isChannelReady -> when (currentChannel) {
                                                        "deepseek" -> "请先在设置中配置 DeepSeek API Key"
                                                        else -> "请先在设置中配置 PAT 和 Bot ID"
                                                    }
                                                    else -> "说点什么..."
                                                },
                                                style = androidx.compose.ui.text.TextStyle(
                                                    fontSize = 15.sp,
                                                    color = colors.textTertiary,
                                                    lineHeight = 22.sp
                                                )
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .height(0.5.dp)
                                .background(colors.border.copy(alpha = 0.4f))
                        )

                        // 底部工具栏
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 左侧：附件按钮 + 通道指示器
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(if (pendingImageUri != null) colors.accent.copy(alpha = 0.12f) else Color.Transparent)
                                        .clickable { onPickImage() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("📎", fontSize = 14.sp)
                                }

                                val channelLabel = if (currentChannel == "deepseek") "DeepSeek" else "🧠 Coze"
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colors.surface.copy(alpha = 0.6f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(channelLabel, fontSize = 10.sp, color = colors.textTertiary)
                                }
                            }

                            // 右侧：发送按钮
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (canSend) colors.accent else colors.border.copy(alpha = 0.3f))
                                    .clickable(enabled = canSend) { onSend() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Send,
                                    contentDescription = "发送",
                                    tint = if (canSend) Color.White else colors.textTertiary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============ 消息气泡 ============
@Composable
private fun TextMessageBubble(msg: ChatMessage) {
    val colors = LocalAppColors.current

    val segments = remember(msg.id, msg.text) {
        if (msg.isUser) listOf(MessageSegment.TextSegment(msg.text))
        else parseMessageSegments(msg.text)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (msg.isUser) Alignment.End else Alignment.Start
        ) {
            val maxWidth = if (segments.any { it is MessageSegment.CodeSegment }) 340.dp else 280.dp
            Box(
                modifier = Modifier
                    .widthIn(max = maxWidth)
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (msg.isUser) 16.dp else 4.dp,
                            bottomEnd = if (msg.isUser) 4.dp else 16.dp
                        )
                    )
                    .background(if (msg.isUser) colors.messageUserBg else colors.messageAiBg)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (msg.imageUri != null) {
                        AsyncImage(
                            model = Uri.parse(msg.imageUri),
                            contentDescription = "用户图片",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.sizeIn(maxWidth = 200.dp, maxHeight = 200.dp).clip(RoundedCornerShape(8.dp))
                        )
                    }

                    SelectionContainer {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            segments.forEach { segment ->
                                when (segment) {
                                    is MessageSegment.TextSegment -> {
                                        val textColor = if (msg.isUser) colors.messageUserText else colors.messageAiText
                                        Text(
                                            text = segment.text,
                                            fontSize = 15.sp,
                                            color = textColor,
                                            lineHeight = 22.sp
                                        )
                                    }
                                    is MessageSegment.CodeSegment -> {
                                        CodeBlockCard(
                                            language = segment.language,
                                            code = segment.code,
                                            fileName = segment.fileName
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // AI 消息的复制按钮
            if (!msg.isUser) {
                val context = LocalContext.current
                var copied by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("message", msg.text)
                                clipboard.setPrimaryClip(clip)
                                copied = true
                                android.widget.Toast.makeText(context, "已复制", android.widget.Toast.LENGTH_SHORT).show()
                            }
                            .padding(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "复制",
                                tint = if (copied) colors.accent else colors.textTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (copied) "已复制" else "复制",
                                fontSize = 11.sp,
                                color = if (copied) colors.accent else colors.textTertiary
                            )
                        }
                    }
                }
            }
        }
    }
}
