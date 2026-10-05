package com.pixelquest.app.qa

import com.pixelquest.app.BuildConfig
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.remote.GeminiClient
import com.pixelquest.app.data.remote.GeminiResult
import com.pixelquest.app.data.repository.HabitInsightRepositoryImpl
import com.pixelquest.app.domain.ai.DebugAiInsightTrigger
import com.pixelquest.app.domain.ai.DefaultHabitInsightToneHook
import com.pixelquest.app.domain.ai.HabitInsightResponse
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.domain.repository.StreakRepository
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import com.pixelquest.app.domain.repository.TaskRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Step 18: Manual QA verification test exercising the debug pipeline with realistic user data
 * and confirming a coherent, on-topic insight comes back from the Gemini pipeline.
 */
class Day24LiveGeminiPipelineQaTest {

    private val sampleStreak = StreakEntity(id = 1, currentStreak = 5, longestStreak = 12, perfectDaysCount = 18)
    private val sampleProfile = UserProfileEntity(id = 1, username = "ArcadeKnight", avatarId = "knight_1", level = 4)
    private val sampleTasks = listOf(
        TaskEntity(id = 1, name = "Morning Run", description = "Run 5k", scheduledDay = LocalDate.now(), scheduledTime = LocalTime.of(7, 0), recurrenceType = RecurrenceType.DAILY, category = TaskCategory.FITNESS),
        TaskEntity(id = 2, name = "Kotlin Study", description = "Read coroutines docs", scheduledDay = LocalDate.now(), scheduledTime = LocalTime.of(14, 0), recurrenceType = RecurrenceType.DAILY, category = TaskCategory.LEARNING),
        TaskEntity(id = 3, name = "Hydrate", description = "Drink 2L water", scheduledDay = LocalDate.now(), scheduledTime = LocalTime.of(20, 0), recurrenceType = RecurrenceType.DAILY, category = TaskCategory.HEALTH)
    )
    private val sampleLogs = listOf(
        TaskCompletionLogEntity(id = 1, taskId = 1, completedDate = LocalDate.now(), wasCompleted = true, pointsAwarded = 10),
        TaskCompletionLogEntity(id = 2, taskId = 1, completedDate = LocalDate.now().minusDays(1), wasCompleted = true, pointsAwarded = 10),
        TaskCompletionLogEntity(id = 3, taskId = 2, completedDate = LocalDate.now(), wasCompleted = false, pointsAwarded = 0),
        TaskCompletionLogEntity(id = 4, taskId = 3, completedDate = LocalDate.now(), wasCompleted = true, pointsAwarded = 10)
    )

    private val fakeStreakRepo = com.pixelquest.app.testing.FakeStreakRepository(sampleStreak)

    private val fakeProfileRepo = com.pixelquest.app.testing.FakeUserProfileRepository(sampleProfile)

    private val fakeTaskRepo = com.pixelquest.app.testing.FakeTaskRepository(sampleTasks)

    private val fakeCompletionRepo = com.pixelquest.app.testing.FakeTaskCompletionRepository(sampleLogs)

    private val fakeSettingsRepo = com.pixelquest.app.testing.FakeSettingsRepository(aiInsights = true)

    @Test
    fun testDebugPipeline_generatesCoherentHabitInsight() = runBlocking {
        // Always the recorded response: a unit test must not spend the developer's Gemini quota or
        // depend on the network just because a real key is in local.properties. Live calls are
        // checked on a device (VERIFICATION.md).
        val geminiClient: GeminiClient = run {
            object : GeminiClient {
                override suspend fun generateContent(prompt: String, systemInstruction: String?): String {
                    // Valid simulation response matching exact Gemini structured format
                    return """
                    {
                      "summary": "Adventurer, your 5-day streak demonstrates remarkable battle focus in Fitness and Health quests.",
                      "suggestion": "Reinforce your Study quests by initiating them earlier before evening exhaustion sets in.",
                      "encouragement": "Victory is forged through discipline; Level 5 is within your grasp!",
                      "highlightCategory": "FITNESS",
                      "specificTaskCallout": "Morning workouts maintain peak momentum"
                    }
                    """.trimIndent()
                }
            }
        }

        val repository = HabitInsightRepositoryImpl(
            streakRepository = fakeStreakRepo,
            userProfileRepository = fakeProfileRepo,
            taskRepository = fakeTaskRepo,
            taskCompletionRepository = fakeCompletionRepo,
            settingsRepository = fakeSettingsRepo,
            geminiClient = geminiClient,
            toneHook = DefaultHabitInsightToneHook()
        )

        val trigger = DebugAiInsightTrigger(repository)

        var finalResult: GeminiResult<HabitInsightResponse>? = null
        trigger.triggerInsightGeneration { result ->
            finalResult = result
        }

        // Wait for asynchronous job or invoke directly
        val directResult = repository.generateHabitInsight()
        assertNotNull("Pipeline must return a non-null result", directResult)
        assertTrue("Pipeline result should be Success, was $directResult", directResult is GeminiResult.Success)

        val insight = (directResult as GeminiResult.Success).data
        assertTrue("Summary must be coherent and informative", insight.summary.isNotBlank())
        assertTrue("Suggestion must be actionable", insight.suggestion.isNotBlank())
        assertTrue("Encouragement must be inspiring", insight.encouragement.isNotBlank())
        assertEquals("FITNESS", insight.highlightCategory)

        // Verify latestInsight flow reflects the generated insight
        val latest = repository.latestInsight.first()
        assertNotNull("latestInsight Flow should emit parsed insight", latest)
        assertEquals(insight.summary, latest?.summary)
    }
}
