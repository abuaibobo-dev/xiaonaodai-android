package com.meitu.generator.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meitu.generator.ui.theme.LocalAppColors

// ============ 统一间距规范 ============
object Spacing {
    val PagePadding = 24.dp     // 页面边距
    val CardSpacing = 16.dp     // 卡片间距
    val ElementSpacing = 12.dp  // 元素间距
}

// ============ 统一圆角规范 ============
object CornerRadius {
    val Card = 16.dp    // 卡片
    val Button = 12.dp  // 按钮
    val Input = 10.dp   // 输入框
}

// ============ 卡片组件 ============
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = LocalAppColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(CornerRadius.Card))
            .background(colors.surface)
            .padding(Spacing.CardSpacing)
    ) {
        content()
    }
}

// ============ 数据卡片 ============
@Composable
fun DataCard(
    label: String,
    value: String,
    highlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(CornerRadius.Card))
            .background(colors.surface)
            .padding(Spacing.CardSpacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = if (highlight) colors.accent else colors.textPrimary
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = colors.textSecondary
        )
    }
}

// ============ 主按钮 (纯色, 无渐变) ============
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val bgColor = if (enabled) colors.accent else colors.surface
    val textColor = if (enabled) Color.White else colors.textSecondary

    Box(
        modifier = modifier
            .height(44.dp)
            .widthIn(min = 180.dp)
            .clip(RoundedCornerShape(CornerRadius.Button))
            .background(bgColor)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

// ============ 次要按钮 ============
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(CornerRadius.Button))
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 14.sp, color = colors.textSecondary)
    }
}

// ============ 文字按钮 ============
@Composable
fun TextButton(
    text: String,
    onClick: () -> Unit,
    color: Color = Color.Unspecified,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val tintColor = if (color != Color.Unspecified) color else colors.accent
    Text(
        text = text,
        fontSize = 14.sp,
        color = tintColor,
        modifier = modifier.clickable(onClick = onClick)
    )
}

// ============ 标签芯片 ============
@Composable
fun TagChip(
    text: String,
    selected: Boolean,
    suggested: Boolean = false,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val bgColor = when {
        selected -> colors.accent
        else -> colors.surface
    }
    val textColor = when {
        selected -> Color.White
        else -> colors.textSecondary
    }
    val borderColor = when {
        suggested && !selected -> colors.accent
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(CornerRadius.Button))
            .background(bgColor)
            .then(
                if (borderColor != Color.Transparent)
                    Modifier.border(1.dp, borderColor, RoundedCornerShape(CornerRadius.Button))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 12.sp, color = textColor)
    }
}

// ============ 区块标题 ============
@Composable
fun SectionTitle(text: String) {
    val colors = LocalAppColors.current
    Text(
        text = text,
        fontSize = 12.sp,
        color = colors.textSecondary,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

// ============ 输入框 ============
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else 5,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = colors.textSecondary, fontSize = 14.sp) },
        singleLine = singleLine,
        maxLines = maxLines,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.accent,
            unfocusedBorderColor = colors.border,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            cursorColor = colors.accent
        ),
        shape = RoundedCornerShape(CornerRadius.Input),
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(CornerRadius.Input))
    )
}

// ============ 日志条目 ============
@Composable
fun LogEntry(level: String, message: String, timestamp: String) {
    val colors = LocalAppColors.current
    val color = when (level) {
        "info" -> colors.textSecondary
        "success" -> colors.success
        "error" -> colors.error
        "warning" -> colors.warning
        "upload" -> colors.accent
        else -> colors.textSecondary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = timestamp,
            fontSize = 11.sp,
            color = colors.textSecondary,
            modifier = Modifier.width(45.dp)
        )
        Text(text = " | ", fontSize = 11.sp, color = colors.border)
        Text(
            text = message,
            fontSize = 12.sp,
            color = color,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

// ============ 进度条 (纯色, 无渐变) ============
@Composable
fun ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(colors.border)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(colors.accent)
        )
    }
}

// ============ 迷你柱状图 (纯色, 无渐变) ============
@Composable
fun MiniLineChart(
    data: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val maxVal = data.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    Row(
        modifier = modifier.fillMaxWidth().height(50.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        data.forEach { (label, value) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                val h = (value.toFloat() / maxVal * 30f).coerceAtLeast(2f)
                Box(
                    modifier = Modifier
                        .width(12.dp)
                        .height(h.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(colors.accent)
                )
                Spacer(Modifier.height(2.dp))
                Text(text = label, fontSize = 8.sp, color = colors.textSecondary)
            }
        }
    }
}
