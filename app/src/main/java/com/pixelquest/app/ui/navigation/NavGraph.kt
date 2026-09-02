package com.pixelquest.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

import com.pixelquest.app.ui.screens.leveling.LevelHistoryScreen

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object Tasks : Screen("tasks")
    object Stats : Screen("stats")
    object Profile : Screen("profile")
    object AvatarSelection : Screen("avatar_selection")
    object DifficultySelection : Screen("difficulty_selection")
    object LevelHistory : Screen("level_history")
    object TaskHistory : Screen("task_history")
    object TaskAnalytics : Screen("task_analytics/{taskId}") {
        fun createRoute(taskId: Long) = "task_analytics/$taskId"
    }
    object CreateTask : Screen("create_task")
    object EditTask : Screen("edit_task/{taskId}") {
        fun createRoute(taskId: Long) = "edit_task/$taskId"
    }
    object Onboarding : Screen("onboarding")
    object Settings : Screen("settings")
}

@Composable
fun PixelNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Splash.route,
    onboardingComplete: Boolean = true
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { PixelTransitions.Enter },
        exitTransition = { PixelTransitions.Exit },
        popEnterTransition = { PixelTransitions.PopEnter },
        popExitTransition = { PixelTransitions.PopExit }
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashTimeout = {
                    val target = if (!onboardingComplete) Screen.Onboarding.route else Screen.Home.route
                    navController.navigate(target) {
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
        composable(
            route = Screen.CreateTask.route,
            enterTransition = { PixelTransitions.ModalEnter },
            exitTransition = { PixelTransitions.ModalExit },
            popEnterTransition = { PixelTransitions.ModalPopEnter },
            popExitTransition = { PixelTransitions.ModalPopExit }
        ) {
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
            arguments = listOf(navArgument("taskId") { type = NavType.LongType }),
            enterTransition = { PixelTransitions.ModalEnter },
            exitTransition = { PixelTransitions.ModalExit },
            popEnterTransition = { PixelTransitions.ModalPopEnter },
            popExitTransition = { PixelTransitions.ModalPopExit }
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
            StatsScreen(
                onNavigateToTaskHistory = {
                    navController.navigate(Screen.TaskHistory.route)
                },
                onNavigateToLevelHistory = {
                    navController.navigate(Screen.LevelHistory.route)
                },
                onNavigateToTaskAnalytics = { taskId ->
                    navController.navigate(Screen.TaskAnalytics.createRoute(taskId))
                }
            )
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateToAvatarSelection = {
                    navController.navigate(Screen.AvatarSelection.route)
                },
                onNavigateToDifficulty = {
                    navController.navigate(Screen.DifficultySelection.route)
                },
                onNavigateToLevelHistory = {
                    navController.navigate(Screen.LevelHistory.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        composable(Screen.AvatarSelection.route) {
            val avatarViewModel: com.pixelquest.app.ui.screens.avatar.AvatarSelectionViewModel = hiltViewModel()
            val currentAvatarId by avatarViewModel.currentAvatarId.collectAsState()
            com.pixelquest.app.ui.screens.avatar.AvatarSelectionScreen(
                currentAvatarId = currentAvatarId,
                onAvatarSelected = { selectedId ->
                    avatarViewModel.selectAvatar(selectedId)
                },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.LevelHistory.route) {
            LevelHistoryScreen()
        }
        composable(Screen.DifficultySelection.route) {
            com.pixelquest.app.ui.screens.difficulty.DifficultySelectionScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.TaskHistory.route) {
            com.pixelquest.app.ui.screens.history.TaskHistoryScreen(
                onTaskClick = { taskId ->
                    navController.navigate(Screen.TaskAnalytics.createRoute(taskId))
                }
            )
        }
        composable(
            route = Screen.TaskAnalytics.route,
            arguments = listOf(navArgument("taskId") { type = NavType.LongType })
        ) {
            com.pixelquest.app.ui.screens.analytics.TaskAnalyticsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Onboarding.route) {
            com.pixelquest.app.ui.screens.onboarding.OnboardingFlowScreen(
                onOnboardingComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Settings.route) {
            com.pixelquest.app.ui.screens.settings.SettingsScreen(
                onNavigateToDifficulty = { navController.navigate(Screen.DifficultySelection.route) },
                onNavigateToAvatar = { navController.navigate(Screen.AvatarSelection.route) },
                onResetComplete = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}

