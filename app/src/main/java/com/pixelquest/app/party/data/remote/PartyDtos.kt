package com.pixelquest.app.party.data.remote

import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.PartyMember
import com.pixelquest.app.party.domain.PartyRole
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.OffsetDateTime

// Every party's rows as Supabase sends them (supabase/schema.md, Parties; docs/party-core.md), and
// their mappings to the domain models. Timestamps arrive as ISO strings with an offset.

/** A party the account is in or invited to, with the account's own membership row. */
data class RemoteParty(val party: PartyDto, val membership: PartyMemberDto)

/** What invite_member answered. It never says whether a name exists. */
enum class InviteOutcome { SENT, ALREADY_MEMBER }

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
    @SerialName("is_public_race") val isPublicRace: Boolean = false,
    /** Its kind (PartyType's wire name); missing from a server before 20261019000031, where every party is darood. */
    @SerialName("party_type") val partyType: String = "darood"
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

/** A row of public.usernames that the reader may see (their own, or a co-member's). */
@Serializable
data class MemberNameDto(
    @SerialName("user_id") val userId: String,
    @SerialName("username") val username: String
)

/** The answer of invite_member, respond_invite, leave_party and remove_member. */
@Serializable
data class PartyStatusReply(@SerialName("status") val status: String)

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
