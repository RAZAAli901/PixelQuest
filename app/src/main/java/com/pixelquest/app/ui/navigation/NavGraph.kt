package com.pixelquest.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object Tasks : Screen("tasks")
    object Stats : Screen("stats")
    object Profile : Screen("profile")
}

@Composable
fun PixelNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Splash.route,
    onSplashFinish: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Splash.route) {
            // Splash destination placeholder
        }
        composable(Screen.Home.route) {
            // Home destination placeholder
        }
        composable(Screen.Tasks.route) {
            // Tasks destination placeholder
        }
        composable(Screen.Stats.route) {
            // Stats destination placeholder
        }
        composable(Screen.Profile.route) {
            // Profile destination placeholder
        }
    }
}
