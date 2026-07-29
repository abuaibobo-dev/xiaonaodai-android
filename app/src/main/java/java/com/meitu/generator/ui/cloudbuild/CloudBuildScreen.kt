package com.meitu.generator.ui.cloudbuild

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meitu.generator.repository.BuildStep
import com.meitu.generator.repository.StepStatus
import com.meitu.generator.ui.theme.LocalAppColors
import com.meitu.generator.ui.components.Spacing
import com.meitu.generator.ui.components.CornerRadius
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudBuildScreen(
    viewModel: CloudBuildViewModel = hiltViewModel()
) {
    val colors = LocalAppColors.current
    val buildState by viewModel.buildState.collectAsState()
    val buildSteps by viewModel.buildSteps.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val ciJobs by viewModel.ciJobs.collectAsState()
    val buildLogs by viewModel.buildLogs.collectAsState()
    val githubToken by viewModel.githubToken.collectAsState()

    var tokenInput by remember { mutableStateOf(githubToken) }
    var showTokenDialog by remember { mutableStateOf(false) }

    val isBuilding = buildState is BuildState.Pushing ||
            buildState is BuildState.Triggering ||
            buildState is BuildState.Building ||
            buildState is BuildState.Downloading
    val isSuccessful = buildState is BuildState.Success
    val isFailed = buildState is BuildState.Failed

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Text(
            "云端编译",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            modifier = Modifier.padding(horizontal = Spacing.PagePadding, vertical = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = Spacing.PagePadding, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ============ GitHub Token 配置 ============
            item { SectionTitle("GitHub 配置") }

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
                        Text("Personal Access Token", fontSize = 14.sp, color = colors.textSecondary)
                        IconButton(onClick = { showTokenDialog = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑Token", tint = colors.accent, modifier = Modifier.size(18.dp))
                        }
                    }
                    if (githubToken.isNotBlank()) {
                        val maskedToken = "${githubToken.take(4)}****${githubToken.takeLast(4)}"
                        Text(maskedToken, fontSize = 13.sp, color = colors.success, fontFamily = FontFamily.Monospace)
                    } else {
                        Text("未配置 - 点击编辑按钮设置", fontSize = 13.sp, color = colors.error)
                    }
                }
            }

            item { Spacer(Modifier.height(12.dp)) }

            // ============ 编译步骤时间线 ============
            if (buildSteps.isNotEmpty()) {
                item { SectionTitle("执行进度") }

                item {
                    BuildStepTimeline(
                        steps = buildSteps,
                        downloadProgress = downloadProgress,
                        ciJobs = ciJobs,
                        buildState = buildState
                    )
                }

                item { Spacer(Modifier.height(12.dp)) }
            }

            // ============ 操作按钮 ============
            item {
                BuildActionBar(
                    buildState = buildState,
                    onStartBuild = {
                        viewModel.startBuild(mapOf(
                            "README.md" to "# 布老师 App - Auto Build\nBuilt from 布老师 App"
                        ))
                    },
                    onCheckStatus = { viewModel.checkStatus() },
                    onDownloadApk = { viewModel.downloadApk() },
                    onReset = { viewModel.reset() },
                    isBuilding = isBuilding,
                    isSuccessful = isSuccessful,
                    isFailed = isFailed,
                    isTokenConfigured = githubToken.isNotBlank()
                )
            }

            item { Spacer(Modifier.height(12.dp)) }

            // ============ 编译日志 ============
            item { SectionTitle("详细日志") }

            item {
                BuildLogCard(buildLogs = buildLogs)
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // Token 编辑对话框
    if (showTokenDialog) {
        var isTokenVisible by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showTokenDialog = false },
            containerColor = colors.surface,
            title = { Text("设置 GitHub Token", color = colors.textPrimary) },
            text = {
                Column {
                    Text("需要 Classic PAT（ghp_ 开头），勾选 repo + workflow 权限", fontSize = 12.sp, color = colors.textSecondary)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        label = { Text("ghp_xxxx") },
                        singleLine = true,
                        visualTransformation = if (isTokenVisible)
                            androidx.compose.ui.text.input.VisualTransformation.None
                        else
                            androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isTokenVisible = !isTokenVisible }) {
                                Icon(
                                    if (isTokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isTokenVisible) "隐藏" else "显示",
                                    tint = colors.textSecondary
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.border,
                            cursorColor = colors.accent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.saveToken(tokenInput.trim())
                        showTokenDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.accent)
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showTokenDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.textSecondary)
                ) { Text("取消") }
            }
        )
    }
}

// ============ 步骤时间线组件 ============

