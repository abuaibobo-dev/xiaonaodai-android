package com.meitu.generator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.meitu.generator.ui.AppEvents
import com.meitu.generator.ui.assistant.AssistantViewModel
import com.meitu.generator.ui.navigation.NavGraph
import com.meitu.generator.ui.navigation.Routes
import com.meitu.generator.ui.theme.LocalAppColors
import com.meitu.generator.ui.theme.MeituTheme
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            MeituTheme(darkTheme = true) {
                MainScreen()
            }
        }
    }
}

data class DrawerItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

val drawerItems = listOf(
    DrawerItem(Routes.ASSISTANT, Icons.Outlined.Chat, "对话"),
)

@Composable
fun MainScreen() {
    val colors = LocalAppColors.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var drawerOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val assistantViewModel: AssistantViewModel = hiltViewModel()

    LaunchedEffect(Unit) {
        AppEvents.events.collect { event ->
            if (event == "navigate_settings") {
                navController.navigate(Routes.SETTINGS) {
                    launchSingleTop = true
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
        Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
            val conversationName by assistantViewModel.conversationName.collectAsState()
            TopAppBar(
                navController = navController,
                currentRoute = currentRoute,
                conversationName = conversationName,
                onMenuClick = { drawerOpen = true },
                onNewChat = {
                    scope.launch {
                        navController.navigate(Routes.ASSISTANT) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                        AppEvents.send("new_chat")
                    }
                },
                onClearChat = {
                    scope.launch { AppEvents.send("clear_chat") }
                },
            )

            Box(modifier = Modifier.weight(1f)) {
                NavGraph(navController = navController)
            }
        }

        // ============ 侧边栏遮罩 ============
        AnimatedVisibility(
            visible = drawerOpen,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f))
                    .clickable { drawerOpen = false }
            )
        }

        // ============ 侧边栏 ============
        AnimatedVisibility(
            visible = drawerOpen,
            enter = slideInHorizontally(initialOffsetX = { -it }),
            exit = slideOutHorizontally(targetOffsetX = { -it })
        ) {
            SidebarDrawer(
                currentRoute = currentRoute,
                viewModel = assistantViewModel,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                    drawerOpen = false
                },
                onDismiss = { drawerOpen = false }
            )
        }
    }
}

// ============ 极简顶栏 ============
@Composable
private fun TopAppBar(
    navController: androidx.navigation.NavHostController,
    currentRoute: String?,
    conversationName: String,
    onMenuClick: () -> Unit,
    onNewChat: () -> Unit,
    onClearChat: () -> Unit
) {
    val colors = LocalAppColors.current
    var showClearConfirm by remember { mutableStateOf(false) }
    Surface(
        color = colors.background,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth().statusBarsPadding()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：汉堡菜单按钮
                if (currentRoute != Routes.SETTINGS) {
                    IconButton(
                        onClick = onMenuClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .drawBehind {
                                    val strokeW = 1.8.dp.toPx()
                                    val lineLen = size.width
                                    drawLine(colors.textPrimary, Offset(0f, size.height * 0.2f), Offset(lineLen, size.height * 0.2f), strokeW, StrokeCap.Round)
                                    drawLine(colors.textPrimary, Offset(0f, size.height * 0.5f), Offset(lineLen, size.height * 0.5f), strokeW, StrokeCap.Round)
                                    drawLine(colors.textPrimary, Offset(0f, size.height * 0.8f), Offset(lineLen * 0.65f, size.height * 0.8f), strokeW, StrokeCap.Round)
                                }
                        )
                    }
                }

                // 中间：标题
                Text(
                    conversationName,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )

                // 右侧：新建对话按钮
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNewChat,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = "新建对话",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // 底部分割线
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(colors.border)
            )
        }
    }

    // ============ 清空对话确认弹窗 ============
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor = colors.surface,
            title = { Text("清空当前对话", color = colors.textPrimary, fontSize = 17.sp) },
            text = { Text("清空后无法恢复，确定继续？", color = colors.textSecondary, fontSize = 14.sp) },
            confirmButton = {
                TextButton(
                    onClick = { showClearConfirm = false; onClearChat() },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.error)
                ) { Text("清空") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearConfirm = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.textTertiary)
                ) { Text("取消") }
            }
        )
    }
}

// ============ 侧边栏抽屉 ============
@Composable
private fun SidebarDrawer(
    currentRoute: String?,
    viewModel: AssistantViewModel,
    onNavigate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current
    val sessionList by viewModel.sessionList.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadSessionList()
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(colors.background)
            .padding(top = 8.dp)
    ) {
        // 导航项
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            drawerItems.forEach { item ->
                val isSelected = currentRoute == item.route
                SidebarNavItem(
                    icon = item.icon,
                    label = item.label,
                    isSelected = isSelected,
                    onClick = { onNavigate(item.route) }
                )
                if (item.route != drawerItems.last().route) {
                    Spacer(Modifier.height(4.dp))
                }
            }

            // 设置
            Spacer(Modifier.height(4.dp))
            SidebarNavItem(
                icon = Icons.Outlined.Settings,
                label = "设置",
                isSelected = currentRoute == Routes.SETTINGS,
                onClick = { onNavigate(Routes.SETTINGS) }
            )
        }

        // 分隔线
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(0.5.dp)
                .background(colors.border)
        )

        // 历史对话标题
        Text(
            "历史对话",
            fontSize = 12.sp,
            color = colors.textTertiary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            letterSpacing = 1.sp
        )

        // 历史对话列表
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(sessionList, key = { it.sessionId }) { session ->
                val title = session.firstUserMessage?.take(30) ?: "对话"
                val dateText = remember(session.firstTimestamp) {
                    val sdf = java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.getDefault())
                    sdf.format(java.util.Date(session.firstTimestamp))
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surface.copy(alpha = 0.6f))
                        .clickable {
                            viewModel.switchToSession(session.sessionId)
                            onNavigate(Routes.ASSISTANT)
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Column {
                        Text(
                            title,
                            fontSize = 13.sp,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            dateText,
                            fontSize = 11.sp,
                            color = colors.textTertiary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }

        // ============ 底部用户信息区 ============
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.Bottom
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(colors.border)
            )
            Spacer(Modifier.height(12.dp))

            var showAvatarMenu by remember { mutableStateOf(false) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.surface)
                            .clickable { showAvatarMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("\uD83E\uDDE0", fontSize = 16.sp)
                    }

                    DropdownMenu(
                        expanded = showAvatarMenu,
                        onDismissRequest = { showAvatarMenu = false },
                        modifier = Modifier.background(colors.surface)
                    ) {
                        // Coze 设置入口
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🔑", fontSize = 14.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Coze API 配置", fontSize = 14.sp, color = colors.textPrimary)
                                }
                            },
                            onClick = {
                                showAvatarMenu = false
                                onNavigate(Routes.SETTINGS)
                            }
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .height(0.5.dp)
                                .background(colors.border)
                        )

                        // 关于
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("ℹ️", fontSize = 14.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("关于", fontSize = 14.sp, color = colors.textPrimary)
                                }
                            },
                            onClick = {
                                showAvatarMenu = false
                                onNavigate(Routes.SETTINGS)
                            }
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "布老师",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(colors.online)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "在线",
                            fontSize = 13.sp,
                            color = colors.textTertiary
                        )
                    }
                }
            }
        }
    }
}

// ============ 侧边栏导航项 ============
@Composable
private fun SidebarNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) colors.accentAlpha12 else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (isSelected) colors.accent else colors.textSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            color = if (isSelected) colors.accent else colors.textPrimary
        )
    }
}
