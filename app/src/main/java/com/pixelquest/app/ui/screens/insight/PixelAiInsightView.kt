package com.pixelquest.app.ui.screens.insight

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pixelquest.app.audio.LocalSoundManager
import com.pixelquest.app.ui.theme.PixelTheme

/**
 * Step 18: Pixel-mode visual treatment for AI Habit Insights.
 * Emphasizes 8-bit retro arcade identity: CRT monitor vibe, dark background,
 * scanline aesthetics, golden quest badges, and terminal-style telemetry headers.
 */
@Composable
fun PixelAiInsightView(
    uiState: AiInsightUiState,
    onRefresh: () -> Unit,
    onNavigateToSettings: (() -> Unit)?,
    isSimpleMode: Boolean = false,
    modifier: Modifier = Modifier,
    display: CoachDisplayState = CoachDisplayState(),
    onToggleLatestExpanded: () -> Unit = {},
    olderInsights: List<OlderInsight> = emptyList(),
    onToggleOlder: (Long) -> Unit = {}
) {
    val colors = PixelTheme.colors
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
        PixelCoachHeader(isSimpleMode = isSimpleMode, refresh = refresh)

        Spacer(modifier = Modifier.height(CoachDimens.ItemGap))

        // State-driven body
        when (uiState) {
            is AiInsightUiState.Success -> {
                CoachInsightCard(
                    uiState = uiState,
                    themeMode = com.pixelquest.app.ui.theme.ThemeMode.Pixel,
                    expanded = !display.compact || display.latestExpanded,
                    onToggleExpanded = if (display.compact) onToggleLatestExpanded else null,
                    isSimpleMode = isSimpleMode,
                )
                CoachOlderInsights(
                    items = olderInsights,
                    expandedId = display.expandedOlderId,
                    onToggle = onToggleOlder,
                    themeMode = com.pixelquest.app.ui.theme.ThemeMode.Pixel
                )
            }
            else -> {
                // Dispatches non-success states through the state router
                AiInsightStateRouter(
                    uiState = uiState,
                    themeMode = com.pixelquest.app.ui.theme.ThemeMode.Pixel,
                    onRefresh = onRefresh,
                    onNavigateToSettings = onNavigateToSettings
                )
            }
        }
    }
}
