package com.pixelquest.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.pixelquest.app.ui.screens.HomeScreen
import com.pixelquest.app.ui.screens.SplashScreen
import com.pixelquest.app.ui.screens.tasks.CreateTaskScreen
import com.pixelquest.app.ui.screens.tasks.TaskFormViewModel

import com.pixelquest.app.ui.screens.leveling.LevelHistoryScreen
import com.pixelquest.app.party.ui.partiesScreens

@Composable
fun PixelNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Splash.route,
    onboardingComplete: Boolean = true,
    /** The Islamic hub's items (IslamicRegistry.providers), whose own screens join the Islamic section. */
    islamicItems: List<com.pixelquest.app.islamic.IslamicItemProvider> = emptyList()
) {
    // The NavHost keeps the first composable lambdas it is given, so read the flag through
    // State; otherwise the splash timeout sees the initial value and skips onboarding.
    val latestOnboardingComplete by rememberUpdatedState(onboardingComplete)
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
                    val target = if (!latestOnboardingComplete) Screen.Onboarding.route else Screen.Home.route
                    navController.navigate(target) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToCreateTask = {
                    navController.navigate(Screen.CreateTask.route)
                },
                onNavigateToEditTask = { taskId ->
                    navController.navigate(Screen.EditTask.createRoute(taskId))
                },
                onNavigateToProfile = {
                    navController.navigateToTab(Screen.Hero.route)
                },
                onNavigateToAiInsight = {
                    navController.navigate(Screen.AiInsight.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToAccount = {
                    navController.navigate(Screen.Account.route)
                },
                onNavigateToNamaz = {
                    // Inside the Islamic section: Back goes to the hub, then Home.
                    navController.navigateInIslamic(Screen.Namaz.route)
                },
                onSeeAllQuests = { navController.navigateToTab(Screen.Tasks.route) },
                onOpenStudyParty = { partyId -> navController.navigate(Screen.PartiesParty.createRoute(partyId)) { launchSingleTop = true } },
                onOpenParties = { navController.navigate(Screen.Parties.route) { launchSingleTop = true } }
            )
        }
        composable(Screen.AiInsight.route) {
            com.pixelquest.app.ui.screens.insight.AiInsightScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToAccount = { navController.navigate(Screen.Account.route) }
            )
        }
        composable(Screen.Tasks.route) {
            com.pixelquest.app.ui.screens.QuestsScreen(
                onNewQuest = { navController.navigate(Screen.CreateTask.route) },
                onEditQuest = { taskId -> navController.navigate(Screen.EditTask.createRoute(taskId)) },
                onOpenHistoryQuest = { taskId -> navController.navigate(Screen.TaskAnalytics.createRoute(taskId)) }
            )
        }
        composable(
            route = Screen.CreateTask.route,
            enterTransition = { PixelTransitions.ModalEnter },
            exitTransition = { PixelTransitions.ModalExit },
            popEnterTransition = { PixelTransitions.ModalPopEnter },
            popExitTransition = { PixelTransitions.ModalPopExit }
        ) {
            // A fresh ViewModel per visit starts empty; no reset here, which used to run again on
            // rotation and wipe what the player had typed.
            val formViewModel: TaskFormViewModel = hiltViewModel()
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
        composable(Screen.Hero.route) {
            com.pixelquest.app.ui.screens.HeroScreen(
                onSettings = { navController.navigate(Screen.Settings.route) },
                onChangeAvatar = { navController.navigate(Screen.AvatarSelection.route) },
                onDifficulty = { navController.navigate(Screen.DifficultySelection.route) },
                onQuestHistory = { navController.navigate(Screen.TaskHistory.route) },
                onLevelHistory = { navController.navigate(Screen.LevelHistory.route) },
                // The trophy's screen, so open it the way the trophy does.
                onLeaderboard = { navController.navigateToTab(Screen.Leaderboard.route) },
                onParties = { navController.navigate(Screen.Parties.route) { launchSingleTop = true } },
                onUsername = { navController.navigate(Screen.UsernameSetup.route) { launchSingleTop = true } }
            )
        }
        composable(Screen.AvatarSelection.route) {
            com.pixelquest.app.ui.screens.AvatarPickerRoute(onBack = { navController.popBackStack() })
        }
        composable(Screen.LevelHistory.route) {
            LevelHistoryScreen()
        }
        composable(Screen.DifficultySelection.route) {
            com.pixelquest.app.ui.screens.difficulty.DifficultySelectionScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        // Hero's Quest history: Quests, open at its History, with a way back to Hero.
        composable(Screen.TaskHistory.route) {
            com.pixelquest.app.ui.screens.QuestsScreen(
                initialTab = com.pixelquest.app.ui.screens.QuestTab.History,
                onNavigateBack = { navController.popBackStack() },
                onNewQuest = { navController.navigate(Screen.CreateTask.route) },
                onEditQuest = { taskId -> navController.navigate(Screen.EditTask.createRoute(taskId)) },
                onOpenHistoryQuest = { taskId -> navController.navigate(Screen.TaskAnalytics.createRoute(taskId)) }
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
                onNavigateToAccount = { navController.navigate(Screen.Account.route) },
                onNavigateBack = { navController.popBackStack() },
                onResetComplete = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Account.route) {
            com.pixelquest.app.ui.screens.account.AccountScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.UsernameSetup.route) {
            com.pixelquest.app.darood.ui.username.UsernameSetupScreen(
                onDone = { navController.popBackStack() },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        islamicGraph(navController, islamicItems) {
            // The Islamic screen from before the hub (docs/islamic-hub.md): the Namaz Tracker.
            composable(Screen.Namaz.route) {
                com.pixelquest.app.ui.screens.namaz.NamazScreen(onNavigateBack = { navController.popBackStack() })
            }
        }
        // The parties of every kind, Darood's too, outside the Islamic section (docs/party-core.md).
        partiesScreens(navController)
        // Namaz's and Darood Party's routes before the hub, for notifications set by 1.4.
        redirect(Routes.LEGACY_NAMAZ, Screen.Namaz.route, navController)
        redirect(Routes.LEGACY_DAROOD, Screen.Parties.route, navController)
        composable(Screen.Leaderboard.route) {
            val leaderboardViewModel: com.pixelquest.app.ui.screens.leaderboard.LeaderboardViewModel = hiltViewModel()
            com.pixelquest.app.ui.screens.leaderboard.LeaderboardScreen(
                viewModel = leaderboardViewModel,
                onNavigateToAccount = { navController.navigate(Screen.Account.route) }
            )
        }
    }
}
