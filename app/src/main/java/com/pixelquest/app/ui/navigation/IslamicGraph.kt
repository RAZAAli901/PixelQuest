package com.pixelquest.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.pixelquest.app.islamic.ui.IslamicHubScreen

/**
 * The Islamic section (docs/islamic-hub.md): one nested graph, starting at the hub, that holds every
 * Islamic screen. Its routes all begin `islamic/`, so the side menu can tell when the player is inside.
 *
 * @param hub the hub's screen, given what opening a card does; tests pass one without Hilt.
 */
fun NavGraphBuilder.islamicGraph(
    navController: NavHostController,
    hub: @Composable (onOpen: (route: String) -> Unit) -> Unit = { onOpen -> IslamicHubScreen(onOpen = onOpen) }
) {
    navigation(startDestination = Routes.ISLAMIC_HUB, route = Routes.ISLAMIC) {
        composable(Routes.ISLAMIC_HUB) {
            hub { route -> navController.navigate(route) { launchSingleTop = true } }
        }
    }
}
