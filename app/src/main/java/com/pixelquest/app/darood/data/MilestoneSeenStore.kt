package com.pixelquest.app.darood.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Which milestone each party's player has already been told about on this phone, by when it was set:
 * a new one (or a raised one) is announced once, even if it was set while they were away.
 */
interface MilestoneSeenStore {
    /** When the last milestone they were told about was set, in epoch millis; null if none yet. */
    fun seenAt(partyId: String): Long?

    fun markSeen(partyId: String, setAt: Long)
}

@Singleton
class PrefsMilestoneSeenStore @Inject constructor(
    @ApplicationContext context: Context
) : MilestoneSeenStore {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override fun seenAt(partyId: String): Long? =
        prefs.getLong(key(partyId), NONE).takeIf { it != NONE }

    override fun markSeen(partyId: String, setAt: Long) {
        prefs.edit().putLong(key(partyId), setAt).apply()
    }

    private fun key(partyId: String) = "milestone_seen_$partyId"

    private companion object {
        const val PREFS = "pixelquest_darood"
        const val NONE = Long.MIN_VALUE
    }
}
