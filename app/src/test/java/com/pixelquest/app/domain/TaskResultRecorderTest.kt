package com.pixelquest.app.domain

import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.repository.StreakRepository
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TaskResultRecorderTest {

    /** In-memory logs that, like the real table, allow one log per task per day. */
    private class FakeLogs : TaskCompletionRepository {
        val logs = MutableStateFlow<List<TaskCompletionLogEntity>>(emptyList())
        private var nextId = 1L

        override suspend fun insertLog(log: TaskCompletionLogEntity): Long {
            yield() // let a racing caller interleave, as a real database write would
            if (logs.value.any { it.taskId == log.taskId && it.completedDate == log.completedDate }) return -1
            val stored = log.copy(id = nextId++)
            logs.value = logs.value + stored
            return stored.id
        }
        override suspend fun updateLog(log: TaskCompletionLogEntity) {
            logs.value = logs.value.map { if (it.id == log.id) log else it }
        }
        override suspend fun getLogForTaskOnDate(taskId: Long, date: LocalDate) =
            logs.value.firstOrNull { it.taskId == taskId && it.completedDate == date }
        override fun getLogsForDate(date: LocalDate): Flow<List<TaskCompletionLogEntity>> =
            logs.map { all -> all.filter { it.completedDate == date } }
        override fun getLogsForTask(taskId: Long): Flow<List<TaskCompletionLogEntity>> =
            logs.map { all -> all.filter { it.taskId == taskId } }
        override fun getCompletionHistory(startDate: LocalDate, endDate: LocalDate) =
            logs.map { all -> all.filter { it.completedDate in startDate..endDate } }
        override fun getAllLogs(): Flow<List<TaskCompletionLogEntity>> = logs
    }

    private class FakeProfile : UserProfileRepository {
        val profile = MutableStateFlow<UserProfileEntity?>(UserProfileEntity(username = "Hero", avatarId = "avatar_hero"))
        override fun getProfile(): Flow<UserProfileEntity?> = profile
        override suspend fun insertProfile(profile: UserProfileEntity) { this.profile.value = profile }
        override suspend fun updateProfile(profile: UserProfileEntity) { this.profile.value = profile }
        override suspend fun performLevelUp(): UserProfileEntity? = null
        override suspend fun updateSupabaseUserId(userId: String?) = Unit
        override suspend fun updateLeaderboardSettings(optIn: Boolean, displayName: String?) = Unit
        override suspend fun updateLeaderboardOptIn(optIn: Boolean) = Unit
        override suspend fun clearCloudData() = Unit
    }

    private class FakeStreak(days: Int) : StreakRepository {
        private val streak = MutableStateFlow<StreakEntity?>(StreakEntity(currentStreak = days))
        override fun getCurrentStreak(): Flow<StreakEntity?> = streak
        override suspend fun insertStreak(streak: StreakEntity) { this.streak.value = streak }
        override suspend fun updateStreak(streak: StreakEntity) { this.streak.value = streak }
    }

    private val day = LocalDate.of(2026, 10, 3)
    private val logs = FakeLogs()
    private val profile = FakeProfile()
    private val recorder = TaskResultRecorder(logs, profile, FakeStreak(days = 2))

    private val xp get() = profile.profile.value!!.totalXp

    @Test
    fun completing_awardsStreakXpOnce() = runTest {
        assertEquals(70, recorder.recordCompleted(1, day)) // 50 + 2 streak days x 10
        assertEquals(0, recorder.recordCompleted(1, day))
        assertEquals(70, xp)
        assertEquals(1, logs.logs.value.size)
    }

    @Test
    fun aDoubleTap_racingItself_isCountedOnce() = runTest {
        val awarded = List(3) { async { recorder.recordCompleted(1, day) } }.awaitAll()
        assertEquals(listOf(70, 0, 0), awarded.sortedDescending())
        assertEquals(70, xp)
        assertEquals(1, logs.logs.value.size)
    }

    @Test
    fun aMissedTask_canStillBeCompletedThatDay() = runTest {
        assertTrue(recorder.recordNotDone(1, day))
        assertEquals(70, recorder.recordCompleted(1, day))
        val log = logs.logs.value.single()
        assertTrue(log.wasCompleted)
        assertEquals(70, log.pointsAwarded)
        assertEquals(70, xp)
    }

    @Test
    fun aCompletedTask_isNeverTurnedIntoAMissedOne() = runTest {
        recorder.recordCompleted(1, day)
        assertFalse(recorder.recordNotDone(1, day))
        assertTrue(logs.logs.value.single().wasCompleted)
    }

    @Test
    fun otherTasksAndOtherDays_areSeparate() = runTest {
        recorder.recordCompleted(1, day)
        recorder.recordCompleted(2, day)
        recorder.recordCompleted(1, day.plusDays(1))
        assertEquals(3, logs.logs.value.size)
        assertEquals(210, xp)
    }
}
