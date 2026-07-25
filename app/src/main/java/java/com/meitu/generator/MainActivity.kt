package com.meitu.generator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
import com.meitu.generator.util.Constants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MeituTheme(darkTheme = true) {
                MainScreen()
            }
        }
    }
}

// ============ 侧边栏导航项 ============
data class DrawerItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

val drawerItems = listOf(
    DrawerItem(Routes.ASSISTANT, Icons.Outlined.Chat, "对话"),
    DrawerItem(Routes.PROJECTS, Icons.Outlined.Folder, "项目"),
    DrawerItem(Routes.CLOUD_BUILD, Icons.Outlined.Build, "编译"),
    DrawerItem(Routes.SETTINGS, Icons.Outlined.Settings, "设置")
)

@Composable
fun MainScreen() {
    val colors = LocalAppColors.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var drawerOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    // 用于顶栏余额显示（与 AssistantScreen 共享 ViewModel 实例）
    val assistantViewModel: AssistantViewModel = hiltViewModel()

    Box(modifier = Modifier.fillMaxSize()) {
        // ============ 主内容区域 ============
        Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
            // ============ 极简顶栏 ============
            TopAppBar(
                navController = navController,
                currentRoute = currentRoute,
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
                        AppEvents.send("clear_chat")
                    }
                },
                onClearChat = {
                    scope.launch { AppEvents.send("clear_chat") }
                }
            )

            // ============ 页面内容 ============
            Box(modifier = Modifier.weight(1f)) {
                NavGraph(navController = navController)
            }
        }

        // ============ 侧边栏遮罩 ============
        AnimatedVisibility(
            visible = drawerOpen,
            enter = slideInHorizontally(),
            exit = slideOutHorizontally()
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
    onMenuClick: () -> Unit,
    onNewChat: () -> Unit,
    onClearChat: () -> Unit
) {
    val colors = LocalAppColors.current
    var menuExpanded by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    Surface(
        color = colors.background,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：汉堡菜单按钮（设置页隐藏）
                if (currentRoute != Routes.SETTINGS) {
                    IconButton(
                        onClick = onMenuClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "菜单",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // 标题
                Text(
                    "布老师",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )

                // 右侧：状态指示点 + 更多菜单（仅对话页显示）
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(colors.online)
                )
                Spacer(Modifier.width(12.dp))

                if (currentRoute == Routes.ASSISTANT || currentRoute == null) {
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "更多",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(colors.surface)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Outlined.Add,
                                            contentDescription = null,
                                            tint = colors.textPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text("新建对话", fontSize = 15.sp, color = colors.textPrimary)
                                    }
                                },
                                onClick = {
                                    menuExpanded = false
                                    onNewChat()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Outlined.DeleteSweep,
                                            contentDescription = null,
                                            tint = colors.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text("清空对话", fontSize = 15.sp, color = colors.error)
                                    }
                                },
                                onClick = {
                                    menuExpanded = false
                                    showClearConfirm = true
                                }
                            )
                        }
                    }
                }
            }

            // 底部分割线（极细）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(colors.border)
            )
        }
    }

    // 清空对话二次确认弹窗
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor = colors.surface,
            title = {
                Text(
                    "清空当前对话",
                    color = colors.textPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium
                )
            },
            text = {
                Text(
                    "确定要清空当前对话吗？此操作不可撤销。",
                    fontSize = 15.sp,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirm = false
                        onClearChat()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.error)
                ) { Text("确认清空") }
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

// ============ 侧边栏 ============
@Composable
private fun SidebarDrawer(
    currentRoute: String?,
    viewModel: AssistantViewModel,
    onNavigate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current
    val balance by viewModel.balance.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshBalance()
    }

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // ============ 顶部: App名称 ============
            Text(
                "布老师",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Text(
                "v${Constants.APP_VERSION}",
                fontSize = 12.sp,
                color = colors.textTertiary,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(Modifier.height(20.dp))

            // ============ 功能入口 ============
            drawerItems.forEach { item ->
                val isSelected = item.route == currentRoute
                SidebarNavItem(
                    icon = item.icon,
                    label = item.label,
                    isSelected = isSelected,
                    onClick = { onNavigate(item.route) }
                )
            }

            Spacer(Modifier.height(24.dp))

            // ============ 分割线 ============
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(colors.border)
            )

            Spacer(Modifier.height(16.dp))

            // ============ 历史对话区 ============
            Text(
                "历史对话",
                fontSize = 13.sp,
                color = colors.textTertiary,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(12.dp))

            // 空状态
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("\uD83D\uDCAC", fontSize = 24.sp)
                    Text(
                        "暂无历史对话",
                        fontSize = 13.sp,
                        color = colors.textTertiary
                    )
                }
            }

            // ============ 底部用户信息区 ============
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.Bottom
            ) {
                // 余额显示区
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text("💰", fontSize = 14.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "账户余额",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "¥${balance.totalBalance}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.accent
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(colors.border)
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 用户头像
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("\uD83E\uDDE0", fontSize = 16.sp)
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
