package com.pixelquest.app.ui.screens.insight

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pixelquest.app.audio.LocalSoundManager
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
    onToggleLatestExpanded: () -> Unit = {},
    olderInsights: List<OlderInsight> = emptyList(),
    onToggleOlder: (Long) -> Unit = {}
) {
    val colors = PixelTheme.colors
    val typography = MaterialTheme.typography
    val soundManager = LocalSoundManager.current

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // A fresh insight is asked for from the header, once there is one to replace.
        val refresh = (uiState as? AiInsightUiState.Success)?.let { success ->
            CoachRefreshAction(
                enabled = success.canRefresh,
                remainingSeconds = success.remainingCooldownSeconds,
                onClick = {
                    soundManager?.playClickSound()
                    onRefresh()
                }
            )
        }
        LightCoachHeader(refresh = refresh)

        Spacer(modifier = Modifier.height(CoachDimens.ItemGap))

        // State dispatch
        when (uiState) {
            is AiInsightUiState.Success -> {
                CoachInsightCard(
                    uiState = uiState,
                    themeMode = com.pixelquest.app.ui.theme.ThemeMode.Light,
                    expanded = !display.compact || display.latestExpanded,
                    onToggleExpanded = if (display.compact) onToggleLatestExpanded else null,
                )
                CoachOlderInsights(
                    items = olderInsights,
                    expandedId = display.expandedOlderId,
                    onToggle = onToggleOlder,
                    themeMode = com.pixelquest.app.ui.theme.ThemeMode.Light
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
