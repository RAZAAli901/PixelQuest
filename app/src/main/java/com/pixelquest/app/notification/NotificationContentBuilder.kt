package com.pixelquest.app.notification

import com.pixelquest.app.domain.model.TaskCategory

/**
 * What a reminder knows about the user's day when it fires. Built in the receiver from Room;
 * kept free of Android types so the copy can be unit tested.
 */
data class ReminderContext(
    val taskName: String,
    val category: TaskCategory = TaskCategory.OTHER,
    val isSimpleMode: Boolean,
    val currentStreak: Int,
    val doneToday: Int,
    val totalToday: Int
)

data class NotificationCopy(
    val title: String,
    val text: String,
    val bigText: String
)

/** Turns a [ReminderContext] into reminder copy for gamified or Simple Mode. */
object NotificationContentBuilder {

    fun reminder(ctx: ReminderContext): NotificationCopy {
        val title = NotificationHelper.getReminderTitle(ctx.taskName, ctx.isSimpleMode)
        val progress = progressLine(ctx)
        val prompt = NotificationHelper.getReminderText(ctx.taskName, ctx.isSimpleMode)
        val bigText = listOfNotNull(progress, prompt).joinToString("\n")
        return NotificationCopy(title = title, text = progress ?: prompt, bigText = bigText)
    }

    /** "🔥 3-day streak · 1 of 3 quests done today", or null when there is nothing to report. */
    internal fun progressLine(ctx: ReminderContext): String? {
        val parts = mutableListOf<String>()
        if (!ctx.isSimpleMode && ctx.currentStreak > 0) {
            parts += "🔥 ${ctx.currentStreak}-day streak"
        }
        if (ctx.totalToday > 0) {
            val noun = if (ctx.isSimpleMode) "tasks" else "quests"
            parts += "${ctx.doneToday.coerceAtMost(ctx.totalToday)} of ${ctx.totalToday} $noun done today"
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    }
}
