package com.pixelquest.app.darood.data.remote

import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.safeSupabaseCall
import com.pixelquest.app.darood.domain.DayTotal
import com.pixelquest.app.darood.domain.DaroodEntry
import com.pixelquest.app.darood.domain.DaroodRefusal
import com.pixelquest.app.darood.domain.MemberStats
import com.pixelquest.app.darood.domain.MemberStatus
import com.pixelquest.app.darood.domain.PartyMember
import com.pixelquest.app.darood.domain.PartyStats
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.time.LocalDate
import kotlinx.serialization.json.JsonObject
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

    /** The leader renames the party. */
    suspend fun renameParty(partyId: String, name: String): SupabaseResult<Unit>

    /** The leader puts the party on the race board, or takes it off. */
    suspend fun setRaceBoard(partyId: String, on: Boolean): SupabaseResult<Unit>

    /** The leader hands the leadership to [userId], an active member; the leader stays as a member. */
    suspend fun transferLeadership(partyId: String, userId: String): SupabaseResult<Unit>

    /** The leader deletes the party: it's gone for everyone. */
    suspend fun deleteParty(partyId: String): SupabaseResult<Unit>

    /** Each poster's total for [day] in a party (party_day_stats), as its members may see it. */
    suspend fun fetchDayTotals(partyId: String, day: LocalDate): SupabaseResult<List<DayTotal>>

    /** The race board: the parties whose leaders put them on it, name and progress only. */
    suspend fun fetchRaceBoard(): SupabaseResult<List<com.pixelquest.app.darood.domain.RaceLine>>

    /** The account's own total for each day it posted on in a party (party_day_stats). */
    suspend fun fetchMyDays(partyId: String): SupabaseResult<Map<LocalDate, Long>>

    /**
     * Posts an entry as the account; the entry as the server stored it. [clientId] is its own key:
     * posting it again (an answer lost on the way back) returns the same entry, never a second.
     */
    suspend fun addEntry(partyId: String, amount: Int, localDate: LocalDate, tz: String, clientId: String): SupabaseResult<DaroodEntry>

    /**
     * A page of a party's entries, newest first: the [limit] newest, or with [beforeId] the [limit]
     * before that entry. Ids only grow, in the order entries reached the server.
     */
    suspend fun fetchEntries(partyId: String, beforeId: Long? = null, limit: Int = PAGE_SIZE): SupabaseResult<List<DaroodEntry>>

    /** The numbers of the given parties (those the account is active in; others have no row). */
    suspend fun fetchStats(partyIds: List<String>): SupabaseResult<List<PartyStats>>

    /** Each poster's part of a party's total, largest first. */
    suspend fun fetchMemberStats(partyId: String): SupabaseResult<List<MemberStats>>

    companion object {
        /** Entries per page in the chat. */
        const val PAGE_SIZE = 50
    }
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

    override suspend fun deleteParty(partyId: String): SupabaseResult<Unit> =
        call<PartyStatusReply>("delete_party") { put("party_id", partyId) }.map { }

    override suspend fun fetchRaceBoard(): SupabaseResult<List<com.pixelquest.app.darood.domain.RaceLine>> {
        if (account.id() == null) return signedOut()
        return safeSupabaseCall {
            postgrest[RACE_BOARD].select(Columns.list("name", "member_count", "total", "milestone", "progress", "completed_at", "mine"))
                .decodeList<RaceLineDto>()
                .map { com.pixelquest.app.darood.domain.RaceLine(it.name, it.memberCount, it.total, it.milestone, it.progress, it.completedAt?.let(::parseInstant), it.mine) }
        }
    }

    override suspend fun fetchMyDays(partyId: String): SupabaseResult<Map<LocalDate, Long>> {
        val me = account.id() ?: return signedOut()
        return safeSupabaseCall {
            postgrest[DAY_STATS].select(Columns.list("local_date", "total")) {
                filter {
                    eq("party_id", partyId)
                    eq("user_id", me)
                }
            }.decodeList<MyDayDto>().associate { LocalDate.parse(it.localDate) to it.total }
        }
    }

    override suspend fun fetchDayTotals(partyId: String, day: LocalDate): SupabaseResult<List<DayTotal>> {
        if (account.id() == null) return signedOut()
        return safeSupabaseCall {
            postgrest[DAY_STATS].select(Columns.list("user_id", "total")) {
                filter {
                    eq("party_id", partyId)
                    eq("local_date", day.toString())
                }
            }.decodeList<DayTotalDto>().map { DayTotal(it.userId, it.total) }
        }
    }

    override suspend fun setMilestone(partyId: String, target: Long): SupabaseResult<Long> =
        call<MilestoneReply>("set_milestone") {
            put("party_id", partyId)
            put("target", target)
        }.map { it.milestone }

    override suspend fun renameParty(partyId: String, name: String): SupabaseResult<Unit> {
        if (account.id() == null) return signedOut()
        val renamed = safeSupabaseCall {
            postgrest[PARTIES].update({ set("name", name.trim()) }) {
                select(Columns.list("id"))
                filter { eq("id", partyId) }
            }.decodeList<JsonObject>()
        }
        // Row-level security lets only the leader's change through; anyone else's changes no row,
        // without an error, so that's read as the refusal it is.
        return if (renamed is SupabaseResult.Success && renamed.data.isEmpty()) {
            SupabaseResult.ServerError(403, DaroodRefusal.NOT_LEADER.token)
        } else {
            renamed.map { }
        }
    }

    override suspend fun setRaceBoard(partyId: String, on: Boolean): SupabaseResult<Unit> {
        if (account.id() == null) return signedOut()
        val changed = safeSupabaseCall {
            postgrest[PARTIES].update({ set("is_public_race", on) }) {
                select(Columns.list("id"))
                filter { eq("id", partyId) }
            }.decodeList<JsonObject>()
        }
        // As with renaming: anyone but the leader changes no row, without an error.
        return if (changed is SupabaseResult.Success && changed.data.isEmpty()) {
            SupabaseResult.ServerError(403, DaroodRefusal.NOT_LEADER.token)
        } else {
            changed.map { }
        }
    }

    override suspend fun transferLeadership(partyId: String, userId: String): SupabaseResult<Unit> =
        call<JsonObject>("transfer_leadership") {
            put("party_id", partyId)
            put("user_id", userId)
        }.map { }

    override suspend fun addEntry(partyId: String, amount: Int, localDate: LocalDate, tz: String, clientId: String): SupabaseResult<DaroodEntry> {
        if (account.id() == null) return signedOut()
        return safeSupabaseCall {
            val arguments = NewDaroodEntryDto(partyId, amount, localDate.toString(), tz, clientId)
            postgrest.rpc("add_darood_entry", kotlinx.serialization.json.Json.encodeToJsonElement(NewDaroodEntryDto.serializer(), arguments) as kotlinx.serialization.json.JsonObject)
                .decodeAs<DaroodEntryDto>().toDomain()
        }
    }

    override suspend fun fetchEntries(partyId: String, beforeId: Long?, limit: Int): SupabaseResult<List<DaroodEntry>> {
        if (account.id() == null) return signedOut()
        return safeSupabaseCall {
            postgrest[ENTRIES].select(Columns.list(ENTRY_COLUMNS)) {
                filter {
                    eq("party_id", partyId)
                    if (beforeId != null) lt("id", beforeId)
                }
                order("id", Order.DESCENDING)
                limit(limit.toLong())
            }.decodeList<DaroodEntryDto>().map { it.toDomain() }
        }
    }

    override suspend fun fetchStats(partyIds: List<String>): SupabaseResult<List<PartyStats>> {
        if (account.id() == null) return signedOut()
        if (partyIds.isEmpty()) return SupabaseResult.Success(emptyList())
        return safeSupabaseCall {
            postgrest[PARTY_STATS].select(Columns.list(STATS_COLUMNS)) {
                filter { isIn("party_id", partyIds) }
            }.decodeList<PartyStatsDto>().map { it.toDomain() }
        }
    }

    override suspend fun fetchMemberStats(partyId: String): SupabaseResult<List<MemberStats>> {
        if (account.id() == null) return signedOut()
        return safeSupabaseCall {
            postgrest[MEMBER_STATS].select(Columns.list(MEMBER_STATS_COLUMNS)) {
                filter { eq("party_id", partyId) }
                order("total", Order.DESCENDING)
            }.decodeList<MemberStatsDto>().map { it.toDomain() }
        }
    }

    internal companion object {
        const val PARTIES = "parties"
        const val DAY_STATS = "party_day_stats"
        const val RACE_BOARD = "race_board"
        const val MEMBERS = "party_members"
        const val USERNAMES = "usernames"
        const val ENTRIES = "darood_entries"
        const val PARTY_STATS = "party_stats"
        const val MEMBER_STATS = "party_member_stats"
        val STATS_COLUMNS = listOf("party_id", "total", "entry_count", "member_count", "last_entry_date")
        val MEMBER_STATS_COLUMNS = listOf("party_id", "user_id", "username", "total", "active_days", "last_entry_date")
        val ENTRY_COLUMNS = listOf("id", "party_id", "user_id", "amount", "local_date", "tz", "created_at", "client_id")
        val PARTY_COLUMNS = listOf("id", "name", "leader_id", "max_members", "milestone", "milestone_set_at", "completed_at", "created_at", "is_public_race")
        val MEMBER_COLUMNS = listOf("party_id", "user_id", "role", "status", "invited_at", "joined_at", "updated_at")
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
