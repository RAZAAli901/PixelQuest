package com.pixelquest.app.worker

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * V029: Streak evaluation worker tests covering:
 * - Midnight and RUN_AT delay calculations
 * - Catch-up across skipped days and rest days
 * - Daylight saving time (spring forward and fall back)
 * - Time zone changes
 * - Device clock moved back idempotency
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class StreakEvaluationWorkerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val yesterday = LocalDate.now().minusDays(1)

    private val tasks = FakeTaskRepository()
    private val logs = FakeTaskCompletionRepository()

    // Medium: 70% of yesterday's quests make a perfect day.
    private val difficulty = FakeDifficultySettingsRepository(
        DifficultySettingsEntity(id = 1, difficultyLevel = DifficultyLevel.MEDIUM, perfectDayThreshold = 0.7f, daysRequiredPerLevel = 7),
    )
    private val streaks = FakeStreakRepository(
        StreakEntity(id = 1, currentStreak = 5, longestStreak = 10, lastCompletedDate = yesterday.minusDays(1)),
    )

    private fun questsYesterday(count: Int) = (1L..count).forEach { id ->
        runBlocking {
            tasks.insertTask(
                TaskEntity(
                    id = id,
                    name = "Quest $id",
                    description = "",
                    scheduledDay = yesterday.minusDays(7),
                    scheduledTime = LocalTime.of(9, 0),
                    recurrenceType = RecurrenceType.DAILY,
                    category = TaskCategory.FITNESS,
                ),
            )
        }
    }

    private fun completed(vararg ids: Long) = runBlocking {
        ids.forEach { logs.insertLog(TaskCompletionLogEntity(taskId = it, completedDate = yesterday, wasCompleted = true, pointsAwarded = 50)) }
    }

    private fun runWorker(): ListenableWorker.Result = runBlocking {
        TestListenableWorkerBuilder<StreakEvaluationWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters) =
                    StreakEvaluationWorker(
                        appContext,
                        workerParameters,
                        tasks,
                        logs,
                        streaks,
                        difficulty,
                        FakeUserProfileRepository(),
                        FakeLevelHistoryRepository(),
                        LevelUpSignalManager(appContext),
                    )
            })
            .build()
            .doWork()
    }

    @Test
    fun doWork_allTasksMissed_resetsCurrentStreakToZero() {
        questsYesterday(2)

        assertEquals(ListenableWorker.Result.success(), runWorker())

        val streak = streaks.streak.value!!
        assertEquals(0, streak.currentStreak)
        assertEquals(10, streak.longestStreak)
        assertEquals(yesterday, streak.lastCompletedDate)
    }

    @Test
    fun doWork_exactThreshold_incrementsStreak() {
        questsYesterday(10)
        completed(1, 2, 3, 4, 5, 6, 7) // exactly 70%

        runWorker()

        assertEquals(6, streaks.streak.value!!.currentStreak)
    }

    @Test
    fun doWork_secondCallSameDate_isIdempotent() {
        questsYesterday(1)
        completed(1)

        runWorker()
        runWorker()

        assertEquals(6, streaks.streak.value!!.currentStreak)
    }

    @Test
    fun delayUntilNextRun_midnightAndRollOverCalculations() {
        val zone = ZoneId.of("UTC")
        val date = LocalDate.of(2026, 10, 27)

        // 1. Exactly at midnight (00:00:00) -> delay to 02:05:00 is exactly 2h 5m (125 minutes)
        val midnight = ZonedDateTime.of(date, LocalTime.of(0, 0), zone)
        val delayFromMidnight = StreakEvaluationWorker.delayUntilNextRun(midnight)
        assertEquals(Duration.ofMinutes(125), delayFromMidnight)

        // 2. At 01:00:00 (before 02:05) -> delay is 1h 5m (65 minutes)
        val atOneAm = ZonedDateTime.of(date, LocalTime.of(1, 0), zone)
        assertEquals(Duration.ofMinutes(65), StreakEvaluationWorker.delayUntilNextRun(atOneAm))

        // 3. At 02:04:00 (1 minute before RUN_AT) -> delay is 1 minute
        val justBeforeRunAt = ZonedDateTime.of(date, LocalTime.of(2, 4), zone)
        assertEquals(Duration.ofMinutes(1), StreakEvaluationWorker.delayUntilNextRun(justBeforeRunAt))

        // 4. Exactly at RUN_AT (02:05:00) -> targets tomorrow at 02:05 (24 hours)
        val exactRunAt = ZonedDateTime.of(date, LocalTime.of(2, 5), zone)
        assertEquals(Duration.ofHours(24), StreakEvaluationWorker.delayUntilNextRun(exactRunAt))

        // 5. At 02:06:00 (just after RUN_AT) -> targets tomorrow at 02:05 (23h 59m)
        val justAfterRunAt = ZonedDateTime.of(date, LocalTime.of(2, 6), zone)
        assertEquals(Duration.ofMinutes(23 * 60 + 59), StreakEvaluationWorker.delayUntilNextRun(justAfterRunAt))

        // 6. At 23:59:00 -> delay to tomorrow 02:05 is 2h 6m (126 minutes)
        val endOfDay = ZonedDateTime.of(date, LocalTime.of(23, 59), zone)
        assertEquals(Duration.ofMinutes(126), StreakEvaluationWorker.delayUntilNextRun(endOfDay))
    }

    @Test
    fun delayUntilNextRun_daylightSavingTime_springForwardAndFallBack() {
        val newYork = ZoneId.of("America/New_York")

        // 1. Spring Forward (second Sunday in March 2026: March 8)
        // Night before transition at 22:00 on March 7
        val eveningBeforeSpring = ZonedDateTime.of(
            LocalDateTime.of(2026, 3, 7, 22, 0),
            newYork,
        )
        val delaySpring = StreakEvaluationWorker.delayUntilNextRun(eveningBeforeSpring)
        assertTrue("Delay across spring forward must be positive", delaySpring.toMinutes() > 0)
        // Next run should target 2026-03-08T02:05 or adjusted wall-clock time
        val targetSpring = eveningBeforeSpring.plus(delaySpring)
        assertEquals(LocalDate.of(2026, 3, 8), targetSpring.toLocalDate())

        // 2. Fall Back (first Sunday in November 2026: November 1)
        // Evening before transition at 22:00 on October 31
        val eveningBeforeFall = ZonedDateTime.of(
            LocalDateTime.of(2026, 10, 31, 22, 0),
            newYork,
        )
        val delayFall = StreakEvaluationWorker.delayUntilNextRun(eveningBeforeFall)
        assertTrue("Delay across fall back must be positive", delayFall.toMinutes() > 0)
        val targetFall = eveningBeforeFall.plus(delayFall)
        assertEquals(LocalDate.of(2026, 11, 1), targetFall.toLocalDate())
        assertEquals(StreakEvaluationWorker.RUN_AT, targetFall.toLocalTime())
    }

    @Test
    fun delayUntilNextRun_multipleTimeZones_alwaysTargetsLocalTwoZeroFive() {
        val testZones = listOf(
            ZoneId.of("Europe/London"),
            ZoneId.of("Asia/Tokyo"),
            ZoneId.of("America/Los_Angeles"),
            ZoneId.of("UTC"),
        )
        val baseDate = LocalDate.of(2026, 10, 27)
        val noon = LocalTime.of(12, 0)

        for (zone in testZones) {
            val nowInZone = ZonedDateTime.of(baseDate, noon, zone)
            val delay = StreakEvaluationWorker.delayUntilNextRun(nowInZone)
            val nextRunTime = nowInZone.plus(delay)

            assertEquals("Target date in $zone must be tomorrow", baseDate.plusDays(1), nextRunTime.toLocalDate())
            assertEquals("Target time in $zone must be 02:05", StreakEvaluationWorker.RUN_AT, nextRunTime.toLocalTime())
            assertEquals("Target zone must match requested zone", zone, nextRunTime.zone)
        }
    }

    @Test
    fun doWork_clockMovedBack_preservesStreakAndDoesNotRollback() {
        // Initial streak lastCompletedDate is already ahead of yesterday (simulating clock moved back)
        val futureDate = yesterday.plusDays(1)
        runBlocking {
            streaks.updateStreak(
                StreakEntity(
                    id = 1,
                    currentStreak = 5,
                    longestStreak = 10,
                    lastCompletedDate = futureDate,
                    perfectDaysCount = 4,
                ),
            )
        }

        // Run worker while yesterday is before lastCompletedDate
        val result = runWorker()
        assertEquals(ListenableWorker.Result.success(), result)

        val finalStreak = streaks.streak.value!!
        assertEquals("Current streak must be preserved when clock is moved back", 5, finalStreak.currentStreak)
        assertEquals("Longest streak must be preserved", 10, finalStreak.longestStreak)
        assertEquals("lastCompletedDate must not regress", futureDate, finalStreak.lastCompletedDate)
        assertEquals("perfectDaysCount must be preserved", 4, finalStreak.perfectDaysCount)
    }

    @Test
    fun doWork_skippedDays_evaluatesEachDaySequentially() = runBlocking {
        // Gap of 3 days: last evaluated 3 days before yesterday
        val dayMinus2 = yesterday.minusDays(2)
        val dayMinus1 = yesterday.minusDays(1)
        val day0 = yesterday

        streaks.updateStreak(
            StreakEntity(
                id = 1,
                currentStreak = 5,
                longestStreak = 10,
                lastCompletedDate = yesterday.minusDays(3),
                perfectDaysCount = 2,
            ),
        )

        // Day -2: has 1 task scheduled, completed (PERFECT -> streak 6, perfectDaysCount 3)
        tasks.insertTask(
            TaskEntity(
                id = 201L,
                name = "Catch-up Quest 1",
                description = "",
                scheduledDay = dayMinus2,
                scheduledTime = LocalTime.of(10, 0),
                recurrenceType = RecurrenceType.DAILY,
                category = TaskCategory.FITNESS,
            ),
        )
        logs.insertLog(
            TaskCompletionLogEntity(
                taskId = 201L,
                completedDate = dayMinus2,
                wasCompleted = true,
                pointsAwarded = 50,
            ),
        )

        // Day -1: has 1 task scheduled, missed (MISSED -> streak resets to 0)
        tasks.insertTask(
            TaskEntity(
                id = 202L,
                name = "Catch-up Quest 2",
                description = "",
                scheduledDay = dayMinus1,
                scheduledTime = LocalTime.of(10, 0),
                recurrenceType = RecurrenceType.DAILY,
                category = TaskCategory.HEALTH,
            ),
        )
        // No log inserted for 202L on dayMinus1 -> missed!

        // Day 0 (yesterday): No tasks scheduled -> REST (streak carries over at 0)

        val result = runWorker()
        assertEquals(ListenableWorker.Result.success(), result)

        val finalStreak = streaks.streak.value!!
        assertEquals("Streak should reset to 0 after missed day in catch-up", 0, finalStreak.currentStreak)
        assertEquals("Longest streak is preserved", 10, finalStreak.longestStreak)
        assertEquals("lastCompletedDate must be yesterday after catch-up", day0, finalStreak.lastCompletedDate)
        assertEquals("perfectDaysCount should count the perfect day on dayMinus2", 3, finalStreak.perfectDaysCount)
    }

    @Test
    fun doWork_skippedDays_withRestDay_carriesStreakOver() = runBlocking {
        val dayMinus1 = yesterday.minusDays(1)
        val day0 = yesterday

        streaks.updateStreak(
            StreakEntity(
                id = 1,
                currentStreak = 5,
                longestStreak = 10,
                lastCompletedDate = yesterday.minusDays(2),
                perfectDaysCount = 2,
            ),
        )

        // Day -1: 0 tasks scheduled -> REST day, streak carries over as 5
        // Day 0: 1 task scheduled, completed -> PERFECT day, streak becomes 6
        tasks.insertTask(
            TaskEntity(
                id = 301L,
                name = "Yesterday Quest",
                description = "",
                scheduledDay = day0,
                scheduledTime = LocalTime.of(10, 0),
                recurrenceType = RecurrenceType.DAILY,
                category = TaskCategory.LEARNING,
            ),
        )
        logs.insertLog(
            TaskCompletionLogEntity(
                taskId = 301L,
                completedDate = day0,
                wasCompleted = true,
                pointsAwarded = 50,
            ),
        )

        val result = runWorker()
        assertEquals(ListenableWorker.Result.success(), result)

        val finalStreak = streaks.streak.value!!
        assertEquals("Streak should carry across rest day and increment to 6", 6, finalStreak.currentStreak)
        assertEquals(day0, finalStreak.lastCompletedDate)
        assertEquals(3, finalStreak.perfectDaysCount)
    }
}
