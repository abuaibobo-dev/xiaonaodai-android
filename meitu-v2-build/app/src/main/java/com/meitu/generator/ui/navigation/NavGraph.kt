package com.meitu.generator.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.meitu.generator.ui.aibrain.AIBrainScreen
import com.meitu.generator.ui.assistant.AssistantScreen
import com.meitu.generator.ui.gallery.GalleryScreen
import com.meitu.generator.ui.history.HistoryScreen
import com.meitu.generator.ui.home.HomeScreen
import com.meitu.generator.ui.settings.SettingsScreen

object Routes {
    const val HOME = "home"
    const val ASSISTANT = "assistant"
    const val AI_BRAIN = "aibrain"
    const val GALLERY = "gallery"
    const val SETTINGS = "settings"
    const val HISTORY = "history"
}

@Composable
fun NavGraph(
    navController: NavHostController,
    onTriggerGeneration: (Int) -> Unit = {},
    onNavigateToTab: (String) -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToHistory = { navController.navigate(Routes.HISTORY) },
                onNavigateToAssistant = { onNavigateToTab(Routes.ASSISTANT) }
            )
        }
        composable(Routes.ASSISTANT) {
            AssistantScreen()
        }
        composable(Routes.AI_BRAIN) {
            AIBrainScreen(
                onNavigate = { route -> onNavigateToTab(route) },
                onTriggerGeneration = { count -> onTriggerGeneration(count) }
            )
        }
        composable(Routes.GALLERY) {
            GalleryScreen()
        }
        composable(Routes.SETTINGS) {
            SettingsScreen()
        }
        composable(Routes.HISTORY) {
            HistoryScreen(onBack = { navController.popBackStack() })
        }
    }
}
