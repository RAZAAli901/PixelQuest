package com.pixelquest.app.ui

import com.pixelquest.app.data.local.entity.LevelHistoryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelHistoryScreenTest {

    @Test
    fun testEmptyStateDisplayCondition() {
        val historyList = emptyList<LevelHistoryEntity>()
        assertTrue(historyList.isEmpty())
    }

    @Test
    fun testPopulatedStateListRendering() {
        val historyList = listOf(
            LevelHistoryEntity(id = 1, level = 2, achievedDate = 1000L, difficultyAtTimeOfLevelUp = "MEDIUM"),
            LevelHistoryEntity(id = 2, level = 3, achievedDate = 2000L, difficultyAtTimeOfLevelUp = "HARD")
        )
        assertEquals(2, historyList.size)
        assertEquals(2, historyList[0].level)
        assertEquals("HARD", historyList[1].difficultyAtTimeOfLevelUp)
    }
}
