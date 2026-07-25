package com.meitu.generator.ui.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.meitu.generator.ui.theme.LocalAppColors
import com.meitu.generator.ui.components.Spacing
import com.meitu.generator.ui.components.CornerRadius
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProjectScreen(
    viewModel: ProjectViewModel = hiltViewModel()
) {
    val colors = LocalAppColors.current
    val tasks by viewModel.tasks.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().background(colors.background)
    ) {
        // 页面标题
        Text(
            "项目",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            modifier = Modifier.padding(horizontal = Spacing.PagePadding, vertical = 16.dp)
        )

        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("暂无项目", fontSize = 16.sp, color = colors.textSecondary)
                    Spacer(Modifier.height(4.dp))
                    Text("在对话中让AI帮你创建项目", fontSize = 14.sp, color = colors.textSecondary)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = Spacing.PagePadding, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.ElementSpacing)
            ) {
                items(tasks, key = { it.id }) { task ->
                    ProjectCard(
                        task = task,
                        getStatusText = { viewModel.getStatusText(it) },
                        getStatusColor = { viewModel.getStatusColor(it) }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun ProjectCard(
    task: TaskEntity,
    getStatusText: (Int) -> String,
    getStatusColor: (Int) -> androidx.compose.ui.graphics.Color
) {
    val colors = LocalAppColors.current
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

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
            Text(
                task.presetName.ifEmpty { "项目 #${task.id}" },
                fontSize = 16.sp,
                color = colors.textPrimary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                getStatusText(task.status),
                fontSize = 12.sp,
                color = getStatusColor(task.status),
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                dateFormat.format(Date(task.startedAt)),
                fontSize = 12.sp,
                color = colors.textSecondary
            )
            if (task.targetCount > 0) {
                Text(
                    "目标: ${task.targetCount}",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
        }
    }
}
