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
    val totalToday: Int,
    /** Minutes until the task's time when the reminder fires early (lead time); 0 = due now. */
    val startsInMinutes: Int = 0,
    /** Short encouraging line shown last in the expanded reminder. */
    val encouragement: String? = null
)

data class NotificationCopy(
    val title: String,
    val text: String,
    val bigText: String
)

/** Turns a [ReminderContext] into reminder copy for gamified or Simple Mode. */
object NotificationContentBuilder {

    fun reminder(ctx: ReminderContext): NotificationCopy {
        val soon = ctx.startsInMinutes > 0
        val title = when {
            ctx.isSimpleMode && soon -> "Coming up in ${ctx.startsInMinutes} min: ${ctx.taskName}"
            ctx.isSimpleMode -> NotificationHelper.getReminderTitle(ctx.taskName, isSimpleMode = true)
            soon -> "${categoryGlyph(ctx.category)} Quest in ${ctx.startsInMinutes} min: ${ctx.taskName}"
            else -> "${categoryGlyph(ctx.category)} Quest Time: ${ctx.taskName}"
        }
        val progress = progressLine(ctx)
        val prompt = streakAtRiskLine(ctx) ?: NotificationHelper.getReminderText(ctx.taskName, ctx.isSimpleMode)
        val bigText = listOfNotNull(progress, prompt, ctx.encouragement?.takeIf { it.isNotBlank() }).joinToString("\n")
        return NotificationCopy(title = title, text = progress ?: prompt, bigText = bigText)
    }

    fun categoryGlyph(category: TaskCategory): String = when (category) {
        TaskCategory.FITNESS -> "💪"
        TaskCategory.HEALTH -> "❤️"
        TaskCategory.LEARNING -> "📚"
        TaskCategory.CHORES -> "🧹"
        TaskCategory.OTHER -> "⚔️"
    }

    /** Only in gamified mode, and only while there is a streak to protect and work left today. */
    internal fun streakAtRiskLine(ctx: ReminderContext): String? {
        if (ctx.isSimpleMode || ctx.currentStreak <= 0) return null
        val remaining = ctx.totalToday - ctx.doneToday
        if (remaining <= 0) return null
        val quests = if (remaining == 1) "quest" else "quests"
        return "$remaining $quests left today. Keep your ${ctx.currentStreak}-day streak alive!"
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
