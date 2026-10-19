package com.pixelquest.app.darood.ui.chat

import com.pixelquest.app.darood.domain.DaroodEntry
import java.time.LocalDate
import java.time.ZoneId

/**
 * One bubble in a party's chat, placed: whose it is, and where it falls in its sender's run (one
 * sender's entries in a row, as a group chat stacks them).
 */
data class BubbleItem(
    val entry: DaroodEntry,
    val mine: Boolean,
    val firstOfRun: Boolean,
    val lastOfRun: Boolean
) {
    /** A run names its sender once, at its start; the player's own bubbles never need a name. */
    val showName: Boolean get() = firstOfRun && !mine
}

/**
 * [entries], oldest first, as bubbles. One sender's entries in a row make a run. Entries whose
 * poster isn't known never join a run, since they may be different people.
 */
fun bubblesOf(entries: List<DaroodEntry>, me: String?): List<BubbleItem> = entries.mapIndexed { i, entry ->
    val sender = entry.userId
    BubbleItem(
        entry = entry,
        mine = sender != null && sender == me,
        firstOfRun = sender == null || entries.getOrNull(i - 1)?.userId != sender,
        lastOfRun = sender == null || entries.getOrNull(i + 1)?.userId != sender
    )
}

/** A line of the chat: a day's heading, or an entry's bubble. */
sealed interface ChatItem {
    /** Stable across updates, for the list to keep its place. */
    val key: String
}

/** Where a new day starts, in the reader's own time zone. */
data class DayItem(val day: LocalDate) : ChatItem {
    override val key: String get() = "day-$day"
}

data class EntryItem(val bubble: BubbleItem) : ChatItem {
    override val key: String get() = "entry-" + (bubble.entry.clientId ?: bubble.entry.id.toString())
}

/** Something that happened to the party (someone joined, the milestone was reached), as a pill. */
data class EventItem(val event: PartyEvent) : ChatItem {
    override val key: String get() = "event-" + event.key
}

/**
 * [entries] and [events] as the chat's lines, oldest first: each day headed once, as its first
 * line's time falls in [zone] (the same clock the bubbles show), and runs as [bubblesOf] makes
 * them. An event at the same moment as an entry comes after it (the entry that reached the
 * milestone, then the milestone). A new day, or an event, starts a new run, so the sender is
 * named again.
 */
fun chatItemsOf(entries: List<DaroodEntry>, me: String?, zone: ZoneId, events: List<PartyEvent> = emptyList()): List<ChatItem> {
    // Each line with its moment: entries before events at the same moment, each kind in its own order.
    val timeline: List<Any> = (entries.map { it to it.createdAt } + events.map { it to it.at })
        .sortedWith(compareBy({ it.second }, { if (it.first is DaroodEntry) 0 else 1 }))
        .map { it.first }
    val days = timeline.map { (if (it is DaroodEntry) it.createdAt else (it as PartyEvent).at).atZone(zone).toLocalDate() }
    val items = mutableListOf<ChatItem>()
    timeline.forEachIndexed { i, line ->
        val day = days[i]
        if (i == 0 || days[i - 1] != day) items += DayItem(day)
        when (line) {
            is DaroodEntry -> {
                val sender = line.userId
                fun sameSender(j: Int) = sender != null && (timeline.getOrNull(j) as? DaroodEntry)?.userId == sender && days[j] == day
                items += EntryItem(BubbleItem(line, mine = sender != null && sender == me, firstOfRun = !sameSender(i - 1), lastOfRun = !sameSender(i + 1)))
            }
            is PartyEvent -> items += EventItem(line)
        }
    }
    return items
}
