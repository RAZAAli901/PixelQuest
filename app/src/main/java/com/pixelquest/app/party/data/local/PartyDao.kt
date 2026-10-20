package com.pixelquest.app.party.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Every party's cache (docs/party-core.md): the parties of any kind and their members. Parties are
 * upserted, never replaced: a replace deletes the row first, and its members (and a Darood Party's
 * entries) would go with it.
 */
@Dao
interface PartyDao {

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

    /**
     * A party's members from the server, unless the party left the phone while they were on their
     * way (deleted, left or removed meanwhile): its rows would point at a party that's gone.
     *
     * @return whether the party was still there.
     */
    @Transaction
    suspend fun replaceMembersIfKept(partyId: String, members: List<PartyMemberEntity>): Boolean {
        if (getParty(partyId) == null) return false
        replaceMembers(partyId, members)
        return true
    }
}
