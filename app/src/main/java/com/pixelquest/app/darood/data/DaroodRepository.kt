package com.pixelquest.app.darood.data

import com.pixelquest.app.darood.sync.PartyChange
import com.pixelquest.app.darood.domain.DaroodEntry
import com.pixelquest.app.darood.domain.DayTotals
import com.pixelquest.app.darood.domain.MemberStats
import com.pixelquest.app.data.remote.SupabaseResult
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Darood Party's own (docs/party-core.md): its entries and their queue on the phone, its numbers,
 * its milestone and the race board. Every party's calls are in PartyRepository. Nothing here reaches
 * XP, levels, streaks or the leaderboard.
 */
interface DaroodRepository {
    /** Each poster's part of the party's total, largest first. */
    fun observeMemberStats(partyId: String): Flow<List<MemberStats>>

    /**
     * The newest [limit] entries the phone has for a party, newest first: those still waiting to be
     * sent (PENDING) or refused (FAILED) first, then the server's.
     */
    fun observeEntries(partyId: String, limit: Int): Flow<List<DaroodEntry>>

    /** Today, as the phone's clock and time zone have it: the day its entries are posted for. */
    fun today(): LocalDate

    /**
     * The player's and the party's totals for [day], from the server (party_day_stats). The
     * player's entries still waiting on the phone aren't in them: [observePendingTotalOn] has those.
     */
    suspend fun fetchDayTotals(partyId: String, day: LocalDate): SupabaseResult<DayTotals>

    /** The race board, from the server: never kept on the phone, since it's other parties' standings. */
    suspend fun fetchRaceBoard(): SupabaseResult<List<com.pixelquest.app.darood.domain.RaceLine>>

    /** The player's total on each day they posted in a party, from the server (party_day_stats). */
    suspend fun fetchMyDays(partyId: String): SupabaseResult<Map<LocalDate, Long>>

    /** The player's entries for [day] still waiting to be sent, summed. */
    fun observePendingTotalOn(partyId: String, day: LocalDate): Flow<Long>

    /** How many entries wait to be sent, in every party. */
    fun observePendingCount(): Flow<Int>

    /**
     * Posts [amount] for today, in the phone's time zone. It's saved on the phone first, as PENDING,
     * then sent; without a connection it stays until it can be. Returns the entry as saved.
     */
    suspend fun addEntry(partyId: String, amount: Int): DaroodEntry

    /** Sends every waiting entry, oldest first, until one can't be sent for now. */
    suspend fun sendPending(): SendReport

    /**
     * Brings a party's live changes into the cache while it's collected, and passes each on: a new
     * entry appears at once, the party's numbers follow once the changes pause, and a change of
     * members refreshes the party. Collect it while the party's screen is up.
     */
    fun followParty(partyId: String): Flow<PartyChange>

    /**
     * Tries a refused entry again, as it was (same key, same day): it waits as PENDING once more, and
     * goes with the others. A rule that still holds refuses it again, with its reason.
     */
    suspend fun retryEntry(clientId: String): SendReport

    /** The page of entries before the oldest the phone has: true if there may be more. */
    suspend fun loadOlderEntries(partyId: String): SupabaseResult<Boolean>

    /** A new party led by the account: its id. */
    suspend fun createParty(name: String, maxMembers: Int, milestone: Long?): SupabaseResult<String>

    suspend fun setMilestone(partyId: String, target: Long): SupabaseResult<Long>

    suspend fun setRaceBoard(partyId: String, on: Boolean): SupabaseResult<Unit>
}
