package com.pixelquest.app.darood.sync

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.darood.data.DaroodReminderSettings
import com.pixelquest.app.notification.NotificationChannels
import com.pixelquest.app.testing.FakeDaroodServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** The weekly Darood note: only if turned on, kind whatever the week was. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DaroodWeeklyWorkerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val notifications get() = shadowOf(context.getSystemService(NotificationManager::class.java)).allNotifications

    private class Reminder(on: Boolean) : DaroodReminderSettings {
        override val weeklyReminder = MutableStateFlow(on)
        override fun setWeeklyReminder(on: Boolean) {
            weeklyReminder.value = on
        }
    }

    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    // The phone's clock reads 1970-01-01 here: that's its today.
    private val today = LocalDate.of(1970, 1, 1)
    private val server = FakeDaroodServer(me = "aria-id").apply { usernames += mapOf("aria-id" to "aria_k", "bram-id" to "bram") }
    private val repository = DaroodPartyRepositoryImpl(server, db.daroodDao(), { server.me }, now = { 1_000L }, zone = { ZoneId.of("UTC") })

    @Before
    fun setUp() {
        NotificationChannels.createAll(context)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun run(reminder: DaroodReminderSettings) = runBlocking {
        TestListenableWorkerBuilder<DaroodWeeklyWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters) =
                    DaroodWeeklyWorker(appContext, workerParameters, repository, reminder)
            })
            .build()
            .doWork()
    }

    @Test
    fun turnedOn_itCountsTheDaysAcrossTheirParties_onTheDaroodChannel() {
        val friday = server.seedParty("Friday circle", "aria-id")
        val family = server.seedParty("Family", "bram-id", "aria-id")
        server.seedEntry(friday, "aria-id", 300, day = today)
        server.seedEntry(family, "aria-id", 11, day = today)
        server.seedEntry(family, "aria-id", 33, day = today.minusDays(3))
        server.seedEntry(friday, "aria-id", 100, day = today.minusDays(9))
        server.seedEntry(family, "bram-id", 500, day = today.minusDays(1))

        run(Reminder(on = true))

        val posted = notifications.single()
        assertEquals(NotificationChannels.Spec.DAROOD.id, posted.channelId)
        assertEquals("Your Darood this week", posted.extras.getString(NotificationCompat.EXTRA_TITLE))
        // Today (in either party, once) and 3 days back; not 9 back, and not Bram's day.
        assertEquals("You added Darood on 2 of the last 7 days. Every one counts.", posted.extras.getString(NotificationCompat.EXTRA_TEXT))
    }

    @Test
    fun aQuietWeek_getsAKindWord_notAScolding() {
        server.seedParty("Friday circle", "aria-id")
        run(Reminder(on = true))
        val text = notifications.single().extras.getString(NotificationCompat.EXTRA_TEXT)!!
        assertTrue(text, text.startsWith("Your parties would love your Darood this week"))
    }

    @Test
    fun turnedOff_orNoParty_orSignedOut_nothing() {
        server.seedParty("Friday circle", "aria-id")
        run(Reminder(on = false))
        assertEquals(0, notifications.size)

        server.parties.clear()
        server.members.clear()
        run(Reminder(on = true))
        assertEquals(0, notifications.size)

        server.seedParty("Friday circle", "aria-id")
        server.me = null
        run(Reminder(on = true))
        assertEquals(0, notifications.size)
    }

    @Test
    fun itRunsOnFridayEvenings() {
        // Wednesday 21 October 2026, noon: two days and six hours to Friday 18:00.
        assertEquals((2 * 24 + 6) * 60L, DaroodWeeklyWorker.delayUntilNextRun(LocalDateTime.of(2026, 10, 21, 12, 0)))
        // Friday at 17:00: this evening.
        assertEquals(60L, DaroodWeeklyWorker.delayUntilNextRun(LocalDateTime.of(2026, 10, 23, 17, 0)))
        // Friday at 19:00: next week's.
        assertEquals((7 * 24 - 1) * 60L, DaroodWeeklyWorker.delayUntilNextRun(LocalDateTime.of(2026, 10, 23, 19, 0)))
    }
}
