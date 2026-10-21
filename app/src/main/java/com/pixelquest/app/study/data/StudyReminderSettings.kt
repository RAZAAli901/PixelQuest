package com.pixelquest.app.study.data

import android.content.Context
import com.pixelquest.app.study.sync.StudyDueWorker
import com.pixelquest.app.study.sync.StudyWeeklyWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A study party's notifications on this phone (docs/study-party.md, Notifications): reminders of
 * what's due, on unless the player turns them off; the Sunday summary, off unless they turn it on.
 */
interface StudyReminderSettings {
    val dueReminders: StateFlow<Boolean>
    val weeklySummary: StateFlow<Boolean>

    fun setDueReminders(on: Boolean)
    fun setWeeklySummary(on: Boolean)
}

/** Kept in the feature's preferences; turning reminders on schedules their check, and off cancels it. */
@Singleton
class PrefsStudyReminderSettings @Inject constructor(
    @ApplicationContext private val context: Context
) : StudyReminderSettings {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _dueReminders = MutableStateFlow(prefs.getBoolean(KEY_DUE, true))
    override val dueReminders: StateFlow<Boolean> = _dueReminders.asStateFlow()
    private val _weeklySummary = MutableStateFlow(prefs.getBoolean(KEY_WEEKLY, false))
    override val weeklySummary: StateFlow<Boolean> = _weeklySummary.asStateFlow()

    override fun setDueReminders(on: Boolean) {
        prefs.edit().putBoolean(KEY_DUE, on).apply()
        _dueReminders.value = on
        if (on) StudyDueWorker.schedule(context) else StudyDueWorker.cancel(context)
    }

    override fun setWeeklySummary(on: Boolean) {
        prefs.edit().putBoolean(KEY_WEEKLY, on).apply()
        _weeklySummary.value = on
        if (on) StudyWeeklyWorker.schedule(context) else StudyWeeklyWorker.cancel(context)
    }

    private companion object {
        const val PREFS = "pixelquest_study"
        const val KEY_DUE = "due_reminders"
        const val KEY_WEEKLY = "weekly_summary"
    }
}

/** In memory, for tests and previews. */
class MemoryStudyReminderSettings(due: Boolean = true, weekly: Boolean = false) : StudyReminderSettings {
    private val _dueReminders = MutableStateFlow(due)
    override val dueReminders: StateFlow<Boolean> = _dueReminders.asStateFlow()
    private val _weeklySummary = MutableStateFlow(weekly)
    override val weeklySummary: StateFlow<Boolean> = _weeklySummary.asStateFlow()
    override fun setDueReminders(on: Boolean) {
        _dueReminders.value = on
    }

    override fun setWeeklySummary(on: Boolean) {
        _weeklySummary.value = on
    }
}
