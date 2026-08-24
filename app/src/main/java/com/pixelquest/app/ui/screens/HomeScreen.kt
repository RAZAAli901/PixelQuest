package com.pixelquest.app.ui.screens

import androidx.compose.runtime.Composable
import com.pixelquest.app.ui.screens.today.TodayScreen

@Composable
fun HomeScreen(
    onNavigateToEditTask: (Long) -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    TodayScreen(
        onNavigateToEditTask = onNavigateToEditTask,
        onNavigateToProfile = onNavigateToProfile
    )
}

