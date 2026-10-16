package com.pixelquest.app.ui.screens.insight

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.audio.LocalSoundManager
import com.pixelquest.app.domain.ai.HabitInsightResponse
import com.pixelquest.app.ui.components.ComicButton
import com.pixelquest.app.ui.components.ComicButtonVariant
import com.pixelquest.app.ui.components.ComicPanel
import com.pixelquest.app.ui.components.ComicPanelVariant
import com.pixelquest.app.ui.theme.BangersFontFamily
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.comicBorder
import com.pixelquest.app.ui.theme.comicDropShadow

/**
 * Step 20: Comic-mode visual treatment for AI Habit Insights.
 * Renders the coach feedback inside pop-art speech/thought-bubble panels,
 * complete with dynamic comic typography (Bangers), bold black ink outlines,
 * flat drop shadows, and dramatic action bubbles.
 */
@Composable
fun ComicAiInsightView(
    uiState: AiInsightUiState,
    onRefresh: () -> Unit,
    onNavigateToSettings: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val soundManager = LocalSoundManager.current

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ComicCoachHeader()

        Spacer(modifier = Modifier.height(CoachDimens.Header.BottomGap))

        // State dispatch
        when (uiState) {
            is AiInsightUiState.Success -> {
                CoachInsightCard(
                    uiState = uiState,
                    themeMode = com.pixelquest.app.ui.theme.ThemeMode.Comic,
                    onRefresh = {
                        soundManager?.playClickSound()
                        onRefresh()
                    }
                )
            }
            else -> {
                AiInsightStateRouter(
                    uiState = uiState,
                    themeMode = com.pixelquest.app.ui.theme.ThemeMode.Comic,
                    onRefresh = onRefresh,
                    onNavigateToSettings = onNavigateToSettings
                )
            }
        }
    }
}
