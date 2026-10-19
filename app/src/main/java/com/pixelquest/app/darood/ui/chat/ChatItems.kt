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

/**
 * [entries], oldest first, as the chat's lines: each day headed once, as its first entry's time
 * falls in [zone] (the same clock the bubbles show), and runs as [bubblesOf] makes them. A new day
 * also starts a new run, so the sender is named again.
 */
fun chatItemsOf(entries: List<DaroodEntry>, me: String?, zone: ZoneId): List<ChatItem> {
    val days = entries.map { it.createdAt.atZone(zone).toLocalDate() }
    val items = mutableListOf<ChatItem>()
    entries.forEachIndexed { i, entry ->
        val day = days[i]
        if (i == 0 || days[i - 1] != day) items += DayItem(day)
        val sender = entry.userId
        val sameBefore = sender != null && i > 0 && entries[i - 1].userId == sender && days[i - 1] == day
        val sameAfter = sender != null && i < entries.lastIndex && entries[i + 1].userId == sender && days[i + 1] == day
        items += EntryItem(BubbleItem(entry, mine = sender != null && sender == me, firstOfRun = !sameBefore, lastOfRun = !sameAfter))
    }
    return items
}
