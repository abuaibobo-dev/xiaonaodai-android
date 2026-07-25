package com.meitu.generator.ui.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meitu.generator.ui.components.*
import com.meitu.generator.ui.theme.*
import com.meitu.generator.util.toShortDateString
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToAssistant: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val stats by viewModel.stats.collectAsState()
    val activePreset by viewModel.activePreset.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val taskProgress by viewModel.taskProgress.collectAsState()
    val completionMsg by viewModel.completionMessage.collectAsState()
    val dailyCounts by viewModel.dailyCounts.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Brush.horizontalGradient(listOf(BrandPurple, BrandCyan))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("M", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("美图生成器", fontSize = 16.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                }
                IconButton(onClick = onNavigateToHistory) {
                    Icon(Icons.Default.History, contentDescription = "历史", tint = BrandCyan, modifier = Modifier.size(20.dp))
                }
            }
        }

        // Data overview - 2 rows x 5 columns
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DataCard("累计生成", "${stats.totalCount}", modifier = Modifier.weight(1f))
                    DataCard("今日生成", "${stats.todayCount}", highlight = true, modifier = Modifier.weight(1f))
                    DataCard("本月生成", "${stats.monthCount}", modifier = Modifier.weight(1f))
                    DataCard("成功率", "${stats.successRate}%", modifier = Modifier.weight(1f))
                    DataCard("今日失败", "${stats.todayFailed}", modifier = Modifier.weight(1f))
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DataCard("平均耗时", stats.avgTime, modifier = Modifier.weight(1f))
                    DataCard("云端备份", "${stats.cloudCount}", modifier = Modifier.weight(1f))
                    DataCard("收藏总数", "${stats.favoriteCount}", modifier = Modifier.weight(1f))
                    DataCard("任务进度", if (stats.taskProgress.isNotEmpty()) stats.taskProgress else "-", modifier = Modifier.weight(1f))
                    DataCard("上次完成", stats.lastTaskTime, modifier = Modifier.weight(1f))
                }
            }
        }

        // Mini trend chart
        if (dailyCounts.isNotEmpty()) {
            item {
                GlassCard {
                    Column {
                        Text("近7天产出趋势", fontSize = 12.sp, color = TextTertiary)
                        Spacer(Modifier.height(8.dp))
                        MiniLineChart(data = dailyCounts.map { it.date to it.count })
                    }
                }
            }
        }

        // Active preset summary
        item {
            if (activePreset != null) {
                val p = activePreset!!
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(p.name, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                            Text("切换 >" , fontSize = 12.sp, color = BrandPurple,
                                modifier = Modifier.clickable { onNavigateToAssistant() })
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = p.prompt,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${p.ratio}", fontSize = 11.sp, color = TextTertiary)
                            Text(p.model, fontSize = 11.sp, color = TextTertiary)
                            Text(p.quality, fontSize = 11.sp, color = TextTertiary)
                        }
                    }
                }
            } else {
                GlassCard(modifier = Modifier.fillMaxWidth().clickable { onNavigateToAssistant() }) {
                    Text("暂无预设，请前往AI助手配置", fontSize = 14.sp, color = TextTertiary)
                }
            }
        }

        // Generation button + progress
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isRunning) {
                    val progress = if (taskProgress.second > 0) taskProgress.first.toFloat() / taskProgress.second else 0f
                    ProgressBar(progress = progress, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${taskProgress.first}/${taskProgress.second}",
                        fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(8.dp))
                    SecondaryButton(text = "停止生成", onClick = { viewModel.stopGeneration() })
                } else {
                    GradientButton(
                        text = "全自动生成",
                        onClick = { viewModel.startGeneration() },
                        enabled = activePreset != null
                    )
                }
            }
        }

        // Logs
        item {
            Text("运行日志", fontSize = 12.sp, color = TextTertiary, modifier = Modifier.padding(vertical = 4.dp))
        }
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BgPrimary)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    reverseLayout = true
                ) {
                    items(logs.take(50)) { log ->
                        LogEntry(
                            level = log.level,
                            message = log.message,
                            timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(log.timestamp))
                        )
                    }
                }
            }
        }

        // Completion message
        if (completionMsg != null) {
            item {
                AnimatedVisibility(visible = true) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SuccessGreen.copy(alpha = 0.15f))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "\uD83C\uDF89 ${completionMsg}",
                            fontSize = 14.sp,
                            color = SuccessGreen
                        )
                    }
                }
            }
        }

        // Bottom spacer
        item { Spacer(Modifier.height(80.dp)) }
    }
}
