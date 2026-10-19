package com.pixelquest.app.darood.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.pixelquest.app.darood.domain.EntryState
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Darood Party's cache. Screens watch the Flows; a refresh from the server replaces whole sets in
 * one transaction, so a screen never sees half of one. Parties are upserted, never replaced: a
 * replace deletes the row first, and its members, entries and pending entries would go with it.
 */
@Dao
interface DaroodDao {

    // Parties

    /** Invitations first, then the parties the account is in, by name. */
    @Query("SELECT * FROM darood_party ORDER BY CASE myStatus WHEN 'INVITED' THEN 0 ELSE 1 END, name COLLATE NOCASE")
    fun observeParties(): Flow<List<PartyEntity>>

    @Query("SELECT * FROM darood_party WHERE id = :partyId")
    fun observeParty(partyId: String): Flow<PartyEntity?>

    @Query("SELECT * FROM darood_party WHERE id = :partyId")
    suspend fun getParty(partyId: String): PartyEntity?

    @Upsert
    suspend fun upsertParties(parties: List<PartyEntity>)

    @Query("DELETE FROM darood_party WHERE id NOT IN (:keep)")
    suspend fun deletePartiesNotIn(keep: List<String>)

    @Query("DELETE FROM darood_party WHERE id = :partyId")
    suspend fun deleteParty(partyId: String)

    /** The account's parties as the server lists them now: others (left, removed, deleted) go. */
    @Transaction
    suspend fun replaceParties(parties: List<PartyEntity>) {
        deletePartiesNotIn(parties.map { it.id })
        upsertParties(parties)
    }

    // Members

    @Query("SELECT * FROM darood_party_member WHERE partyId = :partyId ORDER BY CASE role WHEN 'LEADER' THEN 0 ELSE 1 END, total DESC, username COLLATE NOCASE")
    fun observeMembers(partyId: String): Flow<List<PartyMemberEntity>>

    @Query("DELETE FROM darood_party_member WHERE partyId = :partyId")
    suspend fun deleteMembers(partyId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<PartyMemberEntity>)

    @Transaction
    suspend fun replaceMembers(partyId: String, members: List<PartyMemberEntity>) {
        deleteMembers(partyId)
        insertMembers(members)
    }

    // Entries

    /** The newest [limit] entries the server has for a party. */
    @Query("SELECT * FROM darood_entry WHERE partyId = :partyId ORDER BY id DESC LIMIT :limit")
    fun observeEntries(partyId: String, limit: Int): Flow<List<DaroodEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntries(entries: List<DaroodEntryEntity>)

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

    // Everything

    /** Signing out: none of it stays on the phone (members, entries and pending entries go with their parties). */
    @Query("DELETE FROM darood_party")
    suspend fun clearAll()
}
