package com.pixelquest.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StreakAtRiskTest {

    @Test
    fun aLiveStreak_belowTheThreshold_needsTheMissingQuests() {
        // Medium (70%): 3 quests due, 1 done; 3 of 3 reach 100% but 2 of 3 is only 67%, so 2 more.
        assertEquals(2, StreakAtRisk.questsStillNeeded(currentStreak = 5, dueToday = 3, doneToday = 1, threshold = 0.7f))
        // Easy (50%): 4 due, 1 done; 2 of 4 is enough, so 1 more.
        assertEquals(1, StreakAtRisk.questsStillNeeded(currentStreak = 2, dueToday = 4, doneToday = 1, threshold = 0.5f))
        // Hardest (100%): every quest.
        assertEquals(3, StreakAtRisk.questsStillNeeded(currentStreak = 9, dueToday = 3, doneToday = 0, threshold = 1.0f))
    }

    @Test
    fun noWarning_withoutAStreak_withoutQuests_orOnceSafe() {
        assertNull("Nothing to lose", StreakAtRisk.questsStillNeeded(currentStreak = 0, dueToday = 3, doneToday = 0, threshold = 0.7f))
        assertNull("Nothing due", StreakAtRisk.questsStillNeeded(currentStreak = 4, dueToday = 0, doneToday = 0, threshold = 0.7f))
        assertNull("Already safe", StreakAtRisk.questsStillNeeded(currentStreak = 4, dueToday = 10, doneToday = 7, threshold = 0.7f))
    }

    @Test
    fun theWording_followsSimpleMode_andCounts() {
        val (title, text) = StreakAtRisk.copy(currentStreak = 5, stillNeeded = 2, isSimpleMode = false)
        assertEquals("🔥 Your 5-day streak is at risk!", title)
        assertTrue(text.contains("2 more quests"))

        val (simpleTitle, simpleText) = StreakAtRisk.copy(currentStreak = 1, stillNeeded = 1, isSimpleMode = true)
        assertEquals("Your 1-day streak ends tonight", simpleTitle)
        assertEquals("Complete 1 more task today to keep it going.", simpleText)
        assertFalse("No game words in Simple Mode", (simpleTitle + simpleText).contains("quest", ignoreCase = true))
    }
}
