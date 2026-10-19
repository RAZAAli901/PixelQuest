package com.pixelquest.app.darood.ui.chat

import com.pixelquest.app.darood.domain.DaroodEntry

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
