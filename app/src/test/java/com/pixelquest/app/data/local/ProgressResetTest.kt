package com.pixelquest.app.data.local

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.InsightCacheEntity
import com.pixelquest.app.data.local.entity.LevelHistoryEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.LevelUpSignalManager
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ProgressResetTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: AppDatabase
    private lateinit var levelUps: LevelUpSignalManager

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        levelUps = LevelUpSignalManager(context)
    }

    @After
    fun tearDown() = db.close()

    private fun seedAPlayedGame() = runBlocking {
        val day = LocalDate.of(2026, 9, 1)
        val taskId = db.taskDao().insertTask(
            TaskEntity(
                name = "Run", description = "", scheduledDay = day, scheduledTime = LocalTime.of(7, 0),
                recurrenceType = RecurrenceType.DAILY, category = TaskCategory.FITNESS
            )
        )
        db.taskCompletionLogDao().insertLog(TaskCompletionLogEntity(taskId = taskId, completedDate = day, wasCompleted = true, pointsAwarded = 50))
        db.levelHistoryDao().insertLevelHistory(LevelHistoryEntity(level = 2, achievedDate = 0, difficultyAtTimeOfLevelUp = "HARD"))
        db.insightCacheDao().insertInsight(
            InsightCacheEntity(summary = "s", suggestion = "s", encouragement = "e", highlightCategory = null, specificTaskCallout = null, dataHash = "h", generatedAt = 1)
        )
        db.streakDao().insertStreak(StreakEntity(currentStreak = 9, longestStreak = 12, lastCompletedDate = day, perfectDaysCount = 30))
        db.userProfileDao().insertProfile(
            UserProfileEntity(
                username = "Aly", avatarId = "avatar_mage", level = 4, totalXp = 2400, perfectDaysTowardNextLevel = 3,
                supabaseUserId = "user-1", leaderboardOptIn = true, leaderboardDisplayName = "Aly_Q"
            )
        )
        db.difficultySettingsDao().insertSettings(DifficultySettingsEntity(difficultyLevel = DifficultyLevel.HARD, perfectDayThreshold = 0.9f))
        levelUps.setPendingLevelUp(5)
    }

    @Test
    fun resetAll_wipesEveryKindOfProgress() = runBlocking {
        seedAPlayedGame()

        ProgressReset(db, TaskAlarmScheduler(context), levelUps).resetAll()

        assertTrue(db.taskDao().getAllTasks().first().isEmpty())
        assertTrue(db.taskCompletionLogDao().getAllLogs().first().isEmpty())
        assertTrue(db.levelHistoryDao().getAllHistory().first().isEmpty())
        assertNull(db.insightCacheDao().getLatestInsight())
        assertEquals(StreakEntity(), db.streakDao().getCurrentStreak().first())
        assertEquals(DifficultySettingsEntity(), db.difficultySettingsDao().getCurrentDifficulty().first())
        assertNull(levelUps.pendingLevelUp.value)

        val profile = db.userProfileDao().getProfile().first()!!
        assertEquals(1, profile.level)
        assertEquals(0, profile.totalXp)
        assertEquals(0, profile.perfectDaysTowardNextLevel)
        assertEquals("PixelHero", profile.username)
    }

    @Test
    fun resetAll_keepsTheCloudAccountLink() = runBlocking {
        seedAPlayedGame()

        ProgressReset(db, TaskAlarmScheduler(context), levelUps).resetAll()

        val profile = db.userProfileDao().getProfile().first()!!
        assertEquals("user-1", profile.supabaseUserId)
        assertTrue(profile.leaderboardOptIn)
        assertEquals("Aly_Q", profile.leaderboardDisplayName)
    }
}
