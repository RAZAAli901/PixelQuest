package com.pixelquest.app.darood.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.darood.domain.MemberStats
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.darood.ui.formatCount
import com.pixelquest.app.ui.theme.PixelTheme

/**
 * The player's part of a party: what they've added, their share of the total, and their place
 * among everyone who has posted.
 *
 * @param share their part of the party's total, 0 to 1; null while the party has nothing yet.
 * @param place 1 for the most; people level with each other share a place. Null before they post.
 * @param posters how many people have posted (people who left or were removed still count: their
 *   entries do).
 */
data class MyStats(val total: Long, val share: Double?, val place: Int?, val posters: Int)

/** The player's numbers from the party's members' totals ([MemberStats]) and its total. */
fun myStatsOf(stats: List<MemberStats>, partyTotal: Long, me: String?): MyStats {
    val mine = stats.firstOrNull { me != null && it.userId == me }?.total ?: 0
    val posters = stats.count { it.total > 0 }
    // Competition ranking: one more than how many have more (1, 2, 2, 4).
    val place = if (mine > 0) stats.count { it.total > mine } + 1 else null
    val share = if (partyTotal > 0) mine.toDouble() / partyTotal else null
    return MyStats(total = mine, share = share, place = place, posters = posters)
}

/** A share as the sheet says it: "34%", "under 1%" for a little, or "0%". */
@Composable
fun shareText(share: Double): String = when {
    share > 0 && share < 0.01 -> stringResource(R.string.darood_stats_share_tiny)
    else -> stringResource(R.string.darood_stats_percent, Math.round(share * 100).toInt())
}

/**
 * The player's numbers in a sheet over the chat, opened from its top bar: their part of the party,
 * and how steady they are in it ([consistency], once the server has said which days they posted).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyStatsSheet(stats: MyStats, consistency: com.pixelquest.app.darood.domain.Consistency? = null, onDismiss: () -> Unit) {
    val colors = PixelTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp)
                .testTag(DaroodTestTags.CHAT_MY_STATS)
        ) {
            Text(
                text = stringResource(R.string.darood_stats_title),
                style = MaterialTheme.typography.titleLarge,
                color = colors.primaryText,
                modifier = Modifier.semantics { heading() }
            )
            if (stats.total == 0L) {
                Text(
                    text = stringResource(R.string.darood_stats_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface
                )
            } else {
                StatLine(stringResource(R.string.darood_stats_total), formatCount(stats.total))
                stats.share?.let { StatLine(stringResource(R.string.darood_stats_share), shareText(it)) }
                stats.place?.let { StatLine(stringResource(R.string.darood_stats_place), stringResource(R.string.darood_stats_place_value, it, stats.posters)) }
            }
            consistency?.let { steady ->
                Text(
                    text = stringResource(R.string.darood_stats_steady),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.primaryText,
                    modifier = Modifier.semantics { heading() }
                )
                StatLine(
                    stringResource(R.string.darood_stats_active_days),
                    stringResource(R.string.darood_stats_of_30, steady.activeLast30)
                )
                StatLine(
                    stringResource(R.string.darood_stats_current_streak),
                    pluralStringResource(R.plurals.darood_stats_days, steady.currentStreak, steady.currentStreak)
                )
                StatLine(
                    stringResource(R.string.darood_stats_best_streak),
                    pluralStringResource(R.plurals.darood_stats_days, steady.bestStreak, steady.bestStreak)
                )
                StatLine(stringResource(R.string.darood_stats_average), formatCount(steady.averagePerActiveDay))
                ConsistencyCalendar(steady)
            }
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    val colors = PixelTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { }
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface, modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = colors.primaryText
        )
    }
}

/**
 * The last 30 days at a glance: a dot a day, three rows of ten, the oldest first and today last
 * (ringed). A filled dot is a day with an entry. TalkBack hears it as one sentence.
 */
@Composable
fun ConsistencyCalendar(consistency: com.pixelquest.app.darood.domain.Consistency, modifier: Modifier = Modifier) {
    val colors = PixelTheme.colors
    val said = pluralStringResource(R.plurals.darood_calendar_said, consistency.activeLast30, consistency.activeLast30)
    val days = consistency.window
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .testTag(DaroodTestTags.CHAT_CALENDAR)
            .clearAndSetSemantics { contentDescription = said }
    ) {
        days.chunked(CALENDAR_ROW).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                week.forEach { day ->
                    val posted = day in consistency.last30
                    Box(
                        modifier = Modifier
                            .testTag(DaroodTestTags.calendarDay(day.toString(), posted))
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (posted) colors.primary else colors.surfaceVariant)
                            .then(if (day == consistency.asOf) Modifier.border(2.dp, colors.onSurface, CircleShape) else Modifier)
                    )
                }
            }
        }
    }
}

/** Days to a row of the calendar. */
private const val CALENDAR_ROW = 10
