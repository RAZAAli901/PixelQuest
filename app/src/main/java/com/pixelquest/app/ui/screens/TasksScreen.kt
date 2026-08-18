package com.pixelquest.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.ui.screens.tasks.TaskUiState
import com.pixelquest.app.ui.screens.tasks.TaskViewModel
import com.pixelquest.app.ui.theme.PixelBackgroundDark
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelTypography

@Composable
fun TasksScreen(
    viewModel: TaskViewModel = hiltViewModel(),
    onNavigateToCreateTask: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateTask,
                containerColor = PixelGold,
                contentColor = PixelBackgroundDark
            ) {
                Text(
                    text = "+",
                    style = PixelTypography.displaySmall
                )
            }
        },
        containerColor = PixelBackgroundDark
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(PixelBackgroundDark),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is TaskUiState.Loading -> {
                    Text("LOADING QUESTS...", style = PixelTypography.bodyMedium, color = PixelGold)
                }
                is TaskUiState.Error -> {
                    Text("ERROR: ${state.message}", style = PixelTypography.bodyMedium, color = PixelGold)
                }
                is TaskUiState.Success -> {
                    Text("QUESTS: ${state.tasks.size}", style = PixelTypography.bodyMedium, color = PixelGold)
                }
            }
        }
    }
}
