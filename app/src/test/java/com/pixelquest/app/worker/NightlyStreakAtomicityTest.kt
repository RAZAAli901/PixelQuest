package com.pixelquest.app.worker

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.local.RoomTransactionRunner
import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.repository.LevelHistoryRepositoryImpl
import com.pixelquest.app.data.repository.StreakRepositoryImpl
import com.pixelquest.app.data.repository.UserProfileRepositoryImpl
import com.pixelquest.app.domain.LevelUpSignalManager
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.domain.repository.StreakRepository
import com.pixelquest.app.testing.FakeDifficultySettingsRepository
import com.pixelquest.app.testing.FakeTaskCompletionRepository
import com.pixelquest.app.testing.FakeTaskRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

/**
 * The nightly check saves each day as a whole. It counted perfect days toward the next level as it
 * went but saved the streak (which marks days as evaluated) only at the end, so a run stopped
 * part-way counted the same days again next time. It also wrote the whole profile back, putting
 * back the XP it had read.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class NightlyStreakAtomicityTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    private val yesterday = LocalDate.now().minusDays(1)
    private val start = yesterday.minusDays(3) // three perfect days to catch up on

    private val profiles = UserProfileRepositoryImpl(db.userProfileDao())
    private val streaks = StreakRepositoryImpl(db.streakDao())
    private val tasks = FakeTaskRepository(
        listOf(
            TaskEntity(
                id = 1, name = "Run", description = "", scheduledDay = start.minusDays(10), scheduledTime = LocalTime.of(9, 0),
                recurrenceType = RecurrenceType.DAILY, category = TaskCategory.FITNESS
            )
        )
    )
    private val logs = FakeTaskCompletionRepository(
        (0L..2L).map { TaskCompletionLogEntity(taskId = 1, completedDate = start.plusDays(1 + it), wasCompleted = true, pointsAwarded = 50) }
    )

    @After
    fun tearDown() = db.close()

    private fun seed() = runBlocking {
        db.userProfileDao().insertProfile(UserProfileEntity(username = "Hero", avatarId = "avatar_hero", level = 1, totalXp = 100))
        db.streakDao().insertStreak(StreakEntity(currentStreak = 0, lastCompletedDate = start))
    }

    /** Streak saving fails on the [failOn]th save, as if the process was stopped there. */
    private class StoppingStreaks(private val real: StreakRepository, private val failOn: Int) : StreakRepository by real {
        var saves = 0
        override suspend fun updateStreak(streak: StreakEntity) {
            if (++saves == failOn) throw IllegalStateException("stopped")
            real.updateStreak(streak)
        }
    }

    private fun runNight(streakRepository: StreakRepository) = runBlocking {
        TestListenableWorkerBuilder<StreakEvaluationWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters) =
                    StreakEvaluationWorker(
                        appContext, workerParameters, tasks, logs, streakRepository,
                        FakeDifficultySettingsRepository(DifficultySettingsEntity(perfectDayThreshold = 1.0f, daysRequiredPerLevel = 7)),
                        profiles, LevelHistoryRepositoryImpl(db.levelHistoryDao()), LevelUpSignalManager(appContext),
                        transactionRunner = RoomTransactionRunner(db)
                    )
            })
            .build()
            .doWork()
    }

    @Test
    fun aRunStoppedPartWay_doesNotCountTheSameDaysTwice() = runBlocking {
        seed()

        val stopped = runCatching { runNight(StoppingStreaks(streaks, failOn = 2)) }
        assertTrue("The first run stopped on day two", stopped.isFailure)
        assertEquals("Day one was saved whole", 1, db.userProfileDao().getProfile().first()!!.perfectDaysTowardNextLevel)
        assertEquals(start.plusDays(1), db.streakDao().getCurrentStreak().first()!!.lastCompletedDate)

        runNight(streaks) // the next night catches up on days two and three

        val profile = db.userProfileDao().getProfile().first()!!
        val streak = db.streakDao().getCurrentStreak().first()!!
        assertEquals("Three perfect days, each counted once", 3, profile.perfectDaysTowardNextLevel)
        assertEquals(3, streak.currentStreak)
        assertEquals(yesterday, streak.lastCompletedDate)
    }

    @Test
    fun levelProgress_andXpAddedMeanwhile_bothStay() = runBlocking {
        seed()
        val readBeforeTheCompletion = profiles.getProfile().first()!!

        profiles.addXp(70) // a quest completed while the night's check has the profile in hand
        profiles.setLevelProgress(readBeforeTheCompletion.level + 1, 0)

        val profile = db.userProfileDao().getProfile().first()!!
        assertEquals("The XP isn't put back to what was read", 170, profile.totalXp)
        assertEquals(2, profile.level)
    }
}
