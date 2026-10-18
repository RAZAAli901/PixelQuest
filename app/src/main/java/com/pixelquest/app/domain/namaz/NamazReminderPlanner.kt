package com.pixelquest.app.domain.namaz

import com.pixelquest.app.data.local.entity.NamazSettingsEntity
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * Which Namaz alarms should be armed now (docs/namaz-design.md, Reminders): for the next
 * [WINDOW_HOURS], each prayer whose reminder is on, at its start less the reminder offset, and with
 * the nudge on, its "still not prayed" [NUDGE_MINUTES] before the next prayer starts. Pure apart
 * from the prayer times, so it can be tested with a fixed moment.
 */
class NamazReminderPlanner @Inject constructor(
    private val provider: PrayerTimeProvider
) {
    suspend fun plan(
        settings: NamazSettingsEntity,
        now: ZonedDateTime,
        options: NamazReminderOptions = NamazReminderOptions()
    ): List<NamazAlarm> {
        if (!settings.enabled) return emptyList()
        val until = now.plusHours(WINDOW_HOURS)
        val offset = settings.reminderOffsetMin.coerceIn(0, MAX_OFFSET_MINUTES).toLong()
        // Yesterday's prayer day reaches past midnight (Isha); the window reaches two days on.
        val days = (-1L..2L).map { now.toLocalDate().plusDays(it) }
        fun inWindow(at: ZonedDateTime) = at.isAfter(now) && !at.isAfter(until)
        return days.flatMap { day ->
            val times = provider.timesFor(day)
            val nextFajr = provider.timesFor(day.plusDays(1)).fajr
            Prayer.entries.filter { settings.remindsFor(it) }.flatMap { prayer ->
                val reminder = times.timeOf(prayer).minusMinutes(offset)
                // The nudge comes while there's still time: before the next prayer (Isha's: tomorrow's Fajr).
                val nudge = (prayer.nextInDay?.let { times.timeOf(it) } ?: nextFajr).minusMinutes(NUDGE_MINUTES)
                listOfNotNull(
                    NamazAlarm(NamazAlarm.Kind.REMINDER, day, prayer, reminder.toInstant().toEpochMilli(), alarmClock = options.asAlarm(prayer))
                        .takeIf { inWindow(reminder) },
                    NamazAlarm(NamazAlarm.Kind.NUDGE, day, prayer, nudge.toInstant().toEpochMilli()).takeIf { options.nudge && inWindow(nudge) }
                )
            }
        }.sortedBy { it.atMillis }
    }

    companion object {
        const val WINDOW_HOURS = 48L

        /** How long before the next prayer the nudge comes. */
        const val NUDGE_MINUTES = 20L

        /** The offsets offered: at the start, or 5, 10 or 15 minutes before. */
        val OFFSETS = listOf(0, 5, 10, 15)

        /** Anything larger (an edited backup) is treated as this. */
        const val MAX_OFFSET_MINUTES = 120
    }
}
