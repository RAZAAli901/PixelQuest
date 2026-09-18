package com.pixelquest.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.ui.screens.home.HomeViewModel
import com.pixelquest.app.ui.screens.leveling.LevelUpCelebrationScreen
import com.pixelquest.app.ui.screens.today.TodayScreen

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToCreateTask: () -> Unit = {},
    onNavigateToEditTask: (Long) -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        TodayScreen(
            onNavigateToCreateTask = onNavigateToCreateTask,
            onNavigateToEditTask = onNavigateToEditTask,
            onNavigateToProfile = onNavigateToProfile
        )

        val pendingLevel = uiState.pendingLevelUp
        if (pendingLevel != null && !uiState.isSimpleMode) {
            LevelUpCelebrationScreen(
                level = pendingLevel,
                onDismiss = { viewModel.dismissLevelUpCelebration() }
            )
        }
    }
}

