package com.meitu.generator.ui.cloudbuild

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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
    val buildLogs by viewModel.buildLogs.collectAsState()
    val githubToken by viewModel.githubToken.collectAsState()

    var tokenInput by remember { mutableStateOf(githubToken) }
    var showTokenDialog by remember { mutableStateOf(false) }
    var isTokenVisible by remember { mutableStateOf(false) }

    val isBuilding = buildState is BuildState.Pushing ||
            buildState is BuildState.Triggering ||
            buildState is BuildState.Building
    val isSuccessful = buildState is BuildState.Success
    val isFailed = buildState is BuildState.Failed

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // 页面标题
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
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "编辑Token",
                                tint = colors.accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    if (githubToken.isNotBlank()) {
                        val maskedToken = "${githubToken.take(4)}****${githubToken.takeLast(4)}"
                        Text(maskedToken, fontSize = 13.sp, color = colors.success, fontFamily = FontFamily.Monospace)
                    } else {
                        Text("未配置 - 点击编辑按钮设置", fontSize = 13.sp, color = colors.error)
                    }
                }
            }

            item { Spacer(Modifier.height(12.dp)) }

            // ============ 编译状态 ============
            item { SectionTitle("编译状态") }

            item {
                BuildStatusCard(
                    buildState = buildState,
                    onStartBuild = {
                        viewModel.startBuild(mapOf(
                            "README.md" to "# 星仔 App - Auto Build\nBuilt from 星仔 App"
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
            item { SectionTitle("编译日志") }

            item {
                BuildLogCard(buildLogs = buildLogs)
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // ============ Token 编辑对话框 ============
    if (showTokenDialog) {
        AlertDialog(
            onDismissRequest = { showTokenDialog = false },
            containerColor = colors.surface,
            title = { Text("设置 GitHub Token", color = colors.textPrimary) },
            text = {
                Column {
                    Text(
                        "需要 Classic PAT（ghp_ 开头），勾选 repo + workflow 权限",
                        fontSize = 12.sp, color = colors.textSecondary
                    )
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
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showTokenDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.textSecondary)
                ) {
                    Text("取消")
                }
            }
        )
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
private fun BuildStatusCard(
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
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val (icon, statusText, statusColor) = when (buildState) {
            is BuildState.Idle -> Triple("⏸️", "待命", colors.textSecondary)
            is BuildState.Pushing -> Triple("📤", "推送代码中...", colors.accent)
            is BuildState.Triggering -> Triple("🚀", "触发编译中...", colors.accent)
            is BuildState.Building -> Triple("🔨", "编译中 [${buildState.status}]", colors.warning)
            is BuildState.Success -> Triple("✅", "编译成功", colors.success)
            is BuildState.Failed -> Triple("❌", "编译失败: ${buildState.error.take(30)}", colors.error)
        }

        Text(icon, fontSize = 40.sp)
        Spacer(Modifier.height(8.dp))
        Text(statusText, fontSize = 16.sp, color = statusColor, fontWeight = FontWeight.Medium)

        if (buildState is BuildState.Building) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                color = colors.accent,
                trackColor = colors.border,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .clip(RoundedCornerShape(4.dp))
            )
        }

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when {
                isSuccessful -> {
                    OutlinedButton(
                        onClick = onDownloadApk,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(CornerRadius.Button),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.success)
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
                    ) {
                        Text("重置", fontSize = 14.sp)
                    }
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
                    ) {
                        Text("重置", fontSize = 14.sp)
                    }
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
                        Text(
                            if (isTokenConfigured) "开始编译" else "请先配置 Token",
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BuildLogCard(buildLogs: List<BuildLog>) {
    val colors = LocalAppColors.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(buildLogs.size) {
        if (buildLogs.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(buildLogs.size - 1)
            }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无日志", fontSize = 14.sp, color = colors.textSecondary)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .padding(12.dp),
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
                        Text(
                            "[$timeStr] ",
                            fontSize = 11.sp,
                            color = colors.textSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            log.message,
                            fontSize = 11.sp,
                            color = color,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
