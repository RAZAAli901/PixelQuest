package com.pixelquest.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.pixelquest.app.ui.screens.HomeScreen
import com.pixelquest.app.ui.screens.ProfileScreen
import com.pixelquest.app.ui.screens.SplashScreen
import com.pixelquest.app.ui.screens.StatsScreen
import com.pixelquest.app.ui.screens.TasksScreen
import com.pixelquest.app.ui.screens.tasks.CreateTaskScreen
import com.pixelquest.app.ui.screens.tasks.TaskFormViewModel

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object Tasks : Screen("tasks")
    object Stats : Screen("stats")
    object Profile : Screen("profile")
    object DifficultySelection : Screen("difficulty_selection")
    object CreateTask : Screen("create_task")
    object EditTask : Screen("edit_task/{taskId}") {
        fun createRoute(taskId: Long) = "edit_task/$taskId"
    }
}

@Composable
fun PixelNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashTimeout = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen()
        }
        composable(Screen.Tasks.route) {
            TasksScreen(
                onNavigateToCreateTask = {
                    navController.navigate(Screen.CreateTask.route)
                },
                onNavigateToEditTask = { taskId ->
                    navController.navigate(Screen.EditTask.createRoute(taskId))
                }
            )
        }
        composable(Screen.CreateTask.route) {
            val formViewModel: TaskFormViewModel = hiltViewModel()
            LaunchedEffect(Unit) {
                formViewModel.resetForm()
            }
            CreateTaskScreen(
                viewModel = formViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.EditTask.route,
            arguments = listOf(navArgument("taskId") { type = NavType.LongType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getLong("taskId") ?: 0L
            val formViewModel: TaskFormViewModel = hiltViewModel()
            LaunchedEffect(taskId) {
                formViewModel.loadTask(taskId)
            }
            CreateTaskScreen(
                viewModel = formViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Stats.route) {
            StatsScreen()
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateToDifficulty = {
                    navController.navigate(Screen.DifficultySelection.route)
                }
            )
        }
        composable(Screen.DifficultySelection.route) {
            com.pixelquest.app.ui.screens.difficulty.DifficultySelectionScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

