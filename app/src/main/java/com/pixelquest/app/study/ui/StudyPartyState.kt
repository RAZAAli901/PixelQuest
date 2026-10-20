package com.pixelquest.app.study.ui

import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.Party
import com.pixelquest.app.party.domain.PartyMember
import com.pixelquest.app.party.domain.PartyTab
import com.pixelquest.app.study.domain.StudyCompletion
import com.pixelquest.app.study.domain.StudyItem
import com.pixelquest.app.study.domain.StudyRefusal
import com.pixelquest.app.study.domain.SyncState
import com.pixelquest.app.study.domain.WeekScore
import com.pixelquest.app.study.domain.WeekWinner
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** How close an item's due moment is, for its card. */
enum class DueState {
    /** Past it, and the player hasn't finished it. */
    OVERDUE,

    /** Due before the player's day ends. */
    TODAY,

    /** Due within the next two days. */
    SOON,
    LATER,

    /** No due date. */
    NONE
}

/** How close [dueAt] is at [now], in the player's [zone]. */
fun dueStateOf(dueAt: Instant?, now: Instant, zone: ZoneId): DueState = when {
    dueAt == null -> DueState.NONE
    dueAt.isBefore(now) -> DueState.OVERDUE
    dueAt.atZone(zone).toLocalDate() == now.atZone(zone).toLocalDate() -> DueState.TODAY
    dueAt.isBefore(now.plus(Duration.ofDays(2))) -> DueState.SOON
    else -> DueState.LATER
}

/**
 * An item as its card shows it.
 *
 * @param mine the player's own tick on it, if any (waiting, refused or on the server).
 * @param doneCount how many active members have ticked it off, of [memberCount].
 * @param canUndo the player may take their tick back: one still on the phone, or one sent within 24 hours.
 * @param canEdit the player may change or delete it: its creator, or the leader.
 */
data class ItemRow(
    val item: StudyItem,
    val mine: StudyCompletion?,
    val doneCount: Int,
    val memberCount: Int,
    val due: DueState,
    val canUndo: Boolean,
    val canEdit: Boolean
) {
    val done: Boolean get() = mine != null && mine.state != SyncState.FAILED
}

/**
 * A line on the scoreboard. [place] is shared by equal points (1, 1, 3); [score] is null for a
 * member with nothing yet this week, who shows with 0.
 */
data class BoardLine(val userId: String, val username: String?, val points: Int, val place: Int, val isMe: Boolean, val score: WeekScore?)

/** Something to tell the player about what they just tried. */
sealed interface StudyNotice {
    /** No connection: it waits, or (for what needs one) didn't happen. */
    data object Offline : StudyNotice

    data class Refused(val refusal: StudyRefusal) : StudyNotice
}

/**
 * The study party's screen: its party and tabs, the items as cards, and the board.
 *
 * @param thisWeek the items for this week: due this week, overdue and not done, or with no date and not done.
 * @param pinned the leader's pinned items, shown in a strip at the top.
 * @param board this week's scoreboard, every active member on it.
 * @param simpleMode the app's Simple Mode: the due list only, no points, ranks or board.
 */
data class StudyPartyUiState(
    val party: Party? = null,
    val myId: String? = null,
    val isLeader: Boolean = false,
    val tab: PartyTab = PartyTab.THIS_WEEK,
    val tabs: List<PartyTab> = StudyPartyKind.tabs,
    val members: List<PartyMember> = emptyList(),
    val items: List<ItemRow> = emptyList(),
    /** Every tick in the party by item, newest first, this phone's own still waiting among them. */
    val ticks: Map<String, List<StudyCompletion>> = emptyMap(),
    val thisWeek: List<ItemRow> = emptyList(),
    val weekSections: List<Pair<WeekSection, List<ItemRow>>> = emptyList(),
    val weekProgress: WeekProgress = WeekProgress(0, 0),
    val pinned: List<ItemRow> = emptyList(),
    val weekStart: LocalDate = LocalDate.MIN,
    val today: LocalDate = LocalDate.MIN,
    val board: List<BoardLine> = emptyList(),
    val myScore: WeekScore? = null,
    val pastScores: List<WeekScore> = emptyList(),
    val winners: List<WeekWinner> = emptyList(),
    val refused: List<StudyCompletion> = emptyList(),
    val waiting: Int = 0,
    val refreshing: Boolean = false,
    val loaded: Boolean = false,
    val simpleMode: Boolean = false,
    val notice: StudyNotice? = null,
    /** The Add item or Edit item sheet, open with these fields; null when closed. */
    val form: ItemForm? = null,
    /** An item the player asked to delete, waiting for them to be sure. */
    val confirmingDelete: ItemRow? = null
) {
    /** The party's active members. */
    val memberCount: Int get() = members.count { it.status == MemberStatus.ACTIVE }
}

