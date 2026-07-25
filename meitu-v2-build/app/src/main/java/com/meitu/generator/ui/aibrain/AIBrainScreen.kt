package com.meitu.generator.ui.aibrain

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meitu.generator.ui.theme.*

@Composable
fun AIBrainScreen(
    onNavigate: (String) -> Unit = {},
    onTriggerGeneration: (Int) -> Unit = {},
    viewModel: AIBrainViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val quickCommands = viewModel.quickCommands
    val listState = rememberLazyListState()

    val nav by viewModel.navigateTo.collectAsState()
    LaunchedEffect(nav) {
        nav?.let { onNavigate(it); viewModel.clearNavigation() }
    }
    val trigger by viewModel.triggerGeneration.collectAsState()
    LaunchedEffect(trigger) {
        trigger?.let { onTriggerGeneration(it); viewModel.clearTrigger() }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(BgPrimary)) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text("\uD83E\uDDE0 AI\u5927\u8111", fontSize = 20.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
            Text("\u8BF4\u51FA\u4F60\u7684\u9700\u6C42\uFF0C\u6211\u6765\u6267\u884C", fontSize = 12.sp, color = TextTertiary)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(msg)
            }
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickCommands) { cmd ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(BgTertiary)
                        .clickable { viewModel.sendQuickCommand(cmd) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(cmd, fontSize = 12.sp, color = TextSecondary)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgSecondary)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { viewModel.setInput(it) },
                placeholder = { Text("\u8F93\u5165\u6307\u4EE4...", color = TextTertiary, fontSize = 14.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandPurple,
                    unfocusedBorderColor = Divider,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = BrandPurple
                ),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.horizontalGradient(listOf(BrandPurple, BrandPurpleLight)))
                    .clickable { viewModel.sendInput() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 12.dp, topEnd = 12.dp,
                        bottomStart = if (msg.isUser) 12.dp else 4.dp,
                        bottomEnd = if (msg.isUser) 4.dp else 12.dp
                    )
                )
                .background(if (msg.isUser) BrandPurple else BgTertiary)
                .padding(12.dp)
        ) {
            Text(
                text = msg.text, fontSize = 14.sp,
                color = if (msg.isUser) Color.White else TextSecondary,
                lineHeight = 20.sp
            )
        }
    }
}
