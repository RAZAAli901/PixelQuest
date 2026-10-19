package com.pixelquest.app.darood.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.darood.domain.DaroodEntry
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
    memberColor: Color = Color.Unspecified,
    showName: Boolean = true
) {
    Box(
        contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart,
        modifier = modifier.fillMaxWidth()
    ) {
        EntryBubble(entry = entry, sender = sender, mine = mine, memberColor = memberColor, showName = showName)
    }
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
 * @param memberColor the poster's own colour for their name, so members tell apart at a glance.
 * @param showName the name heads the bubble; a run of one sender's bubbles names them once.
 */
@Composable
fun EntryBubble(
    entry: DaroodEntry,
    sender: String?,
    modifier: Modifier = Modifier,
    mine: Boolean = false,
    memberColor: Color = Color.Unspecified,
    showName: Boolean = true
) {
    val colors = PixelTheme.colors
    val bubble = bubbleColors(mine)
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .testTag(DaroodTestTags.bubble(entry.clientId ?: entry.id.toString()))
            .semantics(mergeDescendants = true) { }
            .widthIn(min = 96.dp, max = 280.dp)
            .clip(if (mine) MyBubbleShape else TheirBubbleShape)
            .background(bubble.container)
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
        }
    }
}

/** Rounded like a chat's message, with the corner nearest its side drawn in, as a tail. */
private val MyBubbleShape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomEnd = 16.dp, bottomStart = 16.dp)
private val TheirBubbleShape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp)

/** The time a bubble shows, in the phone's own clock and language: 10:05 AM, or 10:05. */
fun bubbleTime(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(zone).format(instant)
