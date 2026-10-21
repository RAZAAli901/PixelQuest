package com.pixelquest.app.darood.ui.chat

import com.pixelquest.app.ui.chat.bubbleColors
import com.pixelquest.app.ui.chat.bubbleTime
import com.pixelquest.app.ui.chat.chatLook
import com.pixelquest.app.ui.chat.memberColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.darood.domain.DaroodEntry
import com.pixelquest.app.darood.domain.DaroodRefusal
import com.pixelquest.app.darood.domain.EntryState
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.darood.ui.formatCount
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqType
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 2.dp).widthIn(max = 300.dp)
    ) {
        Text(
            text = stringResource(refusalText(refusal)),
            style = PqType.Meta.copy(color = PqColors.Error),
            modifier = Modifier.weight(1f, fill = false).testTag(DaroodTestTags.ENTRY_REFUSAL)
        )
        Text(
            text = stringResource(R.string.darood_entry_retry).uppercase(),
            style = PqType.Button.copy(color = PqColors.RedText),
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
    val bubble = bubbleColors(mine)
    val look = chatLook()
    val shape = look.bubbleShape(mine = mine, tail = tail)
    val said = bubbleSaid(entry, sender, mine)
    Column(
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
        modifier = modifier
            .testTag(DaroodTestTags.bubble(entry.clientId ?: entry.id.toString()))
            // TalkBack reads the bubble as one sentence: "bram added 300 at 2:01 PM".
            .clearAndSetSemantics { contentDescription = said }
    ) {
        // Who posted it, over the bubble (mock 4E), in their own colour.
        if (showName && !mine) {
            Text(
                text = sender?.let { stringResource(R.string.darood_username_handle, it) } ?: stringResource(R.string.darood_someone),
                style = PqType.MetaStrong.copy(color = if (memberColor != Color.Unspecified) memberColor else PqColors.RedText),
                maxLines = 1,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .widthIn(min = 96.dp, max = 280.dp)
                .then(look.shadowed(shape))
                .clip(shape)
                .background(bubble.container)
                .then(look.outlined(shape))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // With large text, the time goes under the number rather than squeezing it.
            val amount = @Composable {
                Text(
                    text = stringResource(R.string.darood_bubble_amount, formatCount(entry.amount)),
                    style = PqType.number(28).copy(color = bubble.content),
                    maxLines = 1
                )
            }
            val stamp = @Composable {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = bubbleTime(entry.createdAt), style = PqType.Meta.copy(color = bubble.secondary))
                    if (mine) SendMark(entry.state, bubble.secondary)
                }
            }
            if (LocalDensity.current.fontScale >= LARGE_TEXT) {
                amount()
                stamp()
            } else {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    amount()
                    stamp()
                }
            }
        }
    }
}

/** From this font scale up (the phone's larger text sizes), a bubble's time goes under its number. */
private const val LARGE_TEXT = 1.5f

/**
 * What TalkBack says for a bubble: who added how many, and when; for the player's own, whether
 * it's sent. The name is said without its @, which TalkBack would read as "at".
 */
@Composable
private fun bubbleSaid(entry: DaroodEntry, sender: String?, mine: Boolean): String {
    val amount = formatCount(entry.amount)
    val time = bubbleTime(entry.createdAt)
    if (!mine) return stringResource(R.string.darood_bubble_said_theirs, sender ?: stringResource(R.string.darood_someone), amount, time)
    val state = stringResource(
        when (entry.state) {
            EntryState.PENDING -> R.string.darood_bubble_said_sending
            EntryState.SYNCED -> R.string.darood_bubble_said_sent
            EntryState.FAILED -> R.string.darood_bubble_said_not_sent
        }
    )
    return stringResource(R.string.darood_bubble_said_mine, amount, time) + " " + state
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
            style = PqType.Meta.copy(color = color),
            modifier = Modifier.testTag(DaroodTestTags.ENTRY_FAILED).clearAndSetSemantics { contentDescription = said }
        )
        return
    }
    val pending = state == EntryState.PENDING
    val said = stringResource(if (pending) R.string.darood_entry_sending else R.string.darood_entry_sent)
    Text(
        text = if (pending) "🕓" else "✓",
        style = PqType.Meta.copy(color = color),
        modifier = Modifier
            .testTag(if (pending) DaroodTestTags.ENTRY_PENDING else DaroodTestTags.ENTRY_SENT)
            .clearAndSetSemantics { contentDescription = said }
    )
}


