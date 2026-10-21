package com.pixelquest.app.study.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What the phone keeps about an account's study parties outside the database: a running timer,
 * which week's winners were seen, which reminders were sent. It goes when the account does
 * (DaroodAccountCleanup), as the parties' cache does; the player's notification choices stay, as
 * they're the phone's.
 */
fun interface StudyAccountData {
    fun clear()
}

@Singleton
class PrefsStudyAccountData @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timer: StudyTimerStore
) : StudyAccountData {
    override fun clear() {
        // The timer holds its state in memory too: it stops now, not at the next launch.
        timer.clear()
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val edit = prefs.edit()
        prefs.all.keys.filterNot { it in PHONE_SETTINGS }.forEach { edit.remove(it) }
        edit.apply()
    }

    internal companion object {
        const val PREFS = "pixelquest_study"

        /** The player's own choices on this phone (StudyReminderSettings): kept. */
        val PHONE_SETTINGS = setOf("due_reminders", "weekly_summary")
    }
}
