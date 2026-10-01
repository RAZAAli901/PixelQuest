package com.pixelquest.app.domain.ai

import java.time.LocalDate

/** Supplies the short encouraging line added to a task reminder. */
interface EncouragementMessageProvider {
    fun messageFor(date: LocalDate, taskId: Long, isSimpleMode: Boolean): String
}

/**
 * Built-in lines used when AI-written messages are off, unavailable or stale. The pick is
 * deterministic per day and task, so the same reminder doesn't change text if it is re-posted.
 */
object StaticEncouragementBank : EncouragementMessageProvider {

    val gamified = listOf(
        "Every quest you finish adds to your legend.",
        "Small quests, big hero. You've got this.",
        "Ready your gear: this one won't take long.",
        "Your future self is cheering you on.",
        "One quest at a time is how heroes level up.",
        "The realm needs you for just a few minutes."
    )

    val calm = listOf(
        "A small step today keeps the habit going.",
        "Doing it now means one less thing on your mind.",
        "Consistency matters more than perfection.",
        "Start small; a few minutes still counts.",
        "You've done this before. You can do it again.",
        "Routines get easier each time you show up."
    )

    override fun messageFor(date: LocalDate, taskId: Long, isSimpleMode: Boolean): String {
        val lines = if (isSimpleMode) calm else gamified
        return pick(lines, date, taskId)
    }

    internal fun pick(lines: List<String>, date: LocalDate, taskId: Long): String {
        val index = Math.floorMod(date.toEpochDay() * 31 + taskId, lines.size.toLong()).toInt()
        return lines[index]
    }
}
