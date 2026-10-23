package com.pixelquest.app.zz

import com.pixelquest.app.domain.ai.HabitInsightResponse
import com.pixelquest.app.ui.screens.insight.AiInsightScreenContent
import com.pixelquest.app.ui.screens.insight.AiInsightUiState
import com.pixelquest.app.ui.screens.insight.CoachDisplayState
import com.pixelquest.app.ui.screens.insight.OlderInsight
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.PqSkin
import org.junit.Test

class CoachScreenShotTest : ScratchShot() {
    private val success = AiInsightUiState.Success(
        insight = HabitInsightResponse(
            "You completed 18 of 21 quests this week, with mornings your strongest time: every fitness quest before 9am got done, while two of three evening reading quests slipped.",
            "Move one reading quest to the morning, right after your workout.",
            "Three perfect days in a row is your best run yet.",
            generatedAt = 1_760_000_000_000L
        ),
        isCached = false,
        remainingCooldownSeconds = 3 * 3600L
    )
    private val older = listOf(
        OlderInsight(2, HabitInsightResponse("Mornings went well, evenings slipped.", "Try reading at lunch.", "Keep going.", generatedAt = 1_759_900_000_000L)),
        OlderInsight(1, HabitInsightResponse("A steady start to the week.", "Add a short walk.", "Nice work.", generatedAt = 1_759_800_000_000L))
    )

    private fun content() = @androidx.compose.runtime.Composable {
        PixelQuestTheme {
            AiInsightScreenContent(uiState = success, displayState = CoachDisplayState(), onRefresh = {}, olderInsights = older, onNavigateBack = {})
        }
    }

    @Test fun comic() = shot("coachscreen", PqSkin.Comic, content())
    @Test fun pixel() = shot("coachscreen", PqSkin.Pixel, content())
    @Test fun signIn() = shot("coachsignin", PqSkin.Comic) {
        PixelQuestTheme { AiInsightScreenContent(uiState = AiInsightUiState.SignInRequired(), onRefresh = {}, onNavigateBack = {}, onNavigateToAccount = {}) }
    }
}
