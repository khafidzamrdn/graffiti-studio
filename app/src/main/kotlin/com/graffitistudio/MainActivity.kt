package com.graffitistudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.graffitistudio.core.designsystem.theme.GraffitiStudioTheme
import com.graffitistudio.feature.home.HomeScreen
import com.graffitistudio.feature.home.HomeViewModel
import com.graffitistudio.feature.workspace.WorkspaceScreen
import com.graffitistudio.feature.workspace.WorkspaceViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as GraffitiStudioApp).container

        setContent {
            GraffitiStudioTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "home"
                ) {
                    composable("home") {
                        val homeViewModel: HomeViewModel = viewModel(
                            factory = HomeViewModel.Factory(container.projectRepository)
                        )
                        HomeScreen(
                            viewModel = homeViewModel,
                            onOpenProject = { projectId ->
                                navController.navigate("workspace/$projectId")
                            }
                        )
                    }

                    composable(
                        route = "workspace/{projectId}",
                        arguments = listOf(navArgument("projectId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
                        val workspaceViewModel: WorkspaceViewModel = viewModel(
                            factory = WorkspaceViewModel.Factory(
                                projectId = projectId,
                                projectRepository = container.projectRepository,
                                preferences = container.workspacePreferences,
                                artworkExporter = container.artworkExporter,
                                appContext = applicationContext
                            )
                        )
                        WorkspaceScreen(
                            viewModel = workspaceViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
