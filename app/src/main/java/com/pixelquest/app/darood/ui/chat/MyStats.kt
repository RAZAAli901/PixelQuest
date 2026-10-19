package com.pixelquest.app.darood.ui.chat

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.platform.testTag
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

/** The player's numbers in a sheet over the chat, opened from its top bar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyStatsSheet(stats: MyStats, onDismiss: () -> Unit) {
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
