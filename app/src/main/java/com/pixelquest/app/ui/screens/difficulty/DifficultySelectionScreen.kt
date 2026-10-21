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
import com.pixelquest.app.ui.components.PqTopBar
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType

@Composable
fun DifficultySelectionScreen(
    viewModel: DifficultyViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    if (state.showWarningDialog && state.pendingLevel != null) {
        val target = state.pendingLevel!!
        val targetPct = (DifficultyMode.getPerfectDayThreshold(target) * 100).toInt()
        val daysReq = DifficultyMode.getDaysRequiredPerLevel(target)
        PixelConfirmDialog(
            title = "Change difficulty?",
            message = "Changing difficulty will update your target to $targetPct% and level threshold to $daysReq days. Progress count carries over. Confirm difficulty change?",
            confirmText = "Change",
            dismissText = "Cancel",
            onConfirm = { viewModel.confirmDifficultyChange() },
            onDismiss = { viewModel.dismissWarningDialog() }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PqColors.Ground)
            .verticalScroll(rememberScrollState())
            .padding(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PqTopBar("Difficulty", onBack = onBackClick)
        Text(text = "How much of each day's quests counts as a perfect day, and how many days a level takes.", style = PqType.Body.copy(color = PqColors.Ink2))
        com.pixelquest.app.ui.components.PixelDifficultyCards(
            selectedLevel = state.currentLevel,
            onLevelSelected = { level -> viewModel.onDifficultyClicked(level) }
        )
    }
}