/** Each item's card, from the party's completions and members, as the player at [now] sees it. */
fun itemRowsOf(
    items: List<StudyItem>,
    completions: List<StudyCompletion>,
    members: List<PartyMember>,
    myId: String?,
    isLeader: Boolean,
    now: Instant,
    zone: ZoneId
): List<ItemRow> {
    val active = members.filter { it.status == MemberStatus.ACTIVE }.map { it.userId }.toSet()
    val byItem = completions.groupBy { it.itemId }
    return items.map { item ->
        val ticks = byItem[item.id].orEmpty()
        val mine = ticks.firstOrNull { it.state != SyncState.SYNCED } ?: ticks.firstOrNull { myId != null && it.userId == myId }
        val doneBy = ticks.filter { it.state != SyncState.FAILED }.mapNotNull { it.userId }.toSet()
        val due = dueStateOf(item.dueAt, now, zone).let { if (it == DueState.OVERDUE && mine != null) DueState.LATER else it }
        ItemRow(
            item = item,
            mine = mine,
            doneCount = doneBy.count { it in active },
            memberCount = active.size,
            due = due,
            canUndo = mine != null && (mine.state != SyncState.SYNCED || mine.canUndoAt(now)),
            canEdit = isLeader || (myId != null && item.createdBy == myId)
        )
    }
}

/** This week's cards: due this week (Monday to Sunday, [weekStart] on), and anything still to do that's overdue or undated. */
fun thisWeekOf(rows: List<ItemRow>, weekStart: LocalDate, zone: ZoneId): List<ItemRow> = rows.filter { row ->
    val day = row.item.dueAt?.atZone(zone)?.toLocalDate()
    when {
        day == null -> !row.done
        !day.isBefore(weekStart) && day.isBefore(weekStart.plusDays(7)) -> true
        day.isBefore(weekStart) -> !row.done
        else -> false
    }
}

/** A heading on This week: overdue, one day of the week, or no date. */
sealed interface WeekSection {
    data object Overdue : WeekSection
    data class Day(val date: LocalDate) : WeekSection
    data object NoDate : WeekSection
}

/**
 * This week's cards under their headings: overdue first, then each day in order, then those with no
 * date. Within a day, soonest due first.
 */
fun weekSectionsOf(rows: List<ItemRow>, zone: ZoneId): List<Pair<WeekSection, List<ItemRow>>> {
    val overdue = rows.filter { it.due == DueState.OVERDUE }
    val dated = rows.filter { it.item.dueAt != null && it.due != DueState.OVERDUE }
        .sortedBy { it.item.dueAt }
        .groupBy { it.item.dueAt!!.atZone(zone).toLocalDate() }
    val undated = rows.filter { it.item.dueAt == null }
    return buildList {
        if (overdue.isNotEmpty()) add(WeekSection.Overdue to overdue.sortedBy { it.item.dueAt })
        dated.toSortedMap().forEach { (day, items) -> add(WeekSection.Day(day) to items) }
        if (undated.isNotEmpty()) add(WeekSection.NoDate to undated)
    }
}

/** This week's cards, done and to do, for its summary line: how many the player has done of them. */
data class WeekProgress(val done: Int, val total: Int)

fun weekProgressOf(rows: List<ItemRow>): WeekProgress = WeekProgress(rows.count { it.done }, rows.size)

/** This week's board: every active member, the most points first, equal points sharing a place. */
fun boardOf(scores: List<WeekScore>, members: List<PartyMember>, myId: String?): List<BoardLine> {
    val byUser = scores.associateBy { it.userId }
    val lines = members.filter { it.status == MemberStatus.ACTIVE }
        .map { member -> Triple(member, byUser[member.userId], byUser[member.userId]?.points ?: 0) }
        .sortedWith(compareByDescending<Triple<PartyMember, WeekScore?, Int>> { it.third }.thenBy { it.first.username ?: it.first.userId })
    return lines.map { (member, score, points) ->
        // The first with these points sets the place; those level with them share it.
        val place = lines.indexOfFirst { it.third == points } + 1
        BoardLine(member.userId, member.username, points, place, member.userId == myId, score)
    }
}
