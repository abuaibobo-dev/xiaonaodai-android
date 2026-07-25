package com.meitu.generator.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.meitu.generator.ui.assistant.AssistantScreen
import com.meitu.generator.ui.cloudbuild.CloudBuildScreen
import com.meitu.generator.ui.projects.ProjectScreen
import com.meitu.generator.ui.settings.SettingsScreen

object Routes {
    const val ASSISTANT = "assistant"
    const val PROJECTS = "projects"
    const val CLOUD_BUILD = "cloud-build"
    const val SETTINGS = "settings"
}

@Composable
fun NavGraph(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Routes.ASSISTANT
    ) {
        composable(Routes.ASSISTANT) {
            AssistantScreen()
        }
        composable(Routes.PROJECTS) {
            ProjectScreen()
        }
        composable(Routes.CLOUD_BUILD) {
            CloudBuildScreen()
        }
        composable(Routes.SETTINGS) {
            SettingsScreen()
        }
    }
}
