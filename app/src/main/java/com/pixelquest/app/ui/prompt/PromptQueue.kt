package com.pixelquest.app.ui.prompt

import android.content.Intent
import android.os.Bundle

/** [occurrenceDate] is the day of the quest the reminder was for (null for reminders from before Day 31). */
data class PromptRequest(val taskId: Long, val taskName: String, val occurrenceDate: java.time.LocalDate? = null)

/**
 * The "Did you do it?" prompts waiting in [TaskPromptActivity]. Two quests due in the same minute used
 * to show only the first: the activity is singleTop, so the second launch arrived as a new intent it
 * ignored. Now each launch joins the queue, and answering or dismissing one shows the next.
 */
data class PromptQueue(val items: List<PromptRequest> = emptyList()) {

    val current: PromptRequest? get() = items.firstOrNull()

    /** Adds [request] unless it is invalid or that quest is already waiting. */
    operator fun plus(request: PromptRequest?): PromptQueue =
        if (request == null || request.taskId < 0 || items.any { it.taskId == request.taskId }) this
        else copy(items = items + request)

    fun advance(): PromptQueue = copy(items = items.drop(1))

    fun saveTo(outState: Bundle) {
        outState.putLongArray(STATE_IDS, items.map { it.taskId }.toLongArray())
        outState.putStringArray(STATE_NAMES, items.map { it.taskName }.toTypedArray())
        outState.putLongArray(STATE_DAYS, items.map { it.occurrenceDate?.toEpochDay() ?: NO_DAY }.toLongArray())
    }

    companion object {
        const val EXTRA_TASK_ID = "EXTRA_TASK_ID"
        const val EXTRA_TASK_NAME = "EXTRA_TASK_NAME"
        private const val STATE_IDS = "prompt_queue_ids"
        private const val STATE_NAMES = "prompt_queue_names"
        private const val STATE_DAYS = "prompt_queue_days"
        private const val NO_DAY = Long.MIN_VALUE

        fun requestFrom(intent: Intent?): PromptRequest? {
            val taskId = intent?.getLongExtra(EXTRA_TASK_ID, -1L) ?: return null
            if (taskId < 0) return null
            return PromptRequest(
                taskId,
                intent.getStringExtra(EXTRA_TASK_NAME) ?: "Task",
                com.pixelquest.app.scheduling.TaskAlarmScheduler.occurrenceDateFrom(intent)
            )
        }

        /** The queue kept across a rotation, or null on a fresh start. */
        fun restoreFrom(savedState: Bundle?): PromptQueue? {
            val ids = savedState?.getLongArray(STATE_IDS) ?: return null
            val names = savedState.getStringArray(STATE_NAMES) ?: return null
            val days = savedState.getLongArray(STATE_DAYS)
            return PromptQueue(ids.zip(names).mapIndexed { i, (id, name) ->
                val day = days?.getOrNull(i)?.takeIf { it != NO_DAY }?.let { java.time.LocalDate.ofEpochDay(it) }
                PromptRequest(id, name, day)
            })
        }
    }
}
