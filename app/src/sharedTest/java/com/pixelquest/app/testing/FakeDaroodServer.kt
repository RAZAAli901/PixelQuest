package com.pixelquest.app.testing

import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.darood.data.remote.DaroodEntryDto
import com.pixelquest.app.darood.data.remote.DaroodPartyRemoteDataSource
import com.pixelquest.app.darood.data.remote.InviteOutcome
import com.pixelquest.app.darood.data.remote.PartyDto
import com.pixelquest.app.darood.data.remote.PartyMemberDto
import com.pixelquest.app.darood.data.remote.RemoteParty
import com.pixelquest.app.darood.data.remote.toDomain
import com.pixelquest.app.darood.domain.DaroodEntry
import com.pixelquest.app.darood.domain.DaroodRules
import com.pixelquest.app.darood.domain.MemberStats
import com.pixelquest.app.darood.domain.MemberStatus
import com.pixelquest.app.darood.domain.PartyMember
import com.pixelquest.app.darood.domain.PartyStats
import java.io.IOException
import java.time.Instant
import java.time.LocalDate

/**
 * Darood Party's server in memory, keeping its main rules (supabase/schema.md, Darood Parties):
 * who sees what, the leader's rights, the member limit, entry amounts, and finishing. [me] is the
 * signed-in account (null: signed out); [offline] fails every call as a network error.
 */
class FakeDaroodServer(var me: String? = "me") : DaroodPartyRemoteDataSource {
    var offline = false

    /** The next entry reaches the server, but its answer is lost on the way back. */
    var loseNextAnswer = false
    var now: Instant = Instant.parse("2026-10-19T10:00:00Z")

    val parties = linkedMapOf<String, PartyDto>()
    val members = mutableListOf<PartyMemberDto>()
    val entries = mutableListOf<DaroodEntryDto>()
    val usernames = mutableMapOf<String, String>()
    val calls = mutableListOf<String>()

    /** Invitations sent, as (inviter, when): 20 an hour each at most, as the server allows. */
    val inviteLog = mutableListOf<Pair<String, Instant>>()
    private var nextParty = 1
    private var nextEntry = 1L

    private fun <T> answer(name: String, block: (String) -> SupabaseResult<T>): SupabaseResult<T> {
        calls += name
        if (offline) return SupabaseResult.NetworkError(IOException("offline"))
        val account = me ?: return SupabaseResult.AuthError(IllegalStateException("signed out"))
        return block(account)
    }

    private fun refuse(token: String, code: Int = 400): SupabaseResult<Nothing> = SupabaseResult.ServerError(code, token)

    private fun membership(party: String, user: String) = members.firstOrNull { it.partyId == party && it.userId == user }

    private fun isActive(party: String, user: String) = membership(party, user)?.status == "active"

    private fun setStatus(party: String, user: String, status: String, joined: Boolean = false) {
        val i = members.indexOfFirst { it.partyId == party && it.userId == user }
        members[i] = members[i].copy(status = status, joinedAt = if (joined) now.toString() else members[i].joinedAt, updatedAt = now.toString())
    }

    fun total(party: String): Long = entries.filter { it.partyId == party }.sumOf { it.amount.toLong() }

    /** A party as if made on the server, led by [leader], with [active] members already in it. */
    fun seedParty(name: String, leader: String, vararg active: String, milestone: Long? = null, maxMembers: Int = 10): String {
        val id = "party-${nextParty++}"
        parties[id] = PartyDto(id, name, leader, maxMembers, milestone, milestone?.let { now.toString() }, null, now.toString())
        members += PartyMemberDto(id, leader, "leader", "active", now.toString(), now.toString())
        active.forEach { members += PartyMemberDto(id, it, "member", "active", now.toString(), now.toString()) }
        return id
    }

    /** An entry as if another member posted it. */
    fun seedEntry(party: String, user: String, amount: Int, day: LocalDate = LocalDate.of(2026, 10, 19)): DaroodEntryDto =
        DaroodEntryDto(nextEntry++, party, user, amount, day.toString(), "UTC", now.toString()).also { entries += it }

