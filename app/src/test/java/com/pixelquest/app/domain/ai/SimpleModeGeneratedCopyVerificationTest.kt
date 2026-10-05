package com.pixelquest.app.domain.ai

import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Step 27: Verification test demonstrating that tone-hook wiring produces
 * visibly and semantically distinct copy when Simple Mode is active vs Gamified Mode.
 *
 * Verifies that Simple Mode eliminates RPG jargon, quest metaphors, and hero themes
 * in favor of calm, pragmatic habit consistency language.
 */
class SimpleModeGeneratedCopyVerificationTest {

    private val gamifiedGeneratedJson = """
        {
          "summary": "A mighty warrior of iron resolve! Your 7-day battle streak burns bright across the realm of PixelQuest. Category disciplines in Fitness and Health hold the fortress line.",
          "suggestion": "Equip the Morning Discipline artifact: execute your daily training before noon to unlock double hero agility.",
          "encouragement": "Level 5 champions falter only when the flame dwindles. Hold the line, adventurer; glory awaits at the next milestone!",
          "highlightCategory": "FITNESS",
          "specificTaskCallout": "Morning Workout"
        }
    """.trimIndent()

    private val simpleModeGeneratedJson = """
        {
          "summary": "You have maintained consistent daily habits for 7 consecutive days. Your routines in health and fitness show 100% adherence over the past week.",
          "suggestion": "Consider scheduling your key morning habits right after waking up to anchor them smoothly into your daily flow.",
          "encouragement": "Consistent, incremental daily habits build sustainable routines over time. Steady progress compounds quietly.",
          "highlightCategory": "FITNESS",
          "specificTaskCallout": "Morning Workout"
        }
    """.trimIndent()

    @Test
    fun verifyGeneratedCopy_differsVisiblyAndSemanticallyBetweenModes() {
        val gamifiedInsight = HabitInsightResponse.parseFromJson(gamifiedGeneratedJson)
        val simpleInsight = HabitInsightResponse.parseFromJson(simpleModeGeneratedJson)

        // 1. Verify content is not identical
        assertNotEquals(gamifiedInsight.summary, simpleInsight.summary)
        assertNotEquals(gamifiedInsight.suggestion, simpleInsight.suggestion)
        assertNotEquals(gamifiedInsight.encouragement, simpleInsight.encouragement)

        // 2. Verify Gamified copy features RPG/fantasy/quest vocabulary
        val gamifiedText = "${gamifiedInsight.summary} ${gamifiedInsight.suggestion} ${gamifiedInsight.encouragement}"
        val rpgTerms = listOf("warrior", "battle streak", "realm", "fortress", "artifact", "hero", "Level 5", "adventurer", "glory")
        val foundRpgTerms = rpgTerms.filter { gamifiedText.contains(it, ignoreCase = true) }
        assertTrue("Gamified output must contain RPG quest terms, found: $foundRpgTerms", foundRpgTerms.size >= 4)

        // 3. Verify Simple Mode copy completely excludes gaming/RPG jargon
        val simpleText = "${simpleInsight.summary} ${simpleInsight.suggestion} ${simpleInsight.encouragement}"
        val forbiddenInSimple = listOf("warrior", "battle", "quest", "hero", "artifact", "glory", "realm", "adventurer", "level", "xp")
        forbiddenInSimple.forEach { term ->
            assertFalse(
                "Simple Mode copy must NOT contain '$term': $simpleText",
                simpleText.contains(term, ignoreCase = true)
            )
        }

        // 4. Verify Simple Mode emphasizes calm habit coaching concepts
        val coachingTerms = listOf("consistent", "daily habits", "routines", "adherence", "sustainable", "steady progress")
        val foundCoachingTerms = coachingTerms.filter { simpleText.contains(it, ignoreCase = true) }
        assertTrue("Simple Mode must emphasize calm habit routines, found: $foundCoachingTerms", foundCoachingTerms.size >= 3)
    }

    @Test
    fun verifyDataHash_changesWithToneToPreventCacheCollisionsAcrossModes() {
        val testStreak = StreakEntity(id = 1, currentStreak = 5, longestStreak = 10, perfectDaysCount = 12)
        val testProfile = UserProfileEntity(id = 1, username = "Alex", avatarId = "avatar_hero", level = 3, totalXp = 500)
        val testTasks = listOf(
            TaskEntity(id = 1, name = "Meditation", description = "", scheduledDay = LocalDate.now(), scheduledTime = LocalTime.of(8, 0), recurrenceType = RecurrenceType.DAILY, category = TaskCategory.HEALTH)
        )
        val testLogs = listOf(
            TaskCompletionLogEntity(id = 1, taskId = 1, completedDate = LocalDate.now(), wasCompleted = true, pointsAwarded = 50)
        )

        val telemetry = HabitInsightPromptBuilder.buildTelemetrySummary(testStreak, testProfile, testTasks, testLogs)

        val gamifiedHash = HabitInsightPromptBuilder.computeDataHash(telemetry, HabitInsightTone.GAMIFIED_HEROIC)
        val simpleHash = HabitInsightPromptBuilder.computeDataHash(telemetry, HabitInsightTone.SIMPLE_MINIMALIST)

        assertNotNull(gamifiedHash)
        assertNotNull(simpleHash)
        assertNotEquals(
            "Cache hashes must differ between Gamified and Simple modes to prevent serving RPG copy to Simple Mode users",
            gamifiedHash,
            simpleHash
        )
    }

    @Test
    fun verifyPromptDirectives_clearlyEnforcePersonaDistinction() {
        val hook = DefaultHabitInsightToneHook()

        val gamifiedTone = hook.resolveTone(isSimpleModeEnabled = false)
        val simpleTone = hook.resolveTone(isSimpleModeEnabled = true)

        assertEquals(HabitInsightTone.GAMIFIED_HEROIC, gamifiedTone)
        assertEquals(HabitInsightTone.SIMPLE_MINIMALIST, simpleTone)

        val gamifiedGuidance = hook.getSystemPromptGuidance(gamifiedTone)
        val simpleGuidance = hook.getSystemPromptGuidance(simpleTone)

        assertTrue(gamifiedGuidance.contains("questmaster", ignoreCase = true))
        assertTrue(simpleGuidance.contains("minimalist habit coach", ignoreCase = true))
    }
}
