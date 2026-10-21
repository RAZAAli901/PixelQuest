package com.pixelquest.app.study.data

import android.content.Context
import com.pixelquest.app.study.sync.StudyDueWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A study party's notifications on this phone (docs/study-party.md, Notifications): reminders of
 * what's due, on unless the player turns them off.
 */
interface StudyReminderSettings {
    val dueReminders: StateFlow<Boolean>

    fun setDueReminders(on: Boolean)
}

/** Kept in the feature's preferences; turning reminders on schedules their check, and off cancels it. */
@Singleton
class PrefsStudyReminderSettings @Inject constructor(
    @ApplicationContext private val context: Context
) : StudyReminderSettings {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _dueReminders = MutableStateFlow(prefs.getBoolean(KEY_DUE, true))
    override val dueReminders: StateFlow<Boolean> = _dueReminders.asStateFlow()

    override fun setDueReminders(on: Boolean) {
        prefs.edit().putBoolean(KEY_DUE, on).apply()
        _dueReminders.value = on
        if (on) StudyDueWorker.schedule(context) else StudyDueWorker.cancel(context)
    }

    private companion object {
        const val PREFS = "pixelquest_study"
        const val KEY_DUE = "due_reminders"
    }
}

/** In memory, for tests and previews. */
class MemoryStudyReminderSettings(due: Boolean = true) : StudyReminderSettings {
    private val _dueReminders = MutableStateFlow(due)
    override val dueReminders: StateFlow<Boolean> = _dueReminders.asStateFlow()
    override fun setDueReminders(on: Boolean) {
        _dueReminders.value = on
    }
}