    override suspend fun fetchMyParties(): SupabaseResult<List<RemoteParty>> = answer("fetchMyParties") { account ->
        SupabaseResult.Success(
            members.filter { it.userId == account && it.status in setOf("active", "invited") }
                .mapNotNull { m -> parties[m.partyId]?.let { RemoteParty(it, m) } }
        )
    }

    override suspend fun fetchMembers(partyId: String): SupabaseResult<List<PartyMember>> = answer("fetchMembers") { account ->
        if (!isActive(partyId, account)) return@answer SupabaseResult.Success(emptyList())
        SupabaseResult.Success(members.filter { it.partyId == partyId }.map { it.toDomain(usernames[it.userId]) })
    }

    override suspend fun createParty(name: String, maxMembers: Int, milestone: Long?): SupabaseResult<String> = answer("createParty") { account ->
        val trimmed = name.trim()
        when {
            account !in usernames -> refuse("username_required")
            trimmed.length !in DaroodRules.NAME_MIN..DaroodRules.NAME_MAX -> refuse("new row violates check constraint \"party_name_length\"")
            maxMembers !in DaroodRules.MEMBERS_MIN..DaroodRules.MEMBERS_MAX -> refuse("new row violates check constraint \"party_member_limit\"")
            else -> SupabaseResult.Success(seedParty(trimmed, account, milestone = milestone, maxMembers = maxMembers))
        }
    }

    override suspend fun invite(partyId: String, username: String): SupabaseResult<InviteOutcome> = answer("invite") { account ->
        val party = parties[partyId]
        if (party?.leaderId != account) return@answer refuse("not_leader", 403)
        if (party.completedAt != null) return@answer refuse("party_closed")
        if (inviteLog.count { (who, at) -> who == account && at.isAfter(now.minusSeconds(3600)) } >= 20) return@answer refuse("too_many_invites")
        inviteLog += account to now
        val invitee = usernames.entries.firstOrNull { it.value.equals(username.trim(), ignoreCase = true) }?.key
        val existing = invitee?.let { membership(partyId, it) }
        if (existing?.status == "active") return@answer SupabaseResult.Success(InviteOutcome.ALREADY_MEMBER)
        if (existing?.status == "invited") return@answer SupabaseResult.Success(InviteOutcome.SENT)
        val taken = members.count { it.partyId == partyId && it.status in setOf("invited", "active") }
        if (taken >= minOf(party.maxMembers, 30)) return@answer refuse("party_full")
        if (invitee == null) return@answer SupabaseResult.Success(InviteOutcome.SENT)
        if (existing != null) setStatus(partyId, invitee, "invited") else members += PartyMemberDto(partyId, invitee, "member", "invited", now.toString(), null)
        SupabaseResult.Success(InviteOutcome.SENT)
    }

    override suspend fun respondToInvite(partyId: String, accept: Boolean): SupabaseResult<MemberStatus> = answer("respondToInvite") { account ->
        if (membership(partyId, account)?.status != "invited") return@answer refuse("no_invitation")
        setStatus(partyId, account, if (accept) "active" else "declined", joined = accept)
        SupabaseResult.Success(if (accept) MemberStatus.ACTIVE else MemberStatus.DECLINED)
    }

    override suspend fun leaveParty(partyId: String): SupabaseResult<Unit> = answer("leaveParty") { account ->
        val mine = membership(partyId, account)
        when {
            mine?.status != "active" -> refuse("not_a_member")
            mine.role == "leader" -> refuse("leader_must_hand_over")
            else -> SupabaseResult.Success(setStatus(partyId, account, "left"))
        }
    }

    override suspend fun removeMember(partyId: String, userId: String): SupabaseResult<Unit> = answer("removeMember") { account ->
        if (parties[partyId]?.leaderId != account) return@answer refuse("not_leader", 403)
        val target = membership(partyId, userId)
        if (target == null || target.role == "leader" || target.status !in setOf("active", "invited")) return@answer refuse("not_a_member")
        SupabaseResult.Success(setStatus(partyId, userId, "removed"))
    }

