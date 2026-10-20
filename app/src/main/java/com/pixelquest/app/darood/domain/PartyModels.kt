package com.pixelquest.app.darood.domain

import com.pixelquest.app.party.domain.PartyRules
import java.time.Instant
import java.time.LocalDate
import com.pixelquest.app.party.domain.Party

/** Darood Party's limits the server enforces (supabase/schema.md, Parties), for the app to mirror. */
object DaroodRules {
    // A party's name and size are every party's (PartyRules); Darood's own limits follow.
    const val NAME_MIN = PartyRules.NAME_MIN
    const val NAME_MAX = PartyRules.NAME_MAX
    const val MEMBERS_MIN = PartyRules.MEMBERS_MIN
    const val MEMBERS_MAX = PartyRules.MEMBERS_MAX
    const val MEMBERS_DEFAULT = PartyRules.MEMBERS_DEFAULT
    const val AMOUNT_MIN = 1
    const val AMOUNT_MAX = 100_000
    const val DAILY_MAX = 500_000

    /** How far back an entry's day may be (in its own time zone), so entries made offline still count. */
    const val DAYS_BACK_MAX = 7L
}

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

/**
 * A party on the race board, as the server's race_board shows it: never its members, usernames,
 * entries or id. [mine] is whether the player is in it.
 */
data class RaceLine(
    val name: String,
    val memberCount: Int,
    val total: Long,
    val milestone: Long,
    val progress: Float,
    val completedAt: Instant?,
    val mine: Boolean
)

/**
 * The race's order: finished parties first, the earliest finish first; then the rest by how far they
 * are, the furthest first; level ones by total, then by name.
 */
fun rankRace(lines: List<RaceLine>): List<RaceLine> = lines.sortedWith(
    compareBy<RaceLine> { it.completedAt == null }
        .thenBy { it.completedAt }
        .thenByDescending { it.progress }
        .thenByDescending { it.total }
        .thenBy { it.name }
)

/** One poster's total for a day in a party, from the server's party_day_stats. */
data class DayTotal(val userId: String?, val total: Long)

/** A day's numbers in a party: the player's own, and everyone's together. */
data class DayTotals(val mine: Long, val party: Long)

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
