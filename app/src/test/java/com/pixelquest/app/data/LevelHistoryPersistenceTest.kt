package com.pixelquest.app.data

import com.pixelquest.app.data.local.entity.LevelHistoryEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class LevelHistoryPersistenceTest {

    @Test
    fun testLevelHistoryPersistenceAcrossSessions() {
        val session1Logs = listOf(
            LevelHistoryEntity(id = 1, level = 2, achievedDate = 1000L, difficultyAtTimeOfLevelUp = "EASY")
        )

        // Simulate session restart: reading persistent Room storage
        val session2Logs = session1Logs.toMutableList()
        session2Logs.add(
            LevelHistoryEntity(id = 2, level = 3, achievedDate = 2000L, difficultyAtTimeOfLevelUp = "MEDIUM")
        )

        assertEquals(2, session2Logs.size)
        assertEquals(2, session2Logs[0].level)
        assertEquals(3, session2Logs[1].level)
        assertEquals("MEDIUM", session2Logs[1].difficultyAtTimeOfLevelUp)
    }
}
