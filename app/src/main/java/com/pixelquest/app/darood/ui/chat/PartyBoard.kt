package com.pixelquest.app.darood.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.darood.domain.MemberStats
import com.pixelquest.app.darood.domain.MemberStatus
import com.pixelquest.app.darood.domain.PartyMember
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.darood.ui.formatCount
import com.pixelquest.app.ui.theme.PixelTheme

/** One person's line on the party's board: what they've added, and their place (shared when level). */
data class BoardRow(val userId: String?, val username: String?, val total: Long, val place: Int, val mine: Boolean) {
    /** The top three places get a medal; nobody below is given a number. */
    val medal: String? get() = when (place) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> null
    }
}

/**
 * The party's board, kindly: everyone who has posted, most first, and the active members who
 * haven't yet, named apart rather than ranked at nothing.
 */
data class PartyBoard(val rows: List<BoardRow>, val yetToAdd: List<String>)

fun partyBoardOf(stats: List<MemberStats>, members: List<PartyMember>, me: String?): PartyBoard {
    val posters = stats.filter { it.total > 0 }.sortedByDescending { it.total }
    val rows = posters.map { poster ->
        BoardRow(
            userId = poster.userId,
            username = poster.username,
            total = poster.total,
            // Competition ranking: level totals share a place (1, 2, 2, 4).
            place = posters.count { it.total > poster.total } + 1,
            mine = me != null && poster.userId == me
        )
    }
    val posted = posters.mapNotNull { it.userId }.toSet()
    val yetToAdd = members
        .filter { it.status == MemberStatus.ACTIVE && it.userId !in posted && it.userId != me }
        .mapNotNull { it.username }
        .sorted()
    return PartyBoard(rows, yetToAdd)
}

/** The board in the stats sheet: "Everyone's part". */
@Composable
fun PartyBoardSection(board: PartyBoard, modifier: Modifier = Modifier) {
    val colors = PixelTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier.fillMaxWidth().testTag(DaroodTestTags.CHAT_BOARD)) {
        Text(
            text = stringResource(R.string.darood_board_title),
            style = MaterialTheme.typography.titleMedium,
            color = colors.primaryText,
            modifier = Modifier.semantics { heading() }
        )
        board.rows.forEach { row ->
            val name = row.username ?: stringResource(R.string.darood_someone)
            val shown = if (row.mine) stringResource(R.string.darood_info_you, name) else name
            val said = stringResource(R.string.darood_board_said, shown, formatCount(row.total))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(DaroodTestTags.boardRow(row.userId ?: "someone"))
                    .clearAndSetSemantics { contentDescription = (row.medal?.let { "$it " } ?: "") + said }
            ) {
                Text(text = row.medal ?: "", style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(32.dp))
                Text(
                    text = shown,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (row.mine) FontWeight.Bold else FontWeight.Normal),
                    color = colors.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(text = formatCount(row.total), style = MaterialTheme.typography.titleMedium, color = colors.primaryText)
            }
        }
        if (board.yetToAdd.isNotEmpty()) {
            Text(
                text = stringResource(R.string.darood_board_yet, board.yetToAdd.joinToString(", ")),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.testTag(DaroodTestTags.CHAT_BOARD_YET)
            )
        }
    }
}
