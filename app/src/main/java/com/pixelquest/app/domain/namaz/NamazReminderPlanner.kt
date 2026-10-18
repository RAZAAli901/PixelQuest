package com.pixelquest.app.domain.namaz

import com.pixelquest.app.data.local.entity.NamazSettingsEntity
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * Which Namaz alarms should be armed now (docs/namaz-design.md, Reminders): for the next
 * [WINDOW_HOURS], each prayer whose reminder is on, at its start less the reminder offset. Pure
 * apart from the prayer times, so it can be tested with a fixed moment.
 */
class NamazReminderPlanner @Inject constructor(
    private val provider: PrayerTimeProvider
) {
    suspend fun plan(settings: NamazSettingsEntity, now: ZonedDateTime): List<NamazAlarm> {
        if (!settings.enabled) return emptyList()
        val until = now.plusHours(WINDOW_HOURS)
        val offset = settings.reminderOffsetMin.coerceIn(0, MAX_OFFSET_MINUTES).toLong()
        // Yesterday's prayer day reaches past midnight (Isha); the window reaches two days on.
        val days = (-1L..2L).map { now.toLocalDate().plusDays(it) }
        return days.flatMap { day ->
            val times = provider.timesFor(day)
            Prayer.entries.filter { settings.remindsFor(it) }.mapNotNull { prayer ->
                val at = times.timeOf(prayer).minusMinutes(offset)
                if (at.isAfter(now) && !at.isAfter(until)) {
                    NamazAlarm(NamazAlarm.Kind.REMINDER, day, prayer, at.toInstant().toEpochMilli())
                } else {
                    null
                }
            }
        }.sortedBy { it.atMillis }
    }

    companion object {
        const val WINDOW_HOURS = 48L

        /** The offsets offered: at the start, or 5, 10 or 15 minutes before. */
        val OFFSETS = listOf(0, 5, 10, 15)

        /** Anything larger (an edited backup) is treated as this. */
        const val MAX_OFFSET_MINUTES = 120
    }
}
