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
    }

    fun describe(date: LocalDate, status: DailyStatus): String = "${date.format(dateFormat)}: ${status(status)}"
}
