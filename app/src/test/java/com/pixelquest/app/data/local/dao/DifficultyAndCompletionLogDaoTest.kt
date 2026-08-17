package com.pixelquest.app.data.local.dao

import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.domain.model.DifficultyLevel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class DifficultyAndCompletionLogDaoTest : BaseDaoTest() {

    private lateinit var difficultyDao: DifficultySettingsDao
    private lateinit var completionLogDao: TaskCompletionLogDao

    @Before
    override fun createDb() {
        super.createDb()
        difficultyDao = database.difficultySettingsDao()
        completionLogDao = database.taskCompletionLogDao()
    }

    @Test
    fun difficultySettingsInsertAndUpdate() = runBlocking {
        val initial = DifficultySettingsEntity(id = 1, difficultyLevel = DifficultyLevel.MEDIUM, perfectDayThreshold = 0.8f, daysRequiredPerLevel = 7)
        difficultyDao.insertSettings(initial)

        val fetched1 = difficultyDao.getCurrentDifficulty().first()
        assertNotNull(fetched1)
        assertEquals(DifficultyLevel.MEDIUM, fetched1?.difficultyLevel)

        val updated = initial.copy(difficultyLevel = DifficultyLevel.HARD, perfectDayThreshold = 0.9f)
        difficultyDao.updateSettings(updated)

        val fetched2 = difficultyDao.getCurrentDifficulty().first()
        assertEquals(DifficultyLevel.HARD, fetched2?.difficultyLevel)
        assertEquals(0.9f, fetched2?.perfectDayThreshold ?: 0f, 0.01f)
    }

    @Test
    fun taskCompletionLogDateRangeQueryCorrectness() = runBlocking {
        val date1 = LocalDate.of(2026, 8, 1)
        val date2 = LocalDate.of(2026, 8, 5)
        val date3 = LocalDate.of(2026, 8, 10)

        val log1 = TaskCompletionLogEntity(id = 1, taskId = 100, completedDate = date1, wasCompleted = true, pointsAwarded = 50)
        val log2 = TaskCompletionLogEntity(id = 2, taskId = 101, completedDate = date2, wasCompleted = true, pointsAwarded = 50)
        val log3 = TaskCompletionLogEntity(id = 3, taskId = 102, completedDate = date3, wasCompleted = false, pointsAwarded = 0)

        completionLogDao.insertLog(log1)
        completionLogDao.insertLog(log2)
        completionLogDao.insertLog(log3)

        val rangeLogs = completionLogDao.getCompletionHistory(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 6)).first()
        assertEquals(2, rangeLogs.size)
        assertEquals(date1, rangeLogs[0].completedDate)
        assertEquals(date2, rangeLogs[1].completedDate)
    }
}
