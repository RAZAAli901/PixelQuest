package com.pixelquest.app.darood.data.remote

import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.safeSupabaseCall
import com.pixelquest.app.darood.domain.MemberStatus
import com.pixelquest.app.darood.domain.PartyMember
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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

/** What invite_member answered. It never says whether a name exists. */
enum class InviteOutcome { SENT, ALREADY_MEMBER }

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

    /** A new party led by the account; its id. Refusals: [com.pixelquest.app.darood.domain.DaroodRefusal]. */
    suspend fun createParty(name: String, maxMembers: Int, milestone: Long?): SupabaseResult<String>

    /** The leader invites [username]. */
    suspend fun invite(partyId: String, username: String): SupabaseResult<InviteOutcome>

    /** Accepts or declines an invitation: the account's new status. */
    suspend fun respondToInvite(partyId: String, accept: Boolean): SupabaseResult<MemberStatus>

    suspend fun leaveParty(partyId: String): SupabaseResult<Unit>

    /** The leader removes a member or withdraws an invitation. */
    suspend fun removeMember(partyId: String, userId: String): SupabaseResult<Unit>

    /** The leader sets or raises the milestone: the milestone now. */
    suspend fun setMilestone(partyId: String, target: Long): SupabaseResult<Long>
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

    private suspend inline fun <reified T : Any> call(function: String, crossinline arguments: JsonObjectBuilder.() -> Unit): SupabaseResult<T> {
        if (account.id() == null) return signedOut()
        return safeSupabaseCall { postgrest.rpc(function, buildJsonObject(arguments)).decodeAs<T>() }
    }

    override suspend fun createParty(name: String, maxMembers: Int, milestone: Long?): SupabaseResult<String> =
        call("create_party") {
            put("name", name.trim())
            put("max_members", maxMembers)
            put("milestone", milestone)
        }

    override suspend fun invite(partyId: String, username: String): SupabaseResult<InviteOutcome> =
        call<PartyStatusReply>("invite_member") {
            put("party_id", partyId)
            put("username", username)
        }.map { if (it.status == "already_member") InviteOutcome.ALREADY_MEMBER else InviteOutcome.SENT }

    override suspend fun respondToInvite(partyId: String, accept: Boolean): SupabaseResult<MemberStatus> =
        call<PartyStatusReply>("respond_invite") {
            put("party_id", partyId)
            put("accept", accept)
        }.map { statusOf(it.status) }

    override suspend fun leaveParty(partyId: String): SupabaseResult<Unit> =
        call<PartyStatusReply>("leave_party") { put("party_id", partyId) }.map { }

    override suspend fun removeMember(partyId: String, userId: String): SupabaseResult<Unit> =
        call<PartyStatusReply>("remove_member") {
            put("party_id", partyId)
            put("user_id", userId)
        }.map { }

    override suspend fun setMilestone(partyId: String, target: Long): SupabaseResult<Long> =
        call<MilestoneReply>("set_milestone") {
            put("party_id", partyId)
            put("target", target)
        }.map { it.milestone }

    internal companion object {
        const val PARTIES = "parties"
        const val MEMBERS = "party_members"
        const val USERNAMES = "usernames"
        val PARTY_COLUMNS = listOf("id", "name", "leader_id", "max_members", "milestone", "milestone_set_at", "completed_at", "created_at")
        val MEMBER_COLUMNS = listOf("party_id", "user_id", "role", "status", "invited_at", "joined_at")
    }
}

/** The value of a success, changed; any failure as it is. */
internal inline fun <T, R> SupabaseResult<T>.map(transform: (T) -> R): SupabaseResult<R> = when (this) {
    is SupabaseResult.Success -> SupabaseResult.Success(transform(data))
    is SupabaseResult.NetworkError -> this
    is SupabaseResult.AuthError -> this
    is SupabaseResult.ServerError -> this
    is SupabaseResult.UnknownError -> this
}
