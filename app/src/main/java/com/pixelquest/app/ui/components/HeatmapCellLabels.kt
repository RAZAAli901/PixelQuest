package com.pixelquest.app.ui.components

import com.pixelquest.app.domain.model.DailyStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** What TalkBack reads for a heatmap cell, which on screen is only a coloured square. */
object HeatmapCellLabels {
    private val dateFormat = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ENGLISH)

    fun status(status: DailyStatus): String = when (status) {
        DailyStatus.PERFECT -> "perfect day"
        DailyStatus.PARTIAL -> "partly done"
        DailyStatus.MISSED -> "missed"
        DailyStatus.NO_TASKS_SCHEDULED -> "nothing scheduled"
        DailyStatus.IN_PROGRESS -> "nothing done yet"
    }

    /**
     * [today] is the heatmap's last day: later days (the rest of this week's column) haven't happened,
     * and today isn't over, so neither gets a final verdict such as "missed" or "nothing scheduled".
     */
    fun describe(date: LocalDate, status: DailyStatus, today: LocalDate? = null): String {
        val day = date.format(dateFormat)
        return when {
            today != null && date.isAfter(today) -> "$day: upcoming"
            today != null && date == today -> when (status) {
                DailyStatus.PERFECT -> "$day, today: perfect day"
                DailyStatus.PARTIAL -> "$day, today: partly done so far"
                DailyStatus.MISSED -> "$day, today: nothing done yet"
                DailyStatus.NO_TASKS_SCHEDULED -> "$day, today: nothing scheduled"
                DailyStatus.IN_PROGRESS -> "$day, today: nothing done yet"
            }
            else -> "$day: ${status(status)}"
        }
    }
}
