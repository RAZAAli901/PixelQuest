package com.pixelquest.app.darood.sync

import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.darood.data.DaroodPartyRepository
import com.pixelquest.app.darood.domain.WeekResult
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.Party
import com.pixelquest.app.party.domain.PartyType
import kotlinx.coroutines.flow.first

/** Posts a closed week's notification (docs/darood-weekly.md, The notification). */
fun interface WeekResultNotifier {
    /** [names]: each ranked member's username, by user id (null when the phone doesn't know it). */
    fun post(party: Party, week: WeekResult, me: String, names: Map<String, String?>)
}

/**
 * One round of the weekly results (J074): for each Darood Party the player is active in, last
 * week is closed (any member may ask; it's stored once), read, cached and told, once per party
 * per week (a muted party's is marked told without a notification, so turning it back on never
 * brings old weeks). A week still running in the party's zone, or a failed call, is tried on the next round.
 * The leader's notification is the same as everyone's: nothing here looks at roles.
 */
class WeekResultsRun(
    private val repository: DaroodPartyRepository,
    private val me: () -> String?,
    private val notifier: WeekResultNotifier,
    private val mutes: WeekRankingMutes? = null
) {
    /** How many notifications it posted. */
    suspend fun run(): Int {
        val account = me() ?: return 0
        if (repository.refreshParties() !is SupabaseResult.Success) return 0
        val parties = repository.observeMyParties(PartyType.DAROOD).first().filter { it.myStatus == MemberStatus.ACTIVE }
        var posted = 0
        for (party in parties) {
            val closed = repository.closeLastWeek(party.id)
            val week = (closed as? SupabaseResult.Success)?.data ?: continue
            // Marked before it's posted: two rounds at once still tell it once.
            if (!repository.markWeekTold(party.id, week.weekStart)) continue
            // Joined after the week closed: nothing of theirs to tell. Turned off: told, quietly.
            if (week.placeOf(account) == null || mutes?.silent(party.id) == true) continue
            val names = repository.observeMembers(party.id).first().associate { it.userId to it.username }
            notifier.post(party, week, account, week.places.associate { it.userId to names[it.userId] })
            posted++
        }
        return posted
    }
}
