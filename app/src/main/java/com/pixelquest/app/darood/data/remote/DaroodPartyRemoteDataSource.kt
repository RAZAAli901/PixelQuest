package com.pixelquest.app.darood.data.remote

import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.safeSupabaseCall
import com.pixelquest.app.darood.domain.DayTotal
import com.pixelquest.app.darood.domain.DaroodEntry
import com.pixelquest.app.darood.domain.DaroodRefusal
import com.pixelquest.app.darood.domain.MemberStats
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.PartyMember
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
import com.pixelquest.app.party.data.remote.InviteOutcome
import com.pixelquest.app.party.data.remote.MemberNameDto
import com.pixelquest.app.party.data.remote.PartyDto
import com.pixelquest.app.party.data.remote.PartyMemberDto
import com.pixelquest.app.party.data.remote.PartyRemoteDataSource
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.party.data.remote.toDomain
import com.pixelquest.app.party.data.remote.PartyStatusReply
import com.pixelquest.app.party.data.remote.RemoteParty
import com.pixelquest.app.party.data.remote.parseInstant
import com.pixelquest.app.party.data.remote.statusOf

/** The signed-in account, as Darood Party asks for it. */
fun interface DaroodAccount {
    /** The account's id, or null when signed out. */
    fun id(): String?
}

@Singleton
class SupabaseDaroodAccount @Inject constructor(private val auth: Auth) : DaroodAccount {
    override fun id(): String? = auth.currentUserOrNull()?.id
}

/**
 * Darood Party on Supabase, through the app's one Postgrest client: every party's calls
 * ([PartyRemoteDataSource]), and Darood's own. Row-level security decides
 * what each call may see (supabase/schema.md, Darood Parties); every call is wrapped in
 * [safeSupabaseCall], like the rest of the app's Supabase calls.
 */
interface DaroodPartyRemoteDataSource : PartyRemoteDataSource {
    /** A new party led by the account; its id. Refusals: [com.pixelquest.app.darood.domain.DaroodRefusal]. */
    suspend fun createParty(name: String, maxMembers: Int, milestone: Long?): SupabaseResult<String>

    /** The leader sets or raises the milestone: the milestone now. */
    suspend fun setMilestone(partyId: String, target: Long): SupabaseResult<Long>

    /** The leader puts the party on the race board, or takes it off. */
    suspend fun setRaceBoard(partyId: String, on: Boolean): SupabaseResult<Unit>

    /** Each poster's total for [day] in a party (party_day_stats), as its members may see it. */
    suspend fun fetchDayTotals(partyId: String, day: LocalDate): SupabaseResult<List<DayTotal>>

    /** The race board: the parties whose leaders put them on it, name and progress only. */
    suspend fun fetchRaceBoard(): SupabaseResult<List<com.pixelquest.app.darood.domain.RaceLine>>

    /** The account's own total for each day it posted on in a party (party_day_stats). */
    suspend fun fetchMyDays(partyId: String): SupabaseResult<Map<LocalDate, Long>>

    /**
     * Closes the week from [weekStart] (a Monday) if it's over in the party's zone, storing its
     * places once; asking again changes nothing. How many members it ranked; week_not_over as a
     * refusal when the week is still running there.
     */
    suspend fun closeWeek(partyId: String, weekStart: LocalDate): SupabaseResult<Int>

    /** The party's goals, oldest first (docs/darood-weekly.md, Goals that go on). */
    suspend fun fetchGoals(partyId: String): SupabaseResult<List<com.pixelquest.app.darood.domain.PartyGoal>>

    /** The party's closed weeks from [since] on, newest first. */
    suspend fun fetchWeekResults(partyId: String, since: LocalDate): SupabaseResult<List<com.pixelquest.app.darood.domain.WeekResult>>

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
            // Every column, not a list: the app reads party_type where the server has it, and still
            // reads the parties of a server without it (before 20261019000031), each a Darood Party.
            val parties = postgrest[PARTIES].select(Columns.ALL) {
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

    override suspend fun createParty(name: String, maxMembers: Int, type: PartyType): SupabaseResult<String> =
        call<String>("create_party") {
            put("name", name.trim())
            put("max_members", maxMembers)
            put("party_type", type.wire)
        }.also { if (it is SupabaseResult.Success) setWeekZone(it.data) }

    override suspend fun createParty(name: String, maxMembers: Int, milestone: Long?): SupabaseResult<String> =
        call<String>("create_party") {
            put("name", name.trim())
            put("max_members", maxMembers)
            put("milestone", milestone)
        }.also { if (it is SupabaseResult.Success) setWeekZone(it.data) }

    /**
     * A new party's week runs in its leader's zone (docs/darood-weekly.md): sent right after it's
     * made. Best effort: without it the week runs in UTC, and the leader can set it later.
     */
    private suspend fun setWeekZone(partyId: String) {
        safeSupabaseCall {
            postgrest.rpc("set_party_week_tz", buildJsonObject {
                put("party_id", partyId)
                put("tz", java.time.ZoneId.systemDefault().id)
            })
        }
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

    override suspend fun closeWeek(partyId: String, weekStart: LocalDate): SupabaseResult<Int> =
        call<Int>("close_darood_week") {
            put("party_id", partyId)
            put("week_start", weekStart.toString())
        }

    override suspend fun fetchWeekResults(partyId: String, since: LocalDate): SupabaseResult<List<com.pixelquest.app.darood.domain.WeekResult>> {
        if (account.id() == null) return signedOut()
        return safeSupabaseCall {
            postgrest[WEEK_RESULTS].select(Columns.list("party_id", "week_start", "user_id", "position", "week_total", "active_days", "closed_at")) {
                filter {
                    eq("party_id", partyId)
                    gte("week_start", since.toString())
                }
                order("week_start", Order.DESCENDING)
            }.decodeList<WeekResultDto>().toWeeks()
        }
    }

    override suspend fun fetchGoals(partyId: String): SupabaseResult<List<com.pixelquest.app.darood.domain.PartyGoal>> {
        if (account.id() == null) return signedOut()
        return safeSupabaseCall {
            postgrest[GOALS].select(Columns.list("sequence", "target", "set_by", "set_at", "reached_at")) {
                filter { eq("party_id", partyId) }
                order("sequence", Order.ASCENDING)
            }.decodeList<PartyGoalDto>().map { it.toDomain() }
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
        const val WEEK_RESULTS = "darood_week_results"
        const val GOALS = "party_milestones"
        val STATS_COLUMNS = listOf("party_id", "total", "entry_count", "member_count", "last_entry_date")
        val MEMBER_STATS_COLUMNS = listOf("party_id", "user_id", "username", "total", "active_days", "last_entry_date")
        val ENTRY_COLUMNS = listOf("id", "party_id", "user_id", "amount", "local_date", "tz", "created_at", "client_id")
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