    override suspend fun renameParty(partyId: String, name: String): SupabaseResult<Unit> = answer("renameParty") { account ->
        val party = parties[partyId]
        val trimmed = name.trim()
        when {
            party?.leaderId != account -> refuse("not_leader", 403)
            trimmed.length !in DaroodRules.NAME_MIN..DaroodRules.NAME_MAX -> refuse("new row violates check constraint \"party_name_length\"")
            else -> SupabaseResult.Success(Unit).also { parties[partyId] = party.copy(name = trimmed) }
        }
    }

    override suspend fun setRaceBoard(partyId: String, on: Boolean): SupabaseResult<Unit> = answer("setRaceBoard") { account ->
        val party = parties[partyId]
        if (party?.leaderId != account) return@answer refuse("not_leader", 403)
        parties[partyId] = party.copy(isPublicRace = on)
        SupabaseResult.Success(Unit)
    }

    override suspend fun transferLeadership(partyId: String, userId: String): SupabaseResult<Unit> = answer("transferLeadership") { account ->
        val party = parties[partyId]
        if (party?.leaderId != account) return@answer refuse("not_leader", 403)
        val target = membership(partyId, userId)
        if (target?.status != "active" || target.role != "member") return@answer refuse("not_a_member")
        members.replaceAll {
            when {
                it.partyId != partyId -> it
                it.userId == account -> it.copy(role = "member")
                it.userId == userId -> it.copy(role = "leader")
                else -> it
            }
        }
        parties[partyId] = party.copy(leaderId = userId)
        SupabaseResult.Success(Unit)
    }

    /** Parties their leader deleted: hidden from everyone, as on the server. */
    val deleted = linkedMapOf<String, PartyDto>()

    override suspend fun deleteParty(partyId: String): SupabaseResult<Unit> = answer("deleteParty") { account ->
        val party = parties[partyId] ?: deleted[partyId]
        if (party?.leaderId != account) return@answer refuse("not_leader", 403)
        parties.remove(partyId)?.let { deleted[partyId] = it }
        SupabaseResult.Success(Unit)
    }

    override suspend fun fetchDayTotals(partyId: String, day: LocalDate): SupabaseResult<List<com.pixelquest.app.darood.domain.DayTotal>> = answer("fetchDayTotals") { account ->
        if (!isActive(partyId, account)) return@answer SupabaseResult.Success(emptyList())
        SupabaseResult.Success(
            entries.filter { it.partyId == partyId && it.localDate == day.toString() }
                .groupBy { it.userId }
                .map { (user, theirs) -> com.pixelquest.app.darood.domain.DayTotal(user, theirs.sumOf { it.amount.toLong() }) }
        )
    }

    override suspend fun fetchRaceBoard(): SupabaseResult<List<com.pixelquest.app.darood.domain.RaceLine>> = answer("fetchRaceBoard") { account ->
        SupabaseResult.Success(
            parties.values.filter { it.isPublicRace && it.milestone != null }.map { party ->
                val total = total(party.id)
                com.pixelquest.app.darood.domain.RaceLine(
                    name = party.name,
                    memberCount = members.count { it.partyId == party.id && it.status == "active" },
                    total = total,
                    milestone = party.milestone!!,
                    progress = minOf(1f, total.toFloat() / party.milestone),
                    completedAt = party.completedAt?.let(Instant::parse),
                    mine = isActive(party.id, account)
                )
            }
        )
    }

    override suspend fun fetchMyDays(partyId: String): SupabaseResult<Map<LocalDate, Long>> = answer("fetchMyDays") { account ->
        if (!isActive(partyId, account)) return@answer SupabaseResult.Success(emptyMap())
        SupabaseResult.Success(
            entries.filter { it.partyId == partyId && it.userId == account }
                .groupBy { LocalDate.parse(it.localDate) }
                .mapValues { (_, theirs) -> theirs.sumOf { it.amount.toLong() } }
        )
    }

