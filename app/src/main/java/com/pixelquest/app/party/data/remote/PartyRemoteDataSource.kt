package com.pixelquest.app.party.data.remote

import com.pixelquest.app.darood.domain.MemberStats
import com.pixelquest.app.darood.domain.PartyStats
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.PartyMember
import com.pixelquest.app.party.domain.PartyType

/**
 * Every party's calls to Supabase, whatever its kind (docs/party-core.md): the account's parties,
 * their members, and what anyone does about membership. Row-level security decides what each call
 * may see (supabase/schema.md, Parties).
 */
interface PartyRemoteDataSource {
    /** The parties the account is active in or invited to. */
    suspend fun fetchMyParties(): SupabaseResult<List<RemoteParty>>

    /** A party's members (all statuses), with the usernames the account may see. */
    suspend fun fetchMembers(partyId: String): SupabaseResult<List<PartyMember>>

    /** The leader invites [username]. */
    suspend fun invite(partyId: String, username: String): SupabaseResult<InviteOutcome>

    /** Accepts or declines an invitation: the account's new status. */
    suspend fun respondToInvite(partyId: String, accept: Boolean): SupabaseResult<MemberStatus>

    suspend fun leaveParty(partyId: String): SupabaseResult<Unit>

    /** The leader removes a member or withdraws an invitation. */
    suspend fun removeMember(partyId: String, userId: String): SupabaseResult<Unit>

    /** The leader renames the party. */
    suspend fun renameParty(partyId: String, name: String): SupabaseResult<Unit>

    /** The leader hands the leadership to [userId], an active member; the leader stays as a member. */
    suspend fun transferLeadership(partyId: String, userId: String): SupabaseResult<Unit>

    /** The leader deletes the party: it's gone for everyone. */
    suspend fun deleteParty(partyId: String): SupabaseResult<Unit>

    /** The numbers of the given parties (those the account is active in; others have no row). */
    suspend fun fetchStats(partyIds: List<String>): SupabaseResult<List<PartyStats>>

    /** Each poster's part of a party's total, largest first. */
    suspend fun fetchMemberStats(partyId: String): SupabaseResult<List<MemberStats>>

    /** A new party of [type] led by the account, with its name and size only: its id. */
    suspend fun createParty(name: String, maxMembers: Int, type: PartyType): SupabaseResult<String>
}
