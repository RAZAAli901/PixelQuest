package com.pixelquest.app.worker

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.TaskResultRecorder
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
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
 * A quest completed from its reminder or the full-screen prompt reaches the cloud profile too. Only
 * the Today screen asked for a sync, so the leaderboard showed the old XP until the next in-app
 * completion; and the sync waited in memory, so a receiver's process ending took it with it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CompletionSyncTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val day = LocalDate.of(2026, 10, 8)

    private class CountingSync : SyncScheduler {
        var calls = 0
        override fun scheduleProfileSync(debounceMs: Long) { calls++ }
    }

    private val sync = CountingSync()
    private val recorder = TaskResultRecorder(
        FakeTaskCompletionRepository(),
        FakeUserProfileRepository(),
        FakeStreakRepository(),
        FakeTaskRepository(
            listOf(
                TaskEntity(
                    id = 1, name = "Run", description = "", scheduledDay = day.minusDays(5), scheduledTime = LocalTime.of(9, 0),
                    recurrenceType = RecurrenceType.DAILY, category = TaskCategory.FITNESS
                )
            )
        ),
        sync
    )

    @Test
    fun completingFromAnywhere_asksForASync_onceForTheXp() = runBlocking {
        recorder.recordCompleted(1, day) // the reminder's YES, the prompt, or Today: all come here
        assertEquals(1, sync.calls)

        recorder.recordCompleted(1, day) // already done: no new XP, nothing to send
        recorder.recordNotDone(1, day.plusDays(1)) // a miss earns nothing either
        recorder.recordCompleted(99, day) // a deleted quest
        assertEquals(1, sync.calls)
    }

    @Test
    fun aBurstOfCompletions_leavesOneSyncWaiting_inWorkManager() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setMinimumLoggingLevel(Log.ERROR).setExecutor(SynchronousExecutor()).build()
        )
        val scheduler = SyncSchedulerImpl(context)

        repeat(3) { scheduler.scheduleProfileSync() }

        val infos = WorkManager.getInstance(context).getWorkInfosForUniqueWork(SyncSchedulerImpl.UNIQUE_SYNC_WORK_NAME).get()
        // Enqueued straight away (it survives the process ending), each replacing the one before.
        assertEquals(listOf(WorkInfo.State.ENQUEUED), infos.map { it.state }.filter { it != WorkInfo.State.CANCELLED })
    }

    @Test
    fun anImmediateSync_isEnqueuedAtOnce() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setMinimumLoggingLevel(Log.ERROR).setExecutor(SynchronousExecutor()).build()
        )

        SyncSchedulerImpl(context).scheduleProfileSync(debounceMs = 0L) // "Sync now"

        val infos = WorkManager.getInstance(context).getWorkInfosForUniqueWork(SyncSchedulerImpl.UNIQUE_SYNC_WORK_NAME).get()
        assertEquals(1, infos.size)
    }
}
