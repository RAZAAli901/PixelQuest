package com.pixelquest.app.domain

import com.pixelquest.app.domain.model.DifficultyLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Days per level follow DifficultyMode, which the difficulty picker shows players:
 * Easy 5, Medium 7, Hard 10, Hardest 14. (The Day 6 notes planned 3/7/14/30; the app has used
 * DifficultyMode's values since Day 4.)
 */
class LevelCalculatorTest {

    @Test
    fun testEasyDifficultyLevelUpThreshold() {
        val daysRequired = DifficultyMode.getDaysRequiredPerLevel(DifficultyLevel.EASY)
        assertFalse(LevelCalculator.shouldLevelUp(0, daysRequired))
        assertFalse(LevelCalculator.shouldLevelUp(4, daysRequired))
        assertTrue(LevelCalculator.shouldLevelUp(5, daysRequired))
        assertTrue(LevelCalculator.shouldLevelUp(6, daysRequired))
    }

    @Test
    fun testMediumDifficultyLevelUpThreshold() {
        val daysRequired = DifficultyMode.getDaysRequiredPerLevel(DifficultyLevel.MEDIUM)
        assertFalse(LevelCalculator.shouldLevelUp(6, daysRequired))
        assertTrue(LevelCalculator.shouldLevelUp(7, daysRequired))
    }

    @Test
    fun testHardDifficultyLevelUpThreshold() {
        val daysRequired = DifficultyMode.getDaysRequiredPerLevel(DifficultyLevel.HARD)
        assertFalse(LevelCalculator.shouldLevelUp(9, daysRequired))
        assertTrue(LevelCalculator.shouldLevelUp(10, daysRequired))
    }

    @Test
    fun testHardestDifficultyLevelUpThreshold() {
        val daysRequired = DifficultyMode.getDaysRequiredPerLevel(DifficultyLevel.HARDEST)
        assertFalse(LevelCalculator.shouldLevelUp(13, daysRequired))
        assertTrue(LevelCalculator.shouldLevelUp(14, daysRequired))
    }

    @Test
    fun testPostLevelUpResetProgress() {
        assertEquals(0, LevelCalculator.getPostLevelUpProgress())
    }

    @Test
    fun testMidProgressDifficultySwitch() {
        val currentProgress = 6 // accumulated under Medium (req 7)
        val easyDaysReq = DifficultyMode.getDaysRequiredPerLevel(DifficultyLevel.EASY) // 5
        val hardDaysReq = DifficultyMode.getDaysRequiredPerLevel(DifficultyLevel.HARD) // 10

        // Switch Medium -> Easy: 6 >= 5 -> Level Up immediately
        assertTrue(LevelCalculator.evaluateLevelProgressOnDifficultySwitch(currentProgress, easyDaysReq))

        // Switch Medium -> Hard: 6 < 10 -> No Level Up yet
        assertFalse(LevelCalculator.evaluateLevelProgressOnDifficultySwitch(currentProgress, hardDaysReq))
    }
}
