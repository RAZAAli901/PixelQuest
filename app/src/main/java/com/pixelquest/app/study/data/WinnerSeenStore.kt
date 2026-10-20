package com.pixelquest.app.study.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Which week's winners each study party's player has already been told about on this phone, as
 * Darood's MilestoneSeenStore does for milestones: a week's banner shows until it's dismissed, once.
 */
interface WinnerSeenStore {
    /** The Monday of the last week whose winners they dismissed; null if none yet. */
    fun seenWeek(partyId: String): LocalDate?

    fun markSeen(partyId: String, weekStart: LocalDate)
}

@Singleton
class PrefsWinnerSeenStore @Inject constructor(@ApplicationContext context: Context) : WinnerSeenStore {
    private val prefs = context.getSharedPreferences("pixelquest_study", Context.MODE_PRIVATE)

    override fun seenWeek(partyId: String): LocalDate? = prefs.getString(key(partyId), null)?.let(LocalDate::parse)

    override fun markSeen(partyId: String, weekStart: LocalDate) {
        prefs.edit().putString(key(partyId), weekStart.toString()).apply()
    }

    private fun key(partyId: String) = "winner_seen_$partyId"
}

/** In memory, for tests and previews. */
class MemoryWinnerSeenStore : WinnerSeenStore {
    private val seen = mutableMapOf<String, LocalDate>()
    override fun seenWeek(partyId: String): LocalDate? = seen[partyId]
    override fun markSeen(partyId: String, weekStart: LocalDate) {
        seen[partyId] = weekStart
    }
}