@Composable
private fun BuildStepTimeline(
    steps: List<BuildStep>,
    downloadProgress: com.meitu.generator.repository.DownloadProgress?,
    ciJobs: List<com.meitu.generator.data.remote.dto.WorkflowJob>,
    buildState: BuildState
) {
    val colors = LocalAppColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CornerRadius.Card))
            .background(colors.surface)
            .padding(16.dp)
    ) {
        steps.forEachIndexed { index, step ->
            BuildStepRow(step = step, colors = colors)

            // 在编译步骤下展开 CI jobs 详情
            if (step.id == "compile" && step.status != StepStatus.PENDING && ciJobs.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .padding(start = 36.dp, top = 4.dp, bottom = 8.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.background.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    Text("CI 编译步骤", fontSize = 11.sp, color = colors.textSecondary, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    ciJobs.forEach { job ->
                        job.steps.forEach { ciStep ->
                            val (stepIcon, stepColor) = when {
                                ciStep.conclusion == "success" -> "✅" to colors.success
                                ciStep.conclusion == "failure" -> "❌" to colors.error
                                ciStep.conclusion == "skipped" -> "⏭️" to colors.textSecondary
                                ciStep.status == "in_progress" -> "⚙️" to colors.warning
                                ciStep.status == "queued" -> "⏳" to colors.textSecondary
                                else -> "○" to colors.textSecondary
                            }
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stepIcon, fontSize = 12.sp)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    ciStep.name,
                                    fontSize = 11.sp,
                                    color = stepColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // 在下载步骤下展开进度条
            if (step.id == "download" && step.status == StepStatus.RUNNING && downloadProgress != null) {
                Column(modifier = Modifier.padding(start = 36.dp, top = 4.dp, bottom = 8.dp)) {
                    LinearProgressIndicator(
                        progress = { downloadProgress.percent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = colors.accent,
                        trackColor = colors.border,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        downloadProgress.displayBytes + " (${(downloadProgress.percent * 100).toInt()}%)",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
            }

            // 连接线
            if (index < steps.size - 1) {
                val lineColor = when (step.status) {
                    StepStatus.SUCCESS -> colors.success.copy(alpha = 0.4f)
                    StepStatus.FAILED -> colors.error.copy(alpha = 0.4f)
                    else -> colors.border
                }
                Box(
                    modifier = Modifier
                        .padding(start = 15.dp)
                        .width(2.dp)
                        .height(16.dp)
                        .background(lineColor)
                )
            }
        }
    }
}

@Composable
private fun BuildStepRow(step: BuildStep, colors: androidx.compose.material3.ColorScheme) {
    val (icon, iconColor, bgColor) = when (step.status) {
        StepStatus.SUCCESS -> Triple(Icons.Default.Check, Color.White, colors.success)
        StepStatus.FAILED -> Triple(Icons.Default.Close, Color.White, colors.error)
        StepStatus.RUNNING -> Triple(Icons.Default.MoreHoriz, Color.White, colors.accent)
        StepStatus.SKIPPED -> Triple(Icons.Default.SkipNext, colors.textSecondary, colors.border)
        StepStatus.PENDING -> Triple(Icons.Default.Circle, colors.textSecondary, Color.Transparent)
    }

    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 状态图标
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            if (step.status == StepStatus.RUNNING) {
                // 旋转动画
                val infiniteTransition = rememberInfiniteTransition(label = "spin")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f, targetValue = 360f,
                    animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
                    label = "rotation"
                )
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(rotation)
                )
            } else {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(Modifier.width(12.dp))

        // 步骤名称和详情
        Column(modifier = Modifier.weight(1f)) {
            Text(
                step.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = when (step.status) {
                    StepStatus.SUCCESS -> colors.success
                    StepStatus.FAILED -> colors.error
                    StepStatus.RUNNING -> colors.textPrimary
                    else -> colors.textSecondary
                }
            )
            if (step.detail.isNotEmpty()) {
                Text(step.detail, fontSize = 12.sp, color = colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ============ 操作按钮栏 ============

@Composable
private fun BuildActionBar(
    buildState: BuildState,
    onStartBuild: () -> Unit,
    onCheckStatus: () -> Unit,
    onDownloadApk: () -> Unit,
    onReset: () -> Unit,
    isBuilding: Boolean,
    isSuccessful: Boolean,
    isFailed: Boolean,
    isTokenConfigured: Boolean
) {
    val colors = LocalAppColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CornerRadius.Card))
            .background(colors.surface)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when {
                isSuccessful -> {
                    Button(
                        onClick = onDownloadApk,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(CornerRadius.Button),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.success)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("下载 APK", fontSize = 14.sp)
                    }
                    OutlinedButton(
                        onClick = onReset,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(CornerRadius.Button),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary)
                    ) { Text("重置", fontSize = 14.sp) }
                }
                isBuilding -> {
                    OutlinedButton(
                        onClick = onCheckStatus,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(CornerRadius.Button),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accent)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("刷新状态", fontSize = 14.sp)
                    }
                }
                isFailed -> {
                    Button(
                        onClick = onStartBuild,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(CornerRadius.Button),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("重新编译", fontSize = 14.sp)
                    }
                    OutlinedButton(
                        onClick = onReset,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(CornerRadius.Button),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary)
                    ) { Text("重置", fontSize = 14.sp) }
                }
                else -> {
                    Button(
                        onClick = onStartBuild,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(CornerRadius.Button),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTokenConfigured) colors.accent else colors.border
                        ),
                        enabled = isTokenConfigured
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (isTokenConfigured) "开始编译" else "请先配置 Token", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    val colors = LocalAppColors.current
    Text(
        text = title,
        fontSize = 12.sp,
        color = colors.textSecondary,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
    )
}

@Composable
private fun BuildLogCard(buildLogs: List<BuildLog>) {
    val colors = LocalAppColors.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(buildLogs.size) {
        if (buildLogs.isNotEmpty()) {
            coroutineScope.launch { listState.animateScrollToItem(buildLogs.size - 1) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CornerRadius.Card))
            .background(colors.surface)
    ) {
        if (buildLogs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无日志", fontSize = 14.sp, color = colors.textSecondary)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(buildLogs) { log ->
                    val color = when (log.level) {
                        "error" -> colors.error
                        "warning" -> colors.warning
                        else -> colors.textSecondary
                    }
                    val timeFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                    val timeStr = timeFormat.format(java.util.Date(log.timestamp))

                    Row {
                        Text("[$timeStr] ", fontSize = 11.sp, color = colors.textSecondary, fontFamily = FontFamily.Monospace)
                        Text(log.message, fontSize = 11.sp, color = color, fontFamily = FontFamily.Monospace, maxLines = 5, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
