package com.pixelquest.app.darood.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.darood.domain.DaroodEntry
import com.pixelquest.app.darood.domain.DaroodRefusal
import com.pixelquest.app.darood.domain.EntryState
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.darood.ui.formatCount
import com.pixelquest.app.ui.theme.PixelTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * One entry in a party's chat, on its side: the player's own on the right, everyone else's on the
 * left, as in a messaging app.
 */
@Composable
fun ChatEntryRow(
    entry: DaroodEntry,
    sender: String?,
    mine: Boolean,
    modifier: Modifier = Modifier,
    memberColor: Color = memberColor(entry.userId),
    showName: Boolean = true,
    firstOfRun: Boolean = true,
    onRetry: (String) -> Unit = {}
) {
    Column(
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
        // A run of one sender's bubbles sits close together; a new sender starts a little apart.
        modifier = modifier.fillMaxWidth().padding(top = if (firstOfRun) 8.dp else 2.dp)
    ) {
        EntryBubble(entry = entry, sender = sender, mine = mine, memberColor = memberColor, showName = showName, tail = firstOfRun)
        val key = entry.clientId
        if (mine && entry.state == EntryState.FAILED && key != null) RefusedLine(entry.refusal, onRetry = { onRetry(key) })
    }
}

/** A placed bubble ([bubblesOf]) on its side, named once per run. */
@Composable
fun ChatEntryRow(item: BubbleItem, sender: String?, modifier: Modifier = Modifier, onRetry: (String) -> Unit = {}) =
    ChatEntryRow(entry = item.entry, sender = sender, mine = item.mine, modifier = modifier, showName = item.showName, firstOfRun = item.firstOfRun, onRetry = onRetry)

/** Under a refused entry: why the server said no, and a way to try it again. */
@Composable
private fun RefusedLine(refusal: String?, onRetry: () -> Unit) {
    val colors = PixelTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 2.dp).widthIn(max = 300.dp)
    ) {
        Text(
            text = stringResource(refusalText(refusal)),
            style = MaterialTheme.typography.bodySmall,
            color = colors.error,
            modifier = Modifier.weight(1f, fill = false).testTag(DaroodTestTags.ENTRY_REFUSAL)
        )
        Text(
            text = stringResource(R.string.darood_entry_retry),
            style = MaterialTheme.typography.labelLarge,
            color = colors.primaryText,
            modifier = Modifier
                .testTag(DaroodTestTags.ENTRY_RETRY)
                .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onRetry)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

/** What a refusal means for the player, in a few words. */
private fun refusalText(token: String?): Int = when (token) {
    DaroodRefusal.DAILY_LIMIT.token -> R.string.darood_entry_refused_daily
    DaroodRefusal.PARTY_CLOSED.token -> R.string.darood_entry_refused_closed
    DaroodRefusal.NOT_A_MEMBER.token -> R.string.darood_entry_refused_not_member
    DaroodRefusal.BAD_DAY.token -> R.string.darood_entry_refused_bad_day
    DaroodRefusal.BAD_TIME_ZONE.token -> R.string.darood_entry_refused_time_zone
    else -> R.string.darood_entry_refused
}

/** A bubble's colours: its fill, and the text on it. */
data class BubbleColors(val container: Color, val content: Color, val secondary: Color)

/** The player's own bubbles in the theme's accent; everyone else's on the theme's surface. */
@Composable
fun bubbleColors(mine: Boolean): BubbleColors {
    val colors = PixelTheme.colors
    return if (mine) {
        BubbleColors(container = colors.primary, content = colors.onPrimary, secondary = colors.onPrimary.copy(alpha = 0.75f))
    } else {
        BubbleColors(container = colors.surface, content = colors.onSurface, secondary = colors.onSurfaceVariant)
    }
}

/**
 * One entry in a party's chat, as a message bubble: who posted it, the amount ("+300"), and when.
 *
 * @param sender the poster's username, or null where it isn't known ("Someone").
 * @param mine the player's own: the accent's colours, and no name (it's theirs).
 * @param memberColor the poster's own colour for their name ([memberColor]), so members tell apart
 *   at a glance.
 * @param showName the name heads the bubble; a run of one sender's bubbles names them once.
 * @param tail the corner on its side is drawn in, as a tail: the first of a run has it, the rest don't.
 */
@Composable
fun EntryBubble(
    entry: DaroodEntry,
    sender: String?,
    modifier: Modifier = Modifier,
    mine: Boolean = false,
    memberColor: Color = Color.Unspecified,
    showName: Boolean = true,
    tail: Boolean = true
) {
    val colors = PixelTheme.colors
    val bubble = bubbleColors(mine)
    val look = chatLook()
    val shape = look.bubbleShape(mine = mine, tail = tail)
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .testTag(DaroodTestTags.bubble(entry.clientId ?: entry.id.toString()))
            .semantics(mergeDescendants = true) { }
            .widthIn(min = 96.dp, max = 280.dp)
            .then(look.shadowed(shape))
            .clip(shape)
            .background(bubble.container)
            .then(look.outlined(shape, if (mine) look.myOutline else look.theirOutline))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        if (showName && !mine) {
            Text(
                text = sender?.let { stringResource(R.string.darood_username_handle, it) } ?: stringResource(R.string.darood_someone),
                style = MaterialTheme.typography.labelMedium,
                color = if (memberColor != Color.Unspecified) memberColor else colors.primaryText,
                maxLines = 1
            )
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.darood_bubble_amount, formatCount(entry.amount)),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = bubble.content
            )
            Text(
                text = bubbleTime(entry.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = bubble.secondary
            )
            if (mine) SendMark(entry.state, bubble.secondary)
        }
    }
}

/**
 * On the player's own bubbles, where the entry is, as a messaging app marks a message: a clock while
 * it waits on the phone to be sent, a tick once the server has it, a warning if it refused it.
 */
@Composable
private fun SendMark(state: EntryState, color: Color) {
    if (state == EntryState.FAILED) {
        val said = stringResource(R.string.darood_entry_not_sent)
        Text(
            text = "⚠",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.testTag(DaroodTestTags.ENTRY_FAILED).clearAndSetSemantics { contentDescription = said }
        )
        return
    }
    val pending = state == EntryState.PENDING
    val said = stringResource(if (pending) R.string.darood_entry_sending else R.string.darood_entry_sent)
    Text(
        text = if (pending) "🕓" else "✓",
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .testTag(if (pending) DaroodTestTags.ENTRY_PENDING else DaroodTestTags.ENTRY_SENT)
            .clearAndSetSemantics { contentDescription = said }
    )
}


/** The time a bubble shows, in the phone's own clock and language: 10:05 AM, or 10:05. */
fun bubbleTime(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(zone).format(instant)
