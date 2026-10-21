package com.pixelquest.app.ui.navigation

import androidx.navigation.NavController

/**
 * The routes whose back stacks start over Home: the tabs other than Home, and the Leaderboard, which
 * the trophy opens the same way. Deen's is the Islamic section's graph.
 */
private val OTHER_TABS = listOf(Screen.Tasks.route, Routes.ISLAMIC, Screen.Hero.route, Screen.Leaderboard.route)

/**
 * Opens a bottom-bar tab: keeps one copy of each tab on the back stack and restores its state, so
 * switching tabs never stacks screens. Used by the bottom bar, the trophy, and buttons that jump to
 * a tab, such as Hero's Leaderboard row.
 *
 * Tapping the tab you're already in goes back to its first screen. Restoring that tab's saved state
 * brought back the very screen you were on, so tapping the tab did nothing.
 */
fun NavController.navigateToTab(route: String) {
    if (route == currentTabRoute()) {
        // Deen's first screen is the hub: the graph itself is never a screen.
        popBackStack(if (route == Routes.ISLAMIC) Routes.ISLAMIC_HUB else route, inclusive = false)
        return
    }
    navigate(route) {
        popUpTo(Screen.Home.route) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * The tab the current screen belongs to, for the bottom bar's highlight: a screen opened from a tab
 * keeps it lit. Null on the Leaderboard, which has the trophy.
 */
fun NavController.currentPqTab(): PqTab? = PqTab.forRoute(currentTabRoute())

/** The tab the current screen belongs to. Switching tabs pops back to Home, so at most one other tab is on the stack. */
private fun NavController.currentTabRoute(): String =
    OTHER_TABS.firstOrNull { tab -> runCatching { getBackStackEntry(tab) }.isSuccess } ?: Screen.Home.route
