package com.pixelquest.app.ui.screens.difficulty

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.domain.DifficultyMode
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelConfirmDialog
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.PixelTypography

@Composable
fun DifficultySelectionScreen(
    viewModel: DifficultyViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {}
) {
    val colors = PixelTheme.colors
    val state by viewModel.uiState.collectAsState()

    if (state.showWarningDialog && state.pendingLevel != null) {
        val target = state.pendingLevel!!
        val targetPct = (DifficultyMode.getPerfectDayThreshold(target) * 100).toInt()
        val daysReq = DifficultyMode.getDaysRequiredPerLevel(target)
        PixelConfirmDialog(
            title = "CHANGE DIFFICULTY?",
            message = "Changing difficulty will update your target to $targetPct% and level threshold to $daysReq days. Progress count carries over. Confirm difficulty change?",
            confirmText = "CONFIRM",
            dismissText = "CANCEL",
            onConfirm = { viewModel.confirmDifficultyChange() },
            onDismiss = { viewModel.dismissWarningDialog() }
        )
    }

    Scaffold(
        containerColor = colors.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(colors.background)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "🛡️ CHOOSE DIFFICULTY",
                style = PixelTypography.titleLarge,
                color = colors.primary
            )
            Text(
                text = "Select a difficulty level to balance your quest requirements.",
                style = PixelTypography.bodyMedium,
                color = colors.onSurfaceVariant
            )

            com.pixelquest.app.ui.components.PixelDifficultyCards(
                selectedLevel = state.currentLevel,
                onLevelSelected = { level -> viewModel.onDifficultyClicked(level) }
            )
        }
    }
}
