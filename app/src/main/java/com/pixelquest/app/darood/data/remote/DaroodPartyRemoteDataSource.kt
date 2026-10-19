package com.pixelquest.app.darood.data.remote

import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.safeSupabaseCall
import com.pixelquest.app.darood.domain.PartyMember
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import javax.inject.Inject
import javax.inject.Singleton

/** The signed-in account, as Darood Party asks for it. */
fun interface DaroodAccount {
    /** The account's id, or null when signed out. */
    fun id(): String?
}

@Singleton
class SupabaseDaroodAccount @Inject constructor(private val auth: Auth) : DaroodAccount {
    override fun id(): String? = auth.currentUserOrNull()?.id
}

/** A party the account is in or invited to, with the account's own membership row. */
data class RemoteParty(val party: PartyDto, val membership: PartyMemberDto)

/**
 * Darood Party on Supabase, through the app's one Postgrest client. Row-level security decides
 * what each call may see (supabase/schema.md, Darood Parties); every call is wrapped in
 * [safeSupabaseCall], like the rest of the app's Supabase calls.
 */
interface DaroodPartyRemoteDataSource {
    /** The parties the account is active in or invited to. */
    suspend fun fetchMyParties(): SupabaseResult<List<RemoteParty>>

    /** A party's members (all statuses), with the usernames the account may see. */
    suspend fun fetchMembers(partyId: String): SupabaseResult<List<PartyMember>>
}

@Singleton
class SupabaseDaroodPartyRemoteDataSource @Inject constructor(
    private val postgrest: Postgrest,
    private val account: DaroodAccount
) : DaroodPartyRemoteDataSource {

    private fun signedOut() = SupabaseResult.AuthError(IllegalStateException("Not signed in"), "Sign in to use Darood Parties.")

    override suspend fun fetchMyParties(): SupabaseResult<List<RemoteParty>> {
        val me = account.id() ?: return signedOut()
        return safeSupabaseCall {
            val memberships = postgrest[MEMBERS].select(Columns.list(MEMBER_COLUMNS)) {
                filter {
                    eq("user_id", me)
                    isIn("status", listOf("active", "invited"))
                }
            }.decodeList<PartyMemberDto>()
            if (memberships.isEmpty()) return@safeSupabaseCall emptyList()
            val parties = postgrest[PARTIES].select(Columns.list(PARTY_COLUMNS)) {
                filter { isIn("id", memberships.map { it.partyId }) }
            }.decodeList<PartyDto>().associateBy { it.id }
            // A party deleted meanwhile isn't readable: its membership is dropped with it.
            memberships.mapNotNull { membership -> parties[membership.partyId]?.let { RemoteParty(it, membership) } }
        }
    }

    override suspend fun fetchMembers(partyId: String): SupabaseResult<List<PartyMember>> {
        if (account.id() == null) return signedOut()
        return safeSupabaseCall {
            val members = postgrest[MEMBERS].select(Columns.list(MEMBER_COLUMNS)) {
                filter { eq("party_id", partyId) }
            }.decodeList<PartyMemberDto>()
            val names = if (members.isEmpty()) {
                emptyMap()
            } else {
                postgrest[USERNAMES].select(Columns.list("user_id", "username")) {
                    filter { isIn("user_id", members.map { it.userId }) }
                }.decodeList<MemberNameDto>().associate { it.userId to it.username }
            }
            members.map { it.toDomain(names[it.userId]) }
        }
    }

    internal companion object {
        const val PARTIES = "parties"
        const val MEMBERS = "party_members"
        const val USERNAMES = "usernames"
        val PARTY_COLUMNS = listOf("id", "name", "leader_id", "max_members", "milestone", "milestone_set_at", "completed_at", "created_at")
        val MEMBER_COLUMNS = listOf("party_id", "user_id", "role", "status", "invited_at", "joined_at")
    }
}
