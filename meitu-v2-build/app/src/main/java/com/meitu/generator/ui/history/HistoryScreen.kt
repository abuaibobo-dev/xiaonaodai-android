package com.meitu.generator.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meitu.generator.data.local.entity.TaskEntity
import com.meitu.generator.ui.components.GlassCard
import com.meitu.generator.ui.theme.*
import com.meitu.generator.util.toDateString
import com.meitu.generator.util.toDurationString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val tasks by viewModel.tasks.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(BgPrimary)) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = TextPrimary)
            }
            Text("\u5386\u53F2\u4EFB\u52A1", fontSize = 20.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
        }

        if (tasks.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("\u6682\u65E0\u5386\u53F2\u4EFB\u52A1", fontSize = 14.sp, color = TextTertiary)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(tasks) { task ->
                    TaskCard(task)
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun TaskCard(task: TaskEntity) {
    val statusText = when (task.status) {
        0 -> "\u8FDB\u884C\u4E2D"
        1 -> "\u5DF2\u5B8C\u6210"
        2 -> "\u5DF2\u53D6\u6D88"
        else -> ""
    }
    val statusColor = when (task.status) {
        0 -> BrandCyan
        1 -> SuccessGreen
        2 -> WarningOrange
        else -> TextTertiary
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(task.presetName, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                Text(statusText, fontSize = 12.sp, color = statusColor)
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("\u76EE\u6807: ${task.targetCount}\u5F20", fontSize = 12.sp, color = TextTertiary)
                Text("\u6210\u529F: ${task.successCount}\u5F20", fontSize = 12.sp, color = SuccessGreen)
                Text("\u5931\u8D25: ${task.failedCount}\u5F20", fontSize = 12.sp, color = ErrorRed)
                Text(task.durationSeconds.toDurationString(), fontSize = 12.sp, color = TextTertiary)
            }
            Spacer(Modifier.height(4.dp))
            Text(task.startedAt.toDateString(), fontSize = 11.sp, color = TextTertiary)
        }
    }
}
