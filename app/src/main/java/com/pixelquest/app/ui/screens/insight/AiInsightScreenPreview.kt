package com.pixelquest.app.ui.screens.insight

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pixelquest.app.domain.ai.HabitInsightResponse
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.ThemeMode

/**
 * Step 22: Comprehensive Compose Previews for [AiInsightScreen]
 * covering all three themes (Pixel, Light, Comic) and every UI state.
 */

private val previewMockInsight = HabitInsightResponse(
    summary = "You completed 5 of 6 quests this week, maintaining a 4-day streak with peak activity in the morning!",
    suggestion = "Pair your evening meditation quest immediately after dinner to avoid late-night fatigue skips.",
    encouragement = "Your hero momentum is surging! Level 6 is within your grasp if you complete tomorrow's daily quest.",
    generatedAt = System.currentTimeMillis() - 3600000L
)

@Preview(name = "Pixel Theme - Success (Cached)", group = "Theme - Pixel")
@Composable
fun AiInsightScreenPixelSuccessPreview() {
    PixelQuestTheme(themeMode = ThemeMode.Pixel) {
        AiInsightScreenContent(
            uiState = AiInsightUiState.Success(
                insight = previewMockInsight,
                isCached = true,
                remainingCooldownSeconds = 7200L,
                canRefresh = false
            ),
            onRefresh = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "Light Theme - Success (Live)", group = "Theme - Light")
@Composable
fun AiInsightScreenLightSuccessPreview() {
    PixelQuestTheme(themeMode = ThemeMode.Light) {
        AiInsightScreenContent(
            uiState = AiInsightUiState.Success(
                insight = previewMockInsight,
                isCached = false,
                remainingCooldownSeconds = 0L,
                canRefresh = true
            ),
            onRefresh = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "Comic Theme - Success", group = "Theme - Comic")
@Composable
fun AiInsightScreenComicSuccessPreview() {
    PixelQuestTheme(themeMode = ThemeMode.Comic) {
        AiInsightScreenContent(
            uiState = AiInsightUiState.Success(
                insight = previewMockInsight,
                isCached = false,
                remainingCooldownSeconds = 0L,
                canRefresh = true
            ),
            onRefresh = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "Pixel Theme - Loading", group = "State - Loading")
@Composable
fun AiInsightScreenPixelLoadingPreview() {
    PixelQuestTheme(themeMode = ThemeMode.Pixel) {
        AiInsightScreenContent(
            uiState = AiInsightUiState.Loading,
            onRefresh = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "Comic Theme - Loading", group = "State - Loading")
@Composable
fun AiInsightScreenComicLoadingPreview() {
    PixelQuestTheme(themeMode = ThemeMode.Comic) {
        AiInsightScreenContent(
            uiState = AiInsightUiState.Loading,
            onRefresh = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "Light Theme - Loading", group = "State - Loading")
@Composable
fun AiInsightScreenLightLoadingPreview() {
    PixelQuestTheme(themeMode = ThemeMode.Light) {
        AiInsightScreenContent(
            uiState = AiInsightUiState.Loading,
            onRefresh = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "Pixel Theme - Rate Limited", group = "State - RateLimited")
@Composable
fun AiInsightScreenPixelRateLimitedPreview() {
    PixelQuestTheme(themeMode = ThemeMode.Pixel) {
        AiInsightScreenContent(
            uiState = AiInsightUiState.RateLimited(
                retryAfterSeconds = 14400L,
                message = "Sage oracle is recharging. Check back in 4 hours for a fresh insight.",
                lastInsight = previewMockInsight
            ),
            onRefresh = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "Comic Theme - Rate Limited", group = "State - RateLimited")
@Composable
fun AiInsightScreenComicRateLimitedPreview() {
    PixelQuestTheme(themeMode = ThemeMode.Comic) {
        AiInsightScreenContent(
            uiState = AiInsightUiState.RateLimited(
                retryAfterSeconds = 14400L,
                message = "HQ transmission frequency locked! Next window opens in 4 hours.",
                lastInsight = previewMockInsight
            ),
            onRefresh = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "Pixel Theme - Not Enough Data", group = "State - NotEnoughData")
@Composable
fun AiInsightScreenPixelNotEnoughDataPreview() {
    PixelQuestTheme(themeMode = ThemeMode.Pixel) {
        AiInsightScreenContent(
            uiState = AiInsightUiState.NotEnoughData(
                daysLogged = 1,
                minimumRequiredDays = 3,
                message = "Keep adventuring! Complete daily quests for at least 3 days to unlock AI Questmaster Insights.",
                encouragingTip = "Every legend begins with a single step. Complete today's quests!"
            ),
            onRefresh = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "Light Theme - Not Enough Data", group = "State - NotEnoughData")
@Composable
fun AiInsightScreenLightNotEnoughDataPreview() {
    PixelQuestTheme(themeMode = ThemeMode.Light) {
        AiInsightScreenContent(
            uiState = AiInsightUiState.NotEnoughData(
                daysLogged = 2,
                minimumRequiredDays = 3,
                message = "Log habits for at least 3 days to generate personalized AI coaching.",
                encouragingTip = "Consistency over intensity creates lasting habit change."
            ),
            onRefresh = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "Comic Theme - Not Enough Data", group = "State - NotEnoughData")
@Composable
fun AiInsightScreenComicNotEnoughDataPreview() {
    PixelQuestTheme(themeMode = ThemeMode.Comic) {
        AiInsightScreenContent(
            uiState = AiInsightUiState.NotEnoughData(
                daysLogged = 1,
                minimumRequiredDays = 3,
                message = "HQ needs at least 3 mission logs before computing tactical battle intel!",
                encouragingTip = "Smash today's objectives to unlock the Oracle's guidance!"
            ),
            onRefresh = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "Pixel Theme - Disabled", group = "State - Disabled")
@Composable
fun AiInsightScreenPixelDisabledPreview() {
    PixelQuestTheme(themeMode = ThemeMode.Pixel) {
        AiInsightScreenContent(
            uiState = AiInsightUiState.Disabled(),
            onRefresh = {},
            onNavigateBack = {},
            onNavigateToSettings = {}
        )
    }
}

@Preview(name = "Comic Theme - Disabled", group = "State - Disabled")
@Composable
fun AiInsightScreenComicDisabledPreview() {
    PixelQuestTheme(themeMode = ThemeMode.Comic) {
        AiInsightScreenContent(
            uiState = AiInsightUiState.Disabled(
                message = "AI Coaching transmissions are deactivated. Turn them ON in Settings!"
            ),
            onRefresh = {},
            onNavigateBack = {},
            onNavigateToSettings = {}
        )
    }
}

@Preview(name = "Pixel Theme - Error", group = "State - Error")
@Composable
fun AiInsightScreenPixelErrorPreview() {
    PixelQuestTheme(themeMode = ThemeMode.Pixel) {
        AiInsightScreenContent(
            uiState = AiInsightUiState.Error(
                message = "Network connection failed. Unable to reach AI oracle.",
                canRetry = true,
                fallbackInsight = previewMockInsight
            ),
            onRefresh = {},
            onNavigateBack = {}
        )
    }
}

// The compact layout (docs/ai-coach-compact.md): a 6-inch phone, collapsed and expanded, in each theme.

private val previewOlderInsights = listOf(
    OlderInsight(2, previewMockInsight.copy(summary = "Mornings went well, evenings slipped.", generatedAt = System.currentTimeMillis() - 86_400_000L)),
    OlderInsight(1, previewMockInsight.copy(summary = "A steady start to the week.", generatedAt = System.currentTimeMillis() - 172_800_000L))
)

@Composable
private fun CompactCoachPreview(mode: ThemeMode, expanded: Boolean, simple: Boolean = false) {
    PixelQuestTheme(themeMode = mode) {
        AiInsightScreenContent(
            uiState = AiInsightUiState.Success(insight = previewMockInsight, isCached = true),
            isSimpleMode = simple,
            displayState = CoachDisplayState(latestExpanded = expanded),
            onRefresh = {},
            olderInsights = previewOlderInsights,
            onNavigateBack = {}
        )
    }
}

@Preview(name = "Compact - Pixel", group = "Compact", widthDp = 360, heightDp = 640)
@Composable
fun AiInsightCompactPixelPreview() = CompactCoachPreview(ThemeMode.Pixel, expanded = false)

@Preview(name = "Compact - Comic", group = "Compact", widthDp = 360, heightDp = 640)
@Composable
fun AiInsightCompactComicPreview() = CompactCoachPreview(ThemeMode.Comic, expanded = false)

@Preview(name = "Compact - Light", group = "Compact", widthDp = 360, heightDp = 640)
@Composable
fun AiInsightCompactLightPreview() = CompactCoachPreview(ThemeMode.Light, expanded = false)

@Preview(name = "Compact - Simple Mode", group = "Compact", widthDp = 360, heightDp = 640)
@Composable
fun AiInsightCompactSimplePreview() = CompactCoachPreview(ThemeMode.Pixel, expanded = false, simple = true)

@Preview(name = "Expanded - Pixel", group = "Compact", widthDp = 360, heightDp = 640)
@Composable
fun AiInsightExpandedPixelPreview() = CompactCoachPreview(ThemeMode.Pixel, expanded = true)

@Preview(name = "Expanded - Comic", group = "Compact", widthDp = 360, heightDp = 640)
@Composable
fun AiInsightExpandedComicPreview() = CompactCoachPreview(ThemeMode.Comic, expanded = true)
