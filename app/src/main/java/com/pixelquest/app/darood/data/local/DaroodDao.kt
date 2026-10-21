package com.pixelquest.app.darood.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.pixelquest.app.darood.domain.EntryState
import com.pixelquest.app.party.data.local.PartyDao
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Darood Party's cache: every party's queries (PartyDao), and its entries and their queue. Screens
 * watch the Flows; a refresh from the server replaces whole sets in one transaction, so a screen
 * never sees half of one.
 */
@Dao
interface DaroodDao : PartyDao {

    // Entries

    /** The newest [limit] entries the server has for a party. */
    @Query("SELECT * FROM darood_entry WHERE partyId = :partyId ORDER BY id DESC LIMIT :limit")
    fun observeEntries(partyId: String, limit: Int): Flow<List<DaroodEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntries(entries: List<DaroodEntryEntity>)

    /** [entries] of one party ([partyId]), unless it has left the phone meanwhile (deleted, left or removed): its rows would point at a party that's gone. */
    @Transaction
    suspend fun upsertEntriesIfKept(partyId: String, entries: List<DaroodEntryEntity>): Boolean {
        if (getParty(partyId) == null) return false
        upsertEntries(entries)
        return true
    }

    @Query("SELECT MIN(id) FROM darood_entry WHERE partyId = :partyId")
    suspend fun oldestEntryId(partyId: String): Long?

    @Query("SELECT COUNT(*) FROM darood_entry WHERE partyId = :partyId")
    suspend fun entryCount(partyId: String): Int

    /** The server's copy of an entry posted on this phone. */
    @Query("SELECT * FROM darood_entry WHERE clientId = :clientId")
    suspend fun entryByClientId(clientId: String): DaroodEntryEntity?

    // Entries waiting to be sent

    // rowid rises with every insert, so it's the order the entries were posted in, even two within
    // the same millisecond, or across a change to the phone's clock.

    /** A party's waiting and refused entries, in the order posted. */
    @Query("SELECT * FROM darood_pending_entry WHERE partyId = :partyId ORDER BY rowid")
    fun observePending(partyId: String): Flow<List<PendingEntryEntity>>

    /** Every entry still to send, oldest first: they go in the order they were posted. */
    @Query("SELECT * FROM darood_pending_entry WHERE state = 'PENDING' ORDER BY rowid")
    suspend fun pendingToSend(): List<PendingEntryEntity>

    /** The account's entries for [day] still waiting to be sent (refused ones don't count). */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM darood_pending_entry WHERE partyId = :partyId AND localDate = :day AND state = 'PENDING'")
    fun observePendingTotalOn(partyId: String, day: LocalDate): Flow<Long>

    @Query("SELECT COUNT(*) FROM darood_pending_entry WHERE state = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPending(entry: PendingEntryEntity)

    @Query("SELECT * FROM darood_pending_entry WHERE clientId = :clientId")
    suspend fun getPending(clientId: String): PendingEntryEntity?

    @Query("DELETE FROM darood_pending_entry WHERE clientId = :clientId")
    suspend fun deletePending(clientId: String)

    @Query("UPDATE darood_pending_entry SET attempts = attempts + 1 WHERE clientId = :clientId")
    suspend fun countAttempt(clientId: String)

    @Query("UPDATE darood_pending_entry SET state = :state, refusal = :refusal WHERE clientId = :clientId")
    suspend fun setPendingState(clientId: String, state: EntryState, refusal: String?)

    /** It reached the server: the server's copy replaces the phone's, in one step. */
    @Transaction
    suspend fun confirmSent(clientId: String, stored: DaroodEntryEntity) {
        upsertEntries(listOf(stored))
        deletePending(clientId)
    }

    // Closed weeks (docs/darood-weekly.md)

    /** A party's closed weeks, newest first, each in display order. */
    @Query("SELECT * FROM darood_week_result WHERE partyId = :partyId ORDER BY weekStart DESC, position ASC, activeDays DESC")
    fun observeWeekResults(partyId: String): Flow<List<DaroodWeekResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeekResults(rows: List<DaroodWeekResultEntity>)

    /** A week's places, unless the party has left the phone meanwhile. */
    @Transaction
    suspend fun upsertWeekResultsIfKept(partyId: String, rows: List<DaroodWeekResultEntity>): Boolean {
        if (getParty(partyId) == null) return false
        upsertWeekResults(rows)
        return true
    }

    @Query("SELECT EXISTS(SELECT 1 FROM darood_week_notified WHERE partyId = :partyId AND weekStart = :weekStart)")
    suspend fun weekNotified(partyId: String, weekStart: LocalDate): Boolean

    /** Marks the week told; false if it already was (or the party's gone), so only one notification goes. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWeekNotified(row: DaroodWeekNotifiedEntity): Long

    @Transaction
    suspend fun markWeekNotified(partyId: String, weekStart: LocalDate, at: Long): Boolean {
        if (getParty(partyId) == null) return false
        return insertWeekNotified(DaroodWeekNotifiedEntity(partyId, weekStart, at)) != -1L
    }

    // Everything

    /** Signing out: none of it stays on the phone (members, entries and pending entries go with their parties). */
    @Query("DELETE FROM darood_party")
    suspend fun clearAll()
}
