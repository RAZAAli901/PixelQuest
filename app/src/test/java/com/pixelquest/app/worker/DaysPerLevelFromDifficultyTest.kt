package com.pixelquest.app.worker

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.DifficultyMode
import com.pixelquest.app.domain.LevelUpSignalManager
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.testing.FakeDifficultySettingsRepository
import com.pixelquest.app.testing.FakeLevelHistoryRepository
import com.pixelquest.app.testing.FakeStreakRepository
import com.pixelquest.app.testing.FakeTaskCompletionRepository
import com.pixelquest.app.testing.FakeTaskRepository
import com.pixelquest.app.testing.FakeUserProfileRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

/**
 * Players who picked a difficulty before 3/7/14/30 have the old days per level stored with it
 * (Hard: 10). Levelling uses the difficulty's current value, not that stored number.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DaysPerLevelFromDifficultyTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val yesterday = LocalDate.now().minusDays(1)

    /** A Hard player saved under the old values, with [progress] perfect days toward the next level. */
    private fun levelAfterAPerfectDay(progress: Int): Int = runBlocking {
        val tasks = FakeTaskRepository(
            listOf(
                TaskEntity(
                    id = 1, name = "Quest", description = "", scheduledDay = yesterday.minusDays(7),
                    scheduledTime = LocalTime.of(9, 0), recurrenceType = RecurrenceType.DAILY, category = TaskCategory.FITNESS
                )
            )
        )
        val logs = FakeTaskCompletionRepository(
            listOf(TaskCompletionLogEntity(taskId = 1, completedDate = yesterday, wasCompleted = true, pointsAwarded = 50))
        )
        val difficulty = FakeDifficultySettingsRepository(
            DifficultySettingsEntity(id = 1, difficultyLevel = DifficultyLevel.HARD, perfectDayThreshold = 0.9f, daysRequiredPerLevel = 10)
        )
        val streaks = FakeStreakRepository(StreakEntity(id = 1, currentStreak = 3, longestStreak = 3, lastCompletedDate = yesterday.minusDays(1)))
        val profiles = FakeUserProfileRepository(
            UserProfileEntity(username = "Hero", avatarId = "avatar_hero", level = 2, perfectDaysTowardNextLevel = progress)
        )

        TestListenableWorkerBuilder<StreakEvaluationWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters) =
                    StreakEvaluationWorker(
                        appContext, workerParameters, tasks, logs, streaks, difficulty,
                        profiles, FakeLevelHistoryRepository(), LevelUpSignalManager(appContext)
                    )
            })
            .build()
            .doWork()

        profiles.profile.value!!.level
    }

    @Test
    fun theOldStoredTen_noLongerLevelsAHardPlayerUp() {
        assertEquals("10 of 14 days: no level-up yet", 2, levelAfterAPerfectDay(progress = 9))
    }

    @Test
    fun hardLevelsUpAtFourteen() {
        assertEquals(3, levelAfterAPerfectDay(progress = 13))
    }

    @Test
    fun daysPerLevel_comesFromTheDifficulty() {
        val stale = DifficultySettingsEntity(difficultyLevel = DifficultyLevel.HARDEST, daysRequiredPerLevel = 14)
        assertEquals(30, DifficultyMode.daysRequiredPerLevel(stale))
        assertEquals("No setting yet means Medium", 7, DifficultyMode.daysRequiredPerLevel(null))
    }

    @Test
    fun theThreshold_comesFromTheDifficultyToo() {
        // E.g. a hand-edited backup restored a 10% threshold for Hard.
        val edited = DifficultySettingsEntity(difficultyLevel = DifficultyLevel.HARD, perfectDayThreshold = 0.1f)
        assertEquals(0.9f, DifficultyMode.perfectDayThreshold(edited), 0.0001f)
        assertEquals(0.7f, DifficultyMode.perfectDayThreshold(null), 0.0001f)
    }
}
