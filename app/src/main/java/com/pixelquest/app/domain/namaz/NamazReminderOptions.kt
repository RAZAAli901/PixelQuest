package com.pixelquest.app.domain.namaz

import kotlinx.coroutines.flow.Flow

/**
 * The reminder options kept outside the database, in SharedPreferences (docs/namaz-design.md,
 * Reminders, Options): a few switches that didn't need a migration. A restore leaves them as they are.
 *
 * @param nudge a "still not prayed" reminder 20 minutes before the next prayer starts.
 * @param batteryTipDismissed the Namaz screen's battery-optimisation tip was hidden for good.
 * @param sound how a prayer's reminder sounds.
 */
data class NamazReminderOptions(
    val nudge: Boolean = false,
    val batteryTipDismissed: Boolean = false,
    val sound: NamazSound = NamazSound.SYSTEM
)

/** How a prayer's reminder sounds. Stored by name; an unknown one is [SYSTEM]. */
enum class NamazSound {
    /** The phone's notification sound. */
    SYSTEM,

    /** No sound or vibration: it appears quietly. */
    SILENT;

    companion object {
        fun fromName(name: String?): NamazSound = entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

/** Reads and changes [NamazReminderOptions]. */
interface NamazReminderOptionsStore {
    val options: Flow<NamazReminderOptions>

    suspend fun get(): NamazReminderOptions

    suspend fun update(change: (NamazReminderOptions) -> NamazReminderOptions)
}
