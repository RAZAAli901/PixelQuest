package com.pixelquest.app.darood.domain

import java.time.Instant
import java.time.LocalDate

/** The limits the server enforces (supabase/schema.md, Darood Parties), for the app to mirror. */
object DaroodRules {
    const val NAME_MIN = 2
    const val NAME_MAX = 40
    const val MEMBERS_MIN = 2
    const val MEMBERS_MAX = 30
    const val MEMBERS_DEFAULT = 10
    const val AMOUNT_MIN = 1
    const val AMOUNT_MAX = 100_000
    const val DAILY_MAX = 500_000

    /** How far back an entry's day may be (in its own time zone), so entries made offline still count. */
    const val DAYS_BACK_MAX = 7L
}

/** A person's part in a party. */
enum class PartyRole { LEADER, MEMBER }

/** Where a person stands in a party. Only [ACTIVE] members post and see the party's entries. */
enum class MemberStatus { INVITED, ACTIVE, DECLINED, LEFT, REMOVED }

/** Whether an entry has reached the server. */
enum class EntryState {
    /** Saved on the phone, waiting to be sent. */
    PENDING,

    /** On the server. */
    SYNCED,

    /** The server refused it (a rule, not the connection): it won't be sent again as it is. */
    FAILED
}

/**
 * A Darood Party (docs/darood-design.md, Parties).
 *
 * @param total everything posted in it, by everyone, ever.
 * @param memberCount its active members.
 * @param myStatus the signed-in player's place in it.
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
    val myStatus: MemberStatus? = null
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

/**
 * One number a member posted. [id] is the server's, null until it's there; [clientId] is the
 * phone's own key for it, so sending it twice never counts it twice.
 */
data class DaroodEntry(
    val id: Long?,
    val clientId: String?,
    val partyId: String,
    val userId: String?,
    val amount: Int,
    val localDate: LocalDate,
    val tz: String,
    val createdAt: Instant,
    val state: EntryState = EntryState.SYNCED,
    /** Why the server refused it (a [DaroodRefusal] token), for a FAILED entry. */
    val refusal: String? = null
)

/** A party's numbers, from the server's party_stats. */
data class PartyStats(
    val partyId: String,
    val total: Long,
    val entryCount: Int,
    val memberCount: Int,
    val lastEntryDate: LocalDate?
)

/** One person's part of a party's total, from the server's party_member_stats. */
data class MemberStats(
    val partyId: String,
    val userId: String?,
    val username: String?,
    val total: Long,
    val activeDays: Int,
    val lastEntryDate: LocalDate?
)
