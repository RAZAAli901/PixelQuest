package com.pixelquest.app.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation

/**
 * The Islamic section (docs/islamic-hub.md): one nested graph, starting at the hub, that holds every
 * Islamic screen. Its routes all begin `islamic/`, so the side menu can tell when the player is inside.
 */
fun NavGraphBuilder.islamicGraph(navController: NavHostController) {
    navigation(startDestination = Routes.ISLAMIC_HUB, route = Routes.ISLAMIC) {
        composable(Routes.ISLAMIC_HUB) {
            // The hub's screen comes next (IslamicHubScreen); nothing opens this route yet.
        }
    }
}
