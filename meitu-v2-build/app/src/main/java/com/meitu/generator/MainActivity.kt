package com.meitu.generator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.meitu.generator.ui.navigation.NavGraph
import com.meitu.generator.ui.navigation.Routes
import com.meitu.generator.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint

data class BottomNavItem(val route: String, val icon: ImageVector, val label: String)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MeituTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val navItems = listOf(
        BottomNavItem(Routes.HOME, Icons.Default.Home, "\u9996\u9875"),
        BottomNavItem(Routes.ASSISTANT, Icons.Default.AutoAwesome, "AI\u52A9\u624B"),
        BottomNavItem(Routes.AI_BRAIN, Icons.Default.Psychology, "AI\u5927\u8111"),
        BottomNavItem(Routes.GALLERY, Icons.Default.PhotoLibrary, "\u56FE\u5E93"),
        BottomNavItem(Routes.SETTINGS, Icons.Default.Settings, "\u8BBE\u7F6E")
    )

    // Show bottom bar only for main tabs
    val showBottomBar = currentRoute in navItems.map { it.route }

    Scaffold(
        containerColor = BgPrimary,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = BgSecondary,
                    contentColor = TextPrimary,
                    tonalElevation = 0.dp
                ) {
                    navItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label, modifier = Modifier.size(20.dp)) },
                            label = { Text(item.label, fontSize = 11.sp) },
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BrandPurple,
                                selectedTextColor = BrandPurple,
                                unselectedIconColor = TextTertiary,
                                unselectedTextColor = TextTertiary,
                                indicatorColor = BgPrimary
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            NavGraph(
                navController = navController,
                onTriggerGeneration = { /* handled by HomeViewModel */ },
                onNavigateToTab = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
