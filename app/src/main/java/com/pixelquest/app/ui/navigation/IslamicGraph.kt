package com.pixelquest.app.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.pixelquest.app.islamic.IslamicItemProvider
import com.pixelquest.app.islamic.ui.IslamicHubScreen

/**
 * The Islamic section (docs/islamic-hub.md): one nested graph, starting at the hub, that holds every
 * Islamic screen. Its routes all begin `islamic/`, so the side menu can tell when the player is inside.
 *
 * @param items the hub's items (IslamicRegistry.providers): each adds its own screens, if it has any.
 * @param hub the hub's screen, given what opening a card does; tests pass one without Hilt.
 * @param screens the section's other screens: those from before the hub (Namaz's, Darood Party's),
 *   which NavGraph adds here.
 */
fun NavGraphBuilder.islamicGraph(
    navController: NavHostController,
    items: List<IslamicItemProvider> = emptyList(),
    hub: @Composable (onOpen: (route: String) -> Unit) -> Unit = { onOpen -> IslamicHubScreen(onOpen = onOpen) },
    screens: NavGraphBuilder.() -> Unit = {}
) {
    navigation(startDestination = Routes.ISLAMIC_HUB, route = Routes.ISLAMIC) {
        composable(Routes.ISLAMIC_HUB) {
            hub { route -> navController.navigate(route) { launchSingleTop = true } }
        }
        screens()
        items.forEach { item -> with(item) { screens(navController) } }
    }
}

/**
 * An old route that only opens [to] in its place, so whatever still carries it (a notification set
 * before an update) lands on the right screen. It shows nothing and leaves nothing on the back stack.
 */
fun NavGraphBuilder.redirect(from: String, to: String, navController: NavController) {
    composable(
        route = from,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        LaunchedEffect(Unit) {
            navController.navigate(to) { popUpTo(from) { inclusive = true } }
        }
    }
}
