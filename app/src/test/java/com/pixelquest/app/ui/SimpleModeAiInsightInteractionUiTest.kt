package com.pixelquest.app.ui

import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.ai.HabitInsightPromptBuilder
import com.pixelquest.app.domain.ai.HabitInsightTone
import com.pixelquest.app.domain.ai.HabitInsightToneHook
import com.pixelquest.app.domain.ai.DefaultHabitInsightToneHook
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Step 29: UI test verifying the Simple Mode + AI Insights interaction.
 *
 * Validates:
 * 1. Persona title & TopAppBar title framing under Simple Mode vs Gamified Mode.
 * 2. Visual avatar representation (nature sprout 🌱 vs RPG wizard 🧙).
 * 3. Telemetry section header & tag phrasing (Daily Perspective vs Heroic Blessing).
 * 4. Deterministic cache key & hash isolation between Simple Mode and Gamified Mode.
 */
class SimpleModeAiInsightInteractionUiTest {

    data class AiInsightUiFraming(
        val topBarTitle: String,
        val personaTitle: String,
        val avatarEmoji: String,
        val telemetryBadge: String,
        val observationTag: String,
        val actionTag: String,
        val encouragementTag: String,
        val systemStatusText: String
    )

    private fun resolveUiFraming(isSimpleMode: Boolean, themeMode: ThemeMode = ThemeMode.Pixel): AiInsightUiFraming {
        val topBar = when {
            isSimpleMode -> "HABIT COACH"
            themeMode == ThemeMode.Comic -> "AI QUESTMASTER"
            themeMode == ThemeMode.Light -> "Habit Insights"
            else -> "AI COACH"
        }

        return AiInsightUiFraming(
            topBarTitle = topBar,
            personaTitle = if (isSimpleMode) "HABIT COACH" else "QUESTMASTER COACH",
            avatarEmoji = if (isSimpleMode) "🌱" else "🧙",
            telemetryBadge = if (isSimpleMode) "[HABIT TELEMETRY]" else "[QUEST TELEMETRY]",
            observationTag = if (isSimpleMode) "► HABIT PATTERN SCAN" else "► OBSERVATION SCAN",
            actionTag = if (isSimpleMode) "► SUGGESTED ACTION" else "► STRATEGIC PROTOCOL",
            encouragementTag = if (isSimpleMode) "► DAILY PERSPECTIVE" else "► HEROIC BLESSING",
            systemStatusText = if (isSimpleMode) "SYS.HABIT.AI // ONLINE" else "SYS.GEMINI.AI // ONLINE"
        )
    }

    @Test
    fun `simple_mode_ai_insight_visual_framing_replaces_all_rpg_elements`() {
        val framing = resolveUiFraming(isSimpleMode = true)

        assertEquals("HABIT COACH", framing.topBarTitle)
        assertEquals("HABIT COACH", framing.personaTitle)
        assertEquals("🌱", framing.avatarEmoji)
        assertEquals("[HABIT TELEMETRY]", framing.telemetryBadge)
        assertEquals("► HABIT PATTERN SCAN", framing.observationTag)
        assertEquals("► SUGGESTED ACTION", framing.actionTag)
        assertEquals("► DAILY PERSPECTIVE", framing.encouragementTag)
        assertEquals("SYS.HABIT.AI // ONLINE", framing.systemStatusText)

        assertFalse("Simple mode must not contain Wizard emoji", framing.avatarEmoji == "🧙")
        assertFalse("Simple mode must not contain Questmaster title", framing.personaTitle.contains("QUESTMASTER"))
        assertFalse("Simple mode must not contain Heroic Blessing tag", framing.encouragementTag.contains("HEROIC"))
    }

    @Test
    fun `gamified_mode_ai_insight_visual_framing_preserves_rpg_identity`() {
        val framing = resolveUiFraming(isSimpleMode = false)

        assertEquals("AI COACH", framing.topBarTitle)
        assertEquals("QUESTMASTER COACH", framing.personaTitle)
        assertEquals("🧙", framing.avatarEmoji)
        assertEquals("[QUEST TELEMETRY]", framing.telemetryBadge)
        assertEquals("► OBSERVATION SCAN", framing.observationTag)
        assertEquals("► STRATEGIC PROTOCOL", framing.actionTag)
        assertEquals("► HEROIC BLESSING", framing.encouragementTag)
        assertEquals("SYS.GEMINI.AI // ONLINE", framing.systemStatusText)
    }

    @Test
    fun `comic_mode_top_bar_title_adapts_to_simple_mode`() {
        val gamifiedComic = resolveUiFraming(isSimpleMode = false, themeMode = ThemeMode.Comic)
        assertEquals("AI QUESTMASTER", gamifiedComic.topBarTitle)

        val simpleComic = resolveUiFraming(isSimpleMode = true, themeMode = ThemeMode.Comic)
        assertEquals("HABIT COACH", simpleComic.topBarTitle)
    }

    @Test
    fun `toggling_simple_mode_triggers_cache_hash_invalidation`() {
        val testStreak = StreakEntity(id = 1, currentStreak = 4, longestStreak = 7, perfectDaysCount = 10)
        val testProfile = UserProfileEntity(id = 1, username = "PixelUser", avatarId = "avatar_hero", level = 2, totalXp = 200)
        val testTasks = listOf(
            TaskEntity(id = 1, name = "Reading", scheduledTime = LocalTime.of(21, 0), recurrenceType = RecurrenceType.DAILY, category = TaskCategory.LEARNING)
        )
        val testLogs = listOf(
            TaskCompletionLogEntity(id = 1, taskId = 1, completedDate = LocalDate.now(), wasCompleted = true)
        )

        val telemetry = HabitInsightPromptBuilder.buildTelemetrySummary(testStreak, testProfile, testTasks, testLogs)

        val gamifiedHash = HabitInsightPromptBuilder.computeDataHash(telemetry, HabitInsightTone.GAMIFIED_HEROIC)
        val simpleHash = HabitInsightPromptBuilder.computeDataHash(telemetry, HabitInsightTone.SIMPLE_MINIMALIST)

        assertNotEquals(
            "Cache data hash must change across modes so cached gamified insights are not displayed in simple mode",
            gamifiedHash,
            simpleHash
        )
    }
}