    override suspend fun setMilestone(partyId: String, target: Long): SupabaseResult<Long> = answer("setMilestone") { account ->
        val party = parties[partyId]
        when {
            party?.leaderId != account -> refuse("not_leader", 403)
            party.completedAt != null -> refuse("party_closed")
            target <= total(partyId) -> refuse("milestone_too_low")
            else -> {
                parties[partyId] = party.copy(milestone = target, milestoneSetAt = now.toString())
                SupabaseResult.Success(target)
            }
        }
    }

    override suspend fun addEntry(partyId: String, amount: Int, localDate: LocalDate, tz: String, clientId: String): SupabaseResult<DaroodEntry> = answer("addEntry") { account ->
        // The same key again: the entry already stored (add_darood_entry). Its answer can be lost too.
        entries.firstOrNull { it.userId == account && it.clientId == clientId }?.let { already ->
            if (loseNextAnswer) {
                loseNextAnswer = false
                return@answer SupabaseResult.NetworkError(IOException("the answer was lost"))
            }
            return@answer SupabaseResult.Success(already.toDomain())
        }
        val party = parties[partyId]
        when {
            party == null || party.completedAt != null -> refuse("party_closed")
            !isActive(partyId, account) -> refuse("not_a_member", 403)
            amount !in DaroodRules.AMOUNT_MIN..DaroodRules.AMOUNT_MAX -> refuse("new row violates check constraint \"darood_entry_amount\"")
            entries.filter { it.partyId == partyId && it.userId == account && it.localDate == localDate.toString() }.sumOf { it.amount } + amount > DaroodRules.DAILY_MAX ->
                refuse("daily_limit")
            else -> {
                val stored = DaroodEntryDto(nextEntry++, partyId, account, amount, localDate.toString(), tz, now.toString(), clientId)
                entries += stored
                val milestone = party.milestone
                if (milestone != null && total(partyId) >= milestone) parties[partyId] = party.copy(completedAt = now.toString())
                if (loseNextAnswer) {
                    loseNextAnswer = false
                    return@answer SupabaseResult.NetworkError(IOException("the answer was lost"))
                }
                SupabaseResult.Success(stored.toDomain())
            }
        }
    }

    override suspend fun fetchEntries(partyId: String, beforeId: Long?, limit: Int): SupabaseResult<List<DaroodEntry>> = answer("fetchEntries") { account ->
        if (!isActive(partyId, account)) return@answer SupabaseResult.Success(emptyList())
        SupabaseResult.Success(
            entries.filter { it.partyId == partyId && (beforeId == null || it.id < beforeId) }
                .sortedByDescending { it.id }.take(limit).map { it.toDomain() }
        )
    }

    override suspend fun fetchStats(partyIds: List<String>): SupabaseResult<List<PartyStats>> = answer("fetchStats") { account ->
        SupabaseResult.Success(
            partyIds.filter { isActive(it, account) }.map { id ->
                val own = entries.filter { it.partyId == id }
                PartyStats(id, total(id), own.size, members.count { it.partyId == id && it.status == "active" }, own.maxOfOrNull { LocalDate.parse(it.localDate) })
            }
        )
    }

    override suspend fun fetchMemberStats(partyId: String): SupabaseResult<List<MemberStats>> = answer("fetchMemberStats") { account ->
        if (!isActive(partyId, account)) return@answer SupabaseResult.Success(emptyList())
        SupabaseResult.Success(
            entries.filter { it.partyId == partyId }.groupBy { it.userId }.map { (user, posted) ->
                MemberStats(partyId, user, user?.let(usernames::get), posted.sumOf { it.amount.toLong() }, posted.map { it.localDate }.distinct().size, posted.maxOf { LocalDate.parse(it.localDate) })
            }.sortedByDescending { it.total }
        )
    }
}
