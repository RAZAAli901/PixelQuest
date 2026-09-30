package com.pixelquest.app.domain.ai

import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 30: Unit test verifying that prompts and system instructions dynamically
 * adapt between Gamified Heroic (arcade RPG) and Simple Minimalist (calm habit coach)
 * based on the user's Simple Mode state.
 */
class SimpleModeTonePromptTest {

    private val toneHook = DefaultHabitInsightToneHook()

    private val sampleTelemetry = HabitTelemetrySummary(
        currentStreak = 6,
        longestStreak = 12,
        perfectDaysCount = 15,
        playerLevel = 4,
        categoryRatios = mapOf(
            "FITNESS" to CategoryMetric(5, 4),
            "STUDY" to CategoryMetric(4, 3)
        ),
        recent7DaysCompletionRate = 0.85f,
        recent7DaysMissedCount = 1
    )

    @Test
    fun prompt_whenSimpleModeActive_requestsMinimalistCoaching() {
        val prompt = HabitInsightPromptBuilder.buildPrompt(
            telemetry = sampleTelemetry,
            toneHook = toneHook,
            isSimpleModeEnabled = true
        )

        // Verifies supportive habit coaching directives
        assertTrue("Prompt must ask for calm habit coaching", prompt.contains("calm habit coaching"))
        assertTrue("Prompt must request omitting gaming jargon", prompt.contains("Omit all gaming, quest, battle, or fantasy references"))

        // Verifies RPG questmaster text is absent
        assertFalse("Prompt must not mention questmaster in simple mode", prompt.contains("questmaster debrief"))
        assertFalse("Prompt must not mention heroic consistency in simple mode", prompt.contains("heroic consistency"))
    }

    @Test
    fun prompt_whenSimpleModeInactive_requestsGamifiedQuestmasterDebrief() {
        val prompt = HabitInsightPromptBuilder.buildPrompt(
            telemetry = sampleTelemetry,
            toneHook = toneHook,
            isSimpleModeEnabled = false
        )

        // Verifies gamified RPG directives
        assertTrue("Prompt must ask for questmaster debrief", prompt.contains("questmaster debrief"))
        assertTrue("Prompt must ask to celebrate heroic consistency", prompt.contains("heroic consistency"))

        // Verifies simple mode omission directive is absent
        assertFalse("Prompt must not omit gaming metaphors when simple mode is inactive", prompt.contains("Omit all gaming"))
    }

    @Test
    fun systemInstruction_adaptsBasedOnSimpleMode() {
        val simpleSystem = HabitInsightPromptBuilder.buildSystemInstruction(toneHook, isSimpleModeEnabled = true)
        val heroicSystem = HabitInsightPromptBuilder.buildSystemInstruction(toneHook, isSimpleModeEnabled = false)

        // Simple mode assertions
        assertTrue("System must set coach persona", simpleSystem.contains("expert, empathetic habit coach"))
        assertTrue("System must forbid gaming jargon", simpleSystem.contains("Do NOT use gaming metaphors"))
        assertFalse("System must not mention Questmaster", simpleSystem.contains("Questmaster"))

        // Heroic mode assertions
        assertTrue("System must set Questmaster persona", heroicSystem.contains("Questmaster of PixelQuest"))
        assertTrue("System must reference battle resilience", heroicSystem.contains("battle resilience"))
        assertTrue("System must reference hero momentum", heroicSystem.contains("hero momentum"))
    }
}
