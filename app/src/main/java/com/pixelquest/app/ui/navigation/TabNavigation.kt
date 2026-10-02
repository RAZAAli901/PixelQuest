package com.pixelquest.app.ui.navigation

import androidx.navigation.NavController

/**
 * Opens a bottom-bar tab: keeps one copy of each tab on the back stack and restores its state, so
 * switching tabs never stacks screens. Used by the bottom bar and by buttons that jump to a tab,
 * such as GLOBAL LEADERBOARD on Stats.
 */
fun NavController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(Screen.Home.route) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
