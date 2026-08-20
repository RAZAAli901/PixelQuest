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
import com.pixelquest.app.ui.theme.PixelBackgroundDark
import com.pixelquest.app.ui.theme.PixelCyan
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelGreen
import com.pixelquest.app.ui.theme.PixelTextWhite
import com.pixelquest.app.ui.theme.PixelTypography

@Composable
fun DifficultySelectionScreen(
    viewModel: DifficultyViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    if (state.showWarningDialog && state.pendingLevel != null) {
        val target = state.pendingLevel!!
        val targetPct = (DifficultyMode.getPerfectDayThreshold(target) * 100).toInt()
        PixelConfirmDialog(
            title = "CHANGE DIFFICULTY?",
            message = "Changing difficulty mid-streak will update your perfect day target to $targetPct%. Are you sure you want to change difficulty?",
            confirmText = "CONFIRM",
            dismissText = "CANCEL",
            onConfirm = { viewModel.confirmDifficultyChange() },
            onDismiss = { viewModel.dismissWarningDialog() }
        )
    }

    Scaffold(
        containerColor = PixelBackgroundDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(PixelBackgroundDark)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "🛡️ CHOOSE DIFFICULTY",
                style = PixelTypography.titleLarge,
                color = PixelGold
            )
            Text(
                text = "Select a difficulty level to balance your quest requirements.",
                style = PixelTypography.bodyMedium,
                color = PixelTextWhite
            )

            DifficultyLevel.values().forEach { level ->
                val isSelected = level == state.currentLevel
                val thresholdPct = (DifficultyMode.getPerfectDayThreshold(level) * 100).toInt()
                val daysReq = DifficultyMode.getDaysRequiredPerLevel(level)

                PixelCard(
                    variant = if (isSelected) PixelPanelVariant.BLUE else PixelPanelVariant.BEIGE,
                    contentPadding = 16.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.onDifficultyClicked(level) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = DifficultyMode.getDisplayName(level).uppercase(),
                                style = PixelTypography.titleMedium,
                                color = if (isSelected) PixelGold else PixelGreen
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Perfect Day: $thresholdPct% completed",
                                style = PixelTypography.bodySmall,
                                color = PixelCyan
                            )
                            Text(
                                text = "Days per Level: $daysReq days",
                                style = PixelTypography.labelSmall,
                                color = PixelTextWhite
                            )
                        }
                        if (isSelected) {
                            Text(
                                text = "ACTIVE",
                                style = PixelTypography.labelMedium,
                                color = PixelGold
                            )
                        }
                    }
                }
            }
        }
    }
}
