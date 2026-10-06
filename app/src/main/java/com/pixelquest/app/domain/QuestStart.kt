package com.pixelquest.app.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * The first day a new quest is due. A quest created after its time of day starts tomorrow, so its
 * first occurrence isn't already past (and recorded as missed, lowering today's result) the moment it
 * is saved. Editing a quest keeps its start day.
 */
object QuestStart {

    fun firstDay(now: LocalDateTime, time: LocalTime): LocalDate =
        if (now.toLocalTime().isBefore(time)) now.toLocalDate() else now.toLocalDate().plusDays(1)

    fun startsTomorrow(now: LocalDateTime, time: LocalTime): Boolean = firstDay(now, time) != now.toLocalDate()
}
