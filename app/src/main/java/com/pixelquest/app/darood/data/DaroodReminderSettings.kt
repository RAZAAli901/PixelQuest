package com.pixelquest.app.darood.data

import android.content.Context
import com.pixelquest.app.darood.sync.DaroodWeeklyWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** The weekly Darood note: off unless the player turns it on, on this phone. */
interface DaroodReminderSettings {
    val weeklyReminder: StateFlow<Boolean>

    fun setWeeklyReminder(on: Boolean)
}

/** Kept in the feature's preferences; turning it on schedules the weekly note, and off cancels it. */
@Singleton
class PrefsDaroodReminderSettings @Inject constructor(
    @ApplicationContext private val context: Context
) : DaroodReminderSettings {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _weeklyReminder = MutableStateFlow(prefs.getBoolean(KEY_WEEKLY, false))
    override val weeklyReminder: StateFlow<Boolean> = _weeklyReminder.asStateFlow()

    override fun setWeeklyReminder(on: Boolean) {
        prefs.edit().putBoolean(KEY_WEEKLY, on).apply()
        _weeklyReminder.value = on
        if (on) DaroodWeeklyWorker.schedule(context) else DaroodWeeklyWorker.cancel(context)
    }

    private companion object {
        const val PREFS = "pixelquest_darood"
        const val KEY_WEEKLY = "weekly_reminder"
    }
}
