package com.pixelquest.app.namaz

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.MainActivity
import com.pixelquest.app.domain.namaz.NamazAlarm
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.testing.LahoreZone
import com.pixelquest.app.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime

/** The prayer reminder as it appears: "Time for Fajr", its time and place, opening the tracker. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class NamazNotifierTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val manager = shadowOf(context.getSystemService(NotificationManager::class.java))
    private val day = LocalDate.of(2026, 10, 17)

    private fun reminder(place: String = "Lahore") = NamazReminder(
        NamazAlarm.Kind.REMINDER, day, Prayer.FAJR, LocalDateTime.of(day, java.time.LocalTime.of(4, 46)).atZone(LahoreZone), place
    )

    private fun shown(): Notification = manager.allNotifications.single()

    @Test
    fun theReminder_saysWhichPrayer_when_andWhere() {
        AndroidNamazNotifier(context).show(reminder())

        val notification = shown()
        assertEquals("Time for Fajr", notification.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("4:46 AM · Lahore", notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString().replace(' ', ' '))
        assertEquals(NamazChannels.REMINDERS, notification.channelId)
        assertEquals(Notification.CATEGORY_REMINDER, notification.category)
    }

    @Test
    fun withoutAPlaceName_itShowsTheTimeAlone() {
        AndroidNamazNotifier(context).show(reminder(place = ""))

        assertEquals("4:46 AM", shown().extras.getCharSequence(Notification.EXTRA_TEXT).toString().replace(' ', ' '))
    }

    @Test
    fun tappingIt_opensTheNamazScreen() {
        AndroidNamazNotifier(context).show(reminder())

        val opened = shadowOf(shown().contentIntent).savedIntent
        assertEquals(MainActivity::class.java.name, opened.component?.className)
        assertEquals(Screen.Namaz.route, opened.getStringExtra(MainActivity.EXTRA_OPEN_ROUTE))
    }

    @Test
    fun aSecondAlarmForTheSamePrayer_replacesTheNotification() {
        val notifier = AndroidNamazNotifier(context)
        notifier.show(reminder())
        notifier.show(reminder().copy(kind = NamazAlarm.Kind.SNOOZE))

        assertEquals(1, manager.allNotifications.size)
    }

    @Test
    fun theButtons_arePrayed_Snooze_andDismiss_eachForThisPrayer() {
        AndroidNamazNotifier(context).show(reminder())

        val actions = shown().actions
        assertEquals(listOf("Prayed", "Snooze 10 min", "Dismiss"), actions.map { it.title.toString() })
        val sent = actions.map { shadowOf(it.actionIntent).savedIntent }
        assertEquals(NamazAction.entries.map { it.action }, sent.map { it.action })
        sent.forEach { intent ->
            assertEquals(NamazActionReceiver::class.java.name, intent.component?.className)
            assertEquals(day, NamazAlarmScheduler.dateFrom(intent))
            assertEquals(Prayer.FAJR, NamazAlarmScheduler.prayerFrom(intent))
        }
    }

    @Test
    fun cancelling_takesTheReminderAway() {
        val notifier = AndroidNamazNotifier(context)
        notifier.show(reminder())

        notifier.cancel(day, Prayer.FAJR)

        assertEquals(0, manager.allNotifications.size)
    }

    @Test
    fun anEarlyReminder_saysHowSoon_andHasNoPrayedButton() {
        AndroidNamazNotifier(context).show(reminder().copy(minutesToStart = 10))

        val notification = shown()
        assertEquals("Fajr in 10 minutes", notification.extras.getString(Notification.EXTRA_TITLE))
        assertEquals(listOf("Snooze 10 min", "Dismiss"), notification.actions.map { it.title.toString() })
    }

    @Test
    fun aNudge_isQuiet_andSaysWhenTheNextPrayerStarts() {
        val maghrib = com.pixelquest.app.domain.namaz.PrayerMoment(
            Prayer.MAGHRIB, day, LocalDateTime.of(day, java.time.LocalTime.of(17, 29)).atZone(LahoreZone)
        )
        AndroidNamazNotifier(context).show(reminder().copy(kind = NamazAlarm.Kind.NUDGE, prayer = Prayer.ASR, next = maghrib))

        val notification = shown()
        assertEquals("Still time for Asr", notification.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("Maghrib starts at 5:29 PM", notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString().replace('\u202F', ' '))
        assertEquals(NamazChannels.NUDGE, notification.channelId)
        assertEquals(listOf("Prayed", "Dismiss"), notification.actions.map { it.title.toString() })
    }

    @Test
    fun aSilentReminder_goesToTheSilentChannel() {
        AndroidNamazNotifier(context).show(reminder().copy(sound = com.pixelquest.app.domain.namaz.NamazSound.SILENT))

        assertEquals(NamazChannels.REMINDERS_SILENT, shown().channelId)
    }

    @Test
    fun fajrAsAnAlarm_goesToTheAlarmChannel_asAnAlarm() {
        AndroidNamazNotifier(context).show(reminder().copy(asAlarm = true))

        val notification = shown()
        assertEquals(NamazChannels.FAJR_ALARM, notification.channelId)
        assertEquals(Notification.CATEGORY_ALARM, notification.category)
        val channel = context.getSystemService(NotificationManager::class.java).getNotificationChannel(NamazChannels.FAJR_ALARM)
        assertEquals(android.media.AudioAttributes.USAGE_ALARM, channel.audioAttributes.usage)
    }
}
