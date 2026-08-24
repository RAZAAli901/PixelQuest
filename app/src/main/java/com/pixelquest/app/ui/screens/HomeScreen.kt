package com.pixelquest.app.ui.screens

import androidx.compose.runtime.Composable
import com.pixelquest.app.ui.screens.today.TodayScreen

@Composable
fun HomeScreen(
    onNavigateToCreateTask: () -> Unit = {},
    onNavigateToEditTask: (Long) -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    TodayScreen(
        onNavigateToCreateTask = onNavigateToCreateTask,
        onNavigateToEditTask = onNavigateToEditTask,
        onNavigateToProfile = onNavigateToProfile
    )
}

