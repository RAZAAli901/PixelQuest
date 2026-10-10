package com.pixelquest.app.ui.navigation

import androidx.navigation.NavController

/** The bottom bar's tabs other than Home, which is where every tab's back stack starts. */
private val OTHER_TABS = listOf(Screen.Tasks.route, Screen.Leaderboard.route, Screen.Stats.route, Screen.Profile.route)

/**
 * Opens a bottom-bar tab: keeps one copy of each tab on the back stack and restores its state, so
 * switching tabs never stacks screens. Used by the bottom bar and by buttons that jump to a tab,
 * such as GLOBAL LEADERBOARD on Stats.
 *
 * Tapping the tab you're already in goes back to its first screen. Restoring that tab's saved state
 * brought back the very screen you were on, so tapping HOME on Account (opened from Today's AI card),
 * or PROFILE on a screen opened from Profile, did nothing.
 */
fun NavController.navigateToTab(route: String) {
    if (route == currentTabRoute()) {
        popBackStack(route, inclusive = false)
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

/** The tab the current screen belongs to. Switching tabs pops back to Home, so at most one other tab is on the stack. */
private fun NavController.currentTabRoute(): String =
    OTHER_TABS.firstOrNull { tab -> runCatching { getBackStackEntry(tab) }.isSuccess } ?: Screen.Home.route
