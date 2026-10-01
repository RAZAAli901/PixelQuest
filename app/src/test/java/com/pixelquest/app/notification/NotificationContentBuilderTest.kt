package com.pixelquest.app.notification

import com.pixelquest.app.domain.model.TaskCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationContentBuilderTest {

    private fun ctx(
        simple: Boolean = false,
        streak: Int = 3,
        done: Int = 1,
        total: Int = 3,
        category: TaskCategory = TaskCategory.LEARNING
    ) = ReminderContext(
        taskName = "Read 15 Pages",
        category = category,
        isSimpleMode = simple,
        currentStreak = streak,
        doneToday = done,
        totalToday = total
    )

    @Test
    fun gamified_titleUsesCategoryGlyph() {
        val copy = NotificationContentBuilder.reminder(ctx())
        assertEquals("📚 Quest Time: Read 15 Pages", copy.title)
    }

    @Test
    fun gamified_bigTextShowsStreakProgressAndRisk() {
        val copy = NotificationContentBuilder.reminder(ctx())
        assertEquals("🔥 3-day streak · 1 of 3 quests done today", copy.text)
        assertTrue(copy.bigText.contains("2 quests left today. Keep your 3-day streak alive!"))
    }

    @Test
    fun simpleMode_hasNoGameLanguage() {
        val copy = NotificationContentBuilder.reminder(ctx(simple = true))
        assertEquals("Time to do: Read 15 Pages", copy.title)
        assertEquals("1 of 3 tasks done today", copy.text)
        listOf("quest", "streak", "🔥", "XP").forEach { word ->
            assertFalse("Simple Mode copy contains '$word': ${copy.bigText}", copy.bigText.contains(word, ignoreCase = true))
        }
    }

    @Test
    fun noStreak_noRiskLine() {
        assertNull(NotificationContentBuilder.streakAtRiskLine(ctx(streak = 0)))
        val copy = NotificationContentBuilder.reminder(ctx(streak = 0))
        assertEquals("1 of 3 quests done today", copy.text)
    }

    @Test
    fun allDone_noRiskLine() {
        assertNull(NotificationContentBuilder.streakAtRiskLine(ctx(done = 3, total = 3)))
    }

    @Test
    fun singleRemaining_usesSingular() {
        assertEquals(
            "1 quest left today. Keep your 3-day streak alive!",
            NotificationContentBuilder.streakAtRiskLine(ctx(done = 2, total = 3))
        )
    }

    @Test
    fun nothingScheduled_fallsBackToPrompt() {
        val copy = NotificationContentBuilder.reminder(ctx(streak = 0, done = 0, total = 0))
        assertEquals(NotificationHelper.getReminderText("Read 15 Pages", isSimpleMode = false), copy.text)
    }

    @Test
    fun missedText_doesNotClaimStreakBroke() {
        listOf(true, false).forEach { simple ->
            val text = NotificationHelper.getMissedTaskText("Go to Gym", simple)
            assertFalse(text.contains("broke", ignoreCase = true))
            assertTrue(text.startsWith("Go to Gym"))
        }
    }
}
