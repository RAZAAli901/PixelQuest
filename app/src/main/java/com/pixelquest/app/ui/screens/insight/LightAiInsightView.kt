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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.audio.LocalSoundManager
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelButtonVariant
import com.pixelquest.app.ui.theme.PixelTheme

/**
 * Step 19: Light-mode visual treatment for AI Habit Insights.
 * Emphasizes clean, daylight readability: crisp paper surfaces, slate borders,
 * high-contrast typography, and purposeful accent indicators.
 */
@Composable
fun LightAiInsightView(
    uiState: AiInsightUiState,
    onRefresh: () -> Unit,
    onNavigateToSettings: (() -> Unit)?,
    modifier: Modifier = Modifier,
    display: CoachDisplayState = CoachDisplayState(),
    onToggleLatestExpanded: () -> Unit = {}
) {
    val colors = PixelTheme.colors
    val typography = MaterialTheme.typography
    val soundManager = LocalSoundManager.current

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LightCoachHeader()

        Spacer(modifier = Modifier.height(CoachDimens.Header.BottomGap))

        // State dispatch
        when (uiState) {
            is AiInsightUiState.Success -> {
                CoachInsightCard(
                    uiState = uiState,
                    themeMode = com.pixelquest.app.ui.theme.ThemeMode.Light,
                    expanded = !display.compact || display.latestExpanded,
                    onToggleExpanded = if (display.compact) onToggleLatestExpanded else null,
                    onRefresh = {
                        soundManager?.playClickSound()
                        onRefresh()
                    }
                )
            }
            else -> {
                AiInsightStateRouter(
                    uiState = uiState,
                    themeMode = com.pixelquest.app.ui.theme.ThemeMode.Light,
                    onRefresh = onRefresh,
                    onNavigateToSettings = onNavigateToSettings
                )
            }
        }
    }
}
