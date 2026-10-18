package com.pixelquest.app.worker

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.pixelquest.app.data.local.entity.NamazSettingsEntity
import com.pixelquest.app.domain.namaz.DefaultPrayerTimeProvider
import com.pixelquest.app.domain.namaz.NamazReminderPlanner
import com.pixelquest.app.domain.namaz.PrayerTimesCalculator
import com.pixelquest.app.namaz.NamazReminders
import com.pixelquest.app.testing.FakeNamazAlarms
import com.pixelquest.app.testing.FakeNamazReminderOptionsStore
import com.pixelquest.app.testing.FakeNamazRepository
import com.pixelquest.app.testing.FakeSettingsRepository
import com.pixelquest.app.testing.FixedClock
import com.pixelquest.app.testing.LahoreZone
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime

/** The daily safety net arms the prayer reminders again. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class NamazRescheduleWorkerTest {

    @Test
    fun aRun_armsTheNext48Hours() = runBlocking {
        val repository = FakeNamazRepository(NamazSettingsEntity(enabled = true))
        val clock = FixedClock(LocalDateTime.of(2026, 10, 17, 16, 0))
        val provider = DefaultPrayerTimeProvider(repository, PrayerTimesCalculator(), clock) { LahoreZone }
        val alarms = FakeNamazAlarms()
        val reminders = NamazReminders(
            repository, FakeNamazReminderOptionsStore(), NamazReminderPlanner(provider), alarms,
            com.pixelquest.app.testing.FakeNamazNotifier(), FakeSettingsRepository(), clock
        ) { LahoreZone }
        val worker = TestListenableWorkerBuilder<NamazRescheduleWorker>(ApplicationProvider.getApplicationContext())
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(appContext: android.content.Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker =
                    NamazRescheduleWorker(appContext, workerParameters, reminders)
            })
            .build()

        assertEquals(ListenableWorker.Result.success(), worker.doWork())
        assertTrue(alarms.armed.isNotEmpty())
    }
}
