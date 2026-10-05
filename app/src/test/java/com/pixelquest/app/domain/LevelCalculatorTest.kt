package com.pixelquest.app.domain

import com.pixelquest.app.domain.model.DifficultyLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Days per level follow DifficultyMode, which the difficulty picker shows players:
 * Easy 3, Medium 7, Hard 14, Hardest 30, as the Day 6 plan set out (Days 1–29 used 5/7/10/14).
 */
class LevelCalculatorTest {

    @Test
    fun testEasyDifficultyLevelUpThreshold() {
        val daysRequired = DifficultyMode.getDaysRequiredPerLevel(DifficultyLevel.EASY)
        assertEquals(3, daysRequired)
        assertFalse(LevelCalculator.shouldLevelUp(0, daysRequired))
        assertFalse(LevelCalculator.shouldLevelUp(2, daysRequired))
        assertTrue(LevelCalculator.shouldLevelUp(3, daysRequired))
        assertTrue(LevelCalculator.shouldLevelUp(4, daysRequired))
    }

    @Test
    fun testMediumDifficultyLevelUpThreshold() {
        val daysRequired = DifficultyMode.getDaysRequiredPerLevel(DifficultyLevel.MEDIUM)
        assertEquals(7, daysRequired)
        assertFalse(LevelCalculator.shouldLevelUp(6, daysRequired))
        assertTrue(LevelCalculator.shouldLevelUp(7, daysRequired))
    }

    @Test
    fun testHardDifficultyLevelUpThreshold() {
        val daysRequired = DifficultyMode.getDaysRequiredPerLevel(DifficultyLevel.HARD)
        assertEquals(14, daysRequired)
        assertFalse(LevelCalculator.shouldLevelUp(13, daysRequired))
        assertTrue(LevelCalculator.shouldLevelUp(14, daysRequired))
    }

    @Test
    fun testHardestDifficultyLevelUpThreshold() {
        val daysRequired = DifficultyMode.getDaysRequiredPerLevel(DifficultyLevel.HARDEST)
        assertEquals(30, daysRequired)
        assertFalse(LevelCalculator.shouldLevelUp(29, daysRequired))
        assertTrue(LevelCalculator.shouldLevelUp(30, daysRequired))
    }

    @Test
    fun testPostLevelUpResetProgress() {
        assertEquals(0, LevelCalculator.getPostLevelUpProgress())
    }

    @Test
    fun testMidProgressDifficultySwitch() {
        val currentProgress = 6 // accumulated under Medium (req 7)
        val easyDaysReq = DifficultyMode.getDaysRequiredPerLevel(DifficultyLevel.EASY) // 3
        val hardDaysReq = DifficultyMode.getDaysRequiredPerLevel(DifficultyLevel.HARD) // 14

        // Switch Medium -> Easy: 6 >= 3 -> Level Up immediately
        assertTrue(LevelCalculator.evaluateLevelProgressOnDifficultySwitch(currentProgress, easyDaysReq))

        // Switch Medium -> Hard: 6 < 14 -> No Level Up yet
        assertFalse(LevelCalculator.evaluateLevelProgressOnDifficultySwitch(currentProgress, hardDaysReq))
    }
}
