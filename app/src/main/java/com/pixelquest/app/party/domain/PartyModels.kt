package com.pixelquest.app.party.domain

import java.time.Instant

/** The limits on every party the server enforces (supabase/schema.md, Parties), for the app to mirror. */
object PartyRules {
    const val NAME_MIN = 2
    const val NAME_MAX = 40
    const val MEMBERS_MIN = 2
    const val MEMBERS_MAX = 30
    const val MEMBERS_DEFAULT = 10
}

/** A person's part in a party. */
enum class PartyRole { LEADER, MEMBER }

/** Where a person stands in a party. Only [ACTIVE] members post and see the party's entries. */
enum class MemberStatus { INVITED, ACTIVE, DECLINED, LEFT, REMOVED }

/**
 * A party of any kind (docs/party-core.md): the shared core, and Darood's own numbers, which stay
 * empty for a study party.
 *
 * @param total everything posted in it, by everyone, ever.
 * @param memberCount its active members.
 * @param myStatus the signed-in player's place in it.
 * @param type its kind: a Darood Party, unless it says otherwise.
 */
data class Party(
    val id: String,
    val name: String,
    val leaderId: String?,
    val maxMembers: Int,
    val milestone: Long?,
    val milestoneSetAt: Instant?,
    val completedAt: Instant?,
    val createdAt: Instant,
    val total: Long = 0,
    val memberCount: Int = 0,
    val myStatus: MemberStatus? = null,
    /** Its leader has put it on the race board: its name and progress, nothing else. */
    val isPublicRace: Boolean = false,
    val type: PartyType = PartyType.DAROOD
) {
    val isFinished: Boolean get() = completedAt != null

    /** How far the total is toward the milestone, 0 to 1; null without a milestone. */
    val progress: Float? get() = milestone?.let { (total.toDouble() / it).coerceIn(0.0, 1.0).toFloat() }
}

/**
 * Someone in a party, as its members see them. [username] is null where it isn't theirs to see.
 * [statusChangedAt] is when their row last changed: for someone who left or was removed, when that
 * happened.
 */
data class PartyMember(
    val partyId: String,
    val userId: String,
    val username: String?,
    val role: PartyRole,
    val status: MemberStatus,
    val invitedAt: Instant?,
    val joinedAt: Instant?,
    val statusChangedAt: Instant? = null
)
