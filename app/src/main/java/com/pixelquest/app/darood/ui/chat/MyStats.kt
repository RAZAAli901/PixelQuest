package com.pixelquest.app.darood.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.darood.domain.MemberStats
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.darood.ui.formatCount
import com.pixelquest.app.ui.components.PqFieldLabel
import com.pixelquest.app.ui.components.PqSheet
import com.pixelquest.app.ui.components.comic
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import com.pixelquest.app.ui.theme.pqShape

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
    val mine = stats.firstOrNull { me != null && it.userId.equals(me, ignoreCase = true) }?.total ?: 0
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
 * The player's numbers in a sheet over the chat (mock 4F), opened from its top bar: their part of the
 * party in a grid, how steady they are in it ([consistency], once the server has said which days they
 * posted), and everyone's part ([board]). It scrolls when it's taller than the screen.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MyStatsSheet(
    stats: MyStats,
    consistency: com.pixelquest.app.darood.domain.Consistency? = null,
    board: PartyBoard? = null,
    onDismiss: () -> Unit,
) {
    PqSheet(onDismiss) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .testTag(DaroodTestTags.CHAT_MY_STATS),
        ) {
            Text(
                text = stringResource(R.string.darood_stats_title).uppercase(),
                style = PqType.Title.copy(color = PqColors.Title),
                modifier = Modifier.semantics { heading() },
            )
            if (stats.total == 0L) {
                Text(text = stringResource(R.string.darood_stats_none), style = PqType.Body)
            } else {
                val cells = buildList {
                    add(formatCount(stats.total) to stringResource(R.string.darood_stats_total))
                    stats.share?.let { add(shareText(it) to stringResource(R.string.darood_stats_share)) }
                    stats.place?.let { add(stringResource(R.string.darood_stats_place_value, it, stats.posters) to stringResource(R.string.darood_stats_place)) }
                    consistency?.let { add(formatCount(it.averagePerActiveDay) to stringResource(R.string.darood_stats_average)) }
                }
                StatGrid(cells)
            }
            consistency?.let { steady ->
                PqFieldLabel(stringResource(R.string.darood_stats_steady), Modifier.semantics { heading() })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SteadyTile(
                        stringResource(R.string.darood_stats_active_value, steady.activeLast30),
                        stringResource(R.string.darood_stats_active_days),
                        PqColors.Sky,
                        Modifier.weight(1f),
                    )
                    SteadyTile(steady.currentStreak.toString(), stringResource(R.string.darood_stats_current_streak), PqColors.Gold, Modifier.weight(1f))
                    SteadyTile(steady.bestStreak.toString(), stringResource(R.string.darood_stats_best_streak), PqColors.Paper, Modifier.weight(1f))
                }
                ConsistencyCalendar(steady)
            }
            if (board != null && board.rows.isNotEmpty()) PartyBoardSection(board)
        }
    }
}

/** The player's numbers two to a row, in one inked box with lines between them (mock 4F). */
@Composable
private fun StatGrid(cells: List<Pair<String, String>>) {
    val shape = pqShape(PqDimens.Radius)
    val rows = cells.chunked(2)
    Column(Modifier.fillMaxWidth().clip(shape).border(PqDimens.Border, PqColors.Ink, shape)) {
        rows.forEachIndexed { r, row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                row.forEachIndexed { c, (value, label) ->
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        Column(
                            Modifier
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .semantics(mergeDescendants = true) { },
                        ) {
                            Text(text = value, style = PqType.Number)
                            Text(text = label, style = PqType.Meta)
                        }
                        if (c == 0 && row.size > 1) Box(Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(PqDimens.BorderThin).background(PqColors.Ink))
                    }
                }
                if (row.size == 1) Box(Modifier.weight(1f))
            }
            if (r < rows.lastIndex) Box(Modifier.fillMaxWidth().height(PqDimens.BorderThin).background(PqColors.Ink))
        }
    }
}

/** One of the three "how steady" numbers, on its colour. */
@Composable
private fun SteadyTile(value: String, label: String, bg: Color, modifier: Modifier) {
    Column(
        modifier
            .comic(bg = bg, radius = 10.dp, border = PqDimens.BorderThin, shadow = 0.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) { },
    ) {
        Text(text = value, style = PqType.Number)
        Text(text = label, style = PqType.Meta.copy(color = PqColors.Ink))
    }
}

/**
 * The last 30 days at a glance (mock 4F): a square a day, two rows of fifteen, the oldest first and
 * today last. A red square is a day with an entry. TalkBack hears it as one sentence.
 */
@Composable
fun ConsistencyCalendar(consistency: com.pixelquest.app.darood.domain.Consistency, modifier: Modifier = Modifier) {
    val said = pluralStringResource(R.plurals.darood_calendar_said, consistency.activeLast30, consistency.activeLast30)
    val days = consistency.window
    val shape = pqShape(3.dp)
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .testTag(DaroodTestTags.CHAT_CALENDAR)
            .clearAndSetSemantics { contentDescription = said },
    ) {
        days.chunked(CALENDAR_ROW).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { day ->
                    val posted = day in consistency.last30
                    Box(
                        modifier = Modifier
                            .testTag(DaroodTestTags.calendarDay(day.toString(), posted))
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(shape)
                            .background(if (posted) PqColors.Red else PqColors.Sunken)
                            .then(if (posted) Modifier.border(2.dp, PqColors.Ink, shape) else Modifier),
                    )
                }
            }
        }
    }
}

/** Days to a row of the calendar. */
private const val CALENDAR_ROW = 15
