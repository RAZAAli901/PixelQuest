package com.pixelquest.app.namaz

import android.content.Context
import android.content.SharedPreferences
import com.pixelquest.app.domain.namaz.NamazReminderOptions
import com.pixelquest.app.domain.namaz.NamazReminderOptionsStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/** [NamazReminderOptions] in their own SharedPreferences file. The key names are released: add, don't rename. */
@Singleton
class PrefsNamazReminderOptionsStore @Inject constructor(
    @ApplicationContext private val context: Context
) : NamazReminderOptionsStore {

    private val prefs: SharedPreferences by lazy { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    private fun read() = NamazReminderOptions(
        nudge = prefs.getBoolean(KEY_NUDGE, false)
    )

    override val options: Flow<NamazReminderOptions> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(read()) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(read())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override suspend fun get(): NamazReminderOptions = read()

    override suspend fun update(change: (NamazReminderOptions) -> NamazReminderOptions) {
        val next = change(read())
        prefs.edit()
            .putBoolean(KEY_NUDGE, next.nudge)
            .apply()
    }

    companion object {
        const val PREFS = "pixelquest_namaz_reminders"
        private const val KEY_NUDGE = "nudge"
    }
}
