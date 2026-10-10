package com.pixelquest.app.ui.screens.today

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.remote.GeminiResult
import com.pixelquest.app.domain.ai.HabitInsightResponse
import com.pixelquest.app.domain.repository.HabitInsightRepository
import com.pixelquest.app.domain.repository.NoOpInsightCacheRepository
import com.pixelquest.app.testing.FakeAiAccess
import com.pixelquest.app.testing.FakeSettingsRepository
import com.pixelquest.app.testing.FakeTaskCompletionRepository
import com.pixelquest.app.ui.screens.insight.AiInsightViewModel
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** The AI Coach's own screen is reachable: Today's insight card links to it (it had no way in since Day 24). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class OpenAiCoachLinkTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun todaysInsight_linksToTheAiCoachScreen() {
        val insight = HabitInsightResponse("Summary", "Suggestion", "Encouragement", generatedAt = 1000L)
        val habits = object : HabitInsightRepository {
            override suspend fun generateHabitInsight(forceRefresh: Boolean): GeminiResult<HabitInsightResponse> = GeminiResult.Success(insight)
            override val latestInsight = MutableStateFlow<HabitInsightResponse?>(insight)
            override suspend fun getRemainingCooldownSeconds(): Long = 0L
        }
        val today = LocalDate.now()
        val logs = FakeTaskCompletionRepository((0L..2L).map {
            TaskCompletionLogEntity(id = it + 1, taskId = 1, completedDate = today.minusDays(it), wasCompleted = true, pointsAwarded = 50)
        })
        val viewModel = AiInsightViewModel(habits, FakeSettingsRepository(aiInsights = true), logs, NoOpInsightCacheRepository(), FakeAiAccess())
        var opened = 0

        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                TodayAiInsightSection(onNavigateToAiInsight = { opened++ }, onNavigateToSettings = {}, viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText("OPEN AI COACH ▶").performClick()
        assertEquals(1, opened)
    }
}
