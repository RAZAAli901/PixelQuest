package com.pixelquest.app.darood.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * One entry in a party's chat, as a message bubble: who posted it, the amount ("+300"), and when.
 *
 * @param sender the poster's username, or null where it isn't known ("Someone").
 * @param showName the name heads the bubble; a run of one sender's bubbles names them once.
 */
@Composable
fun EntryBubble(
    entry: DaroodEntry,
    sender: String?,
    modifier: Modifier = Modifier,
    showName: Boolean = true
) {
    val colors = PixelTheme.colors
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .widthIn(min = 96.dp, max = 280.dp)
            .clip(BubbleShape)
            .background(colors.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) { }
            .testTag(DaroodTestTags.bubble(entry.clientId ?: entry.id.toString()))
    ) {
        if (showName) {
            Text(
                text = sender?.let { stringResource(R.string.darood_username_handle, it) } ?: stringResource(R.string.darood_someone),
                style = MaterialTheme.typography.labelMedium,
                color = colors.primaryText,
                maxLines = 1
            )
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.darood_bubble_amount, formatCount(entry.amount)),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = colors.onSurface
            )
            Text(
                text = bubbleTime(entry.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}

/** Rounded, like a chat's message. */
private val BubbleShape = RoundedCornerShape(16.dp)

/** The time a bubble shows, in the phone's own clock and language: 10:05 AM, or 10:05. */
fun bubbleTime(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(zone).format(instant)
