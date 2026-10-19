package com.pixelquest.app.darood.data.remote

import com.pixelquest.app.darood.domain.DaroodEntry
import com.pixelquest.app.darood.domain.EntryState
import com.pixelquest.app.darood.domain.MemberStats
import com.pixelquest.app.darood.domain.MemberStatus
import com.pixelquest.app.darood.domain.Party
import com.pixelquest.app.darood.domain.PartyMember
import com.pixelquest.app.darood.domain.PartyRole
import com.pixelquest.app.darood.domain.PartyStats
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

// Darood Party's rows as Supabase sends them (supabase/schema.md, Darood Parties), and their
// mappings to the domain models. Timestamps arrive as ISO strings with an offset; days as yyyy-MM-dd.

/** A row of public.parties. */
@Serializable
data class PartyDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("leader_id") val leaderId: String? = null,
    @SerialName("max_members") val maxMembers: Int,
    @SerialName("milestone") val milestone: Long? = null,
    @SerialName("milestone_set_at") val milestoneSetAt: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("is_public_race") val isPublicRace: Boolean = false
)

/** A row of public.party_day_stats, as read: one poster's total for a day. */
@Serializable
data class DayTotalDto(
    @SerialName("user_id") val userId: String? = null,
    @SerialName("total") val total: Long
)

/** A row of public.party_day_stats for the account itself: its total on one day. */
@Serializable
data class MyDayDto(
    @SerialName("local_date") val localDate: String,
    @SerialName("total") val total: Long
)

/** A line of public.race_board, as read. */
@Serializable
data class RaceLineDto(
    @SerialName("name") val name: String,
    @SerialName("member_count") val memberCount: Int,
    @SerialName("total") val total: Long,
    @SerialName("milestone") val milestone: Long,
    @SerialName("progress") val progress: Float,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("mine") val mine: Boolean = false
)

/** A row of public.party_members. */
@Serializable
data class PartyMemberDto(
    @SerialName("party_id") val partyId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("role") val role: String,
    @SerialName("status") val status: String,
    @SerialName("invited_at") val invitedAt: String? = null,
    @SerialName("joined_at") val joinedAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

/** A row of public.darood_entries, as read. */
@Serializable
data class DaroodEntryDto(
    @SerialName("id") val id: Long,
    @SerialName("party_id") val partyId: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("amount") val amount: Int,
    @SerialName("local_date") val localDate: String,
    @SerialName("tz") val tz: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("client_id") val clientId: String? = null
)

/**
 * What a member sends to post an entry (add_darood_entry's arguments): the server fills in who and
 * when. [clientId] is the entry's own key: sent again, the same entry comes back, never a second.
 */
@Serializable
data class NewDaroodEntryDto(
    @SerialName("party_id") val partyId: String,
    @SerialName("amount") val amount: Int,
    @SerialName("local_date") val localDate: String,
    @SerialName("tz") val tz: String,
    @SerialName("client_id") val clientId: String
)

/** A row of public.party_stats. */
@Serializable
data class PartyStatsDto(
    @SerialName("party_id") val partyId: String,
    @SerialName("total") val total: Long,
    @SerialName("entry_count") val entryCount: Int,
    @SerialName("member_count") val memberCount: Int,
    @SerialName("last_entry_date") val lastEntryDate: String? = null
)

/** A row of public.party_member_stats. */
@Serializable
data class MemberStatsDto(
    @SerialName("party_id") val partyId: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("username") val username: String? = null,
    @SerialName("total") val total: Long,
    @SerialName("active_days") val activeDays: Int,
    @SerialName("last_entry_date") val lastEntryDate: String? = null
)

/** A row of public.usernames that the reader may see (their own, or a co-member's). */
@Serializable
data class MemberNameDto(
    @SerialName("user_id") val userId: String,
    @SerialName("username") val username: String
)

/** The answer of invite_member, respond_invite, leave_party and remove_member. */
@Serializable
data class PartyStatusReply(@SerialName("status") val status: String)

/** set_milestone's answer. */
@Serializable
data class MilestoneReply(
    @SerialName("milestone") val milestone: Long,
    @SerialName("milestone_set_at") val milestoneSetAt: String
)

/**
 * A timestamp as Supabase sends it: ISO from the REST API ("2026-10-19T10:00:00.5+00:00"), or
 * Postgres's own text form from Realtime ("2026-10-19 10:00:00.5+00"), which ISO doesn't read.
 */
internal fun parseInstant(value: String): Instant {
    var iso = value.trim().replace(' ', 'T')
    val shortOffset = Regex("([+-])(\\d{2})(\\d{2})?$").find(iso)
    if (shortOffset != null && iso[shortOffset.range.first - 1].isDigit()) {
        val (sign, hours, minutes) = shortOffset.destructured
        iso = iso.substring(0, shortOffset.range.first) + "$sign$hours:${minutes.ifEmpty { "00" }}"
    }
    return OffsetDateTime.parse(iso).toInstant()
}

internal fun roleOf(value: String): PartyRole = if (value == "leader") PartyRole.LEADER else PartyRole.MEMBER

internal fun statusOf(value: String): MemberStatus = when (value) {
    "active" -> MemberStatus.ACTIVE
    "invited" -> MemberStatus.INVITED
    "declined" -> MemberStatus.DECLINED
    "left" -> MemberStatus.LEFT
    else -> MemberStatus.REMOVED
}

fun PartyDto.toDomain(stats: PartyStatsDto? = null, myStatus: MemberStatus? = null): Party = Party(
    id = id,
    name = name,
    leaderId = leaderId,
    maxMembers = maxMembers,
    milestone = milestone,
    milestoneSetAt = milestoneSetAt?.let(::parseInstant),
    completedAt = completedAt?.let(::parseInstant),
    createdAt = parseInstant(createdAt),
    total = stats?.total ?: 0,
    memberCount = stats?.memberCount ?: 0,
    myStatus = myStatus
)

fun PartyMemberDto.toDomain(username: String?): PartyMember = PartyMember(
    partyId = partyId,
    userId = userId,
    username = username,
    role = roleOf(role),
    status = statusOf(status),
    invitedAt = invitedAt?.let(::parseInstant),
    joinedAt = joinedAt?.let(::parseInstant),
    statusChangedAt = updatedAt?.let(::parseInstant)
)

fun DaroodEntryDto.toDomain(): DaroodEntry = DaroodEntry(
    id = id,
    clientId = clientId,
    partyId = partyId,
    userId = userId,
    amount = amount,
    localDate = LocalDate.parse(localDate),
    tz = tz,
    createdAt = parseInstant(createdAt),
    state = EntryState.SYNCED
)

fun PartyStatsDto.toDomain(): PartyStats = PartyStats(
    partyId = partyId,
    total = total,
    entryCount = entryCount,
    memberCount = memberCount,
    lastEntryDate = lastEntryDate?.let(LocalDate::parse)
)

fun MemberStatsDto.toDomain(): MemberStats = MemberStats(
    partyId = partyId,
    userId = userId,
    username = username,
    total = total,
    activeDays = activeDays,
    lastEntryDate = lastEntryDate?.let(LocalDate::parse)
)
