package com.pixelquest.app.darood.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.darood.domain.MemberStats
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.darood.ui.formatCount
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.PartyMember
import com.pixelquest.app.ui.components.InitialAvatar
import com.pixelquest.app.ui.components.PqFieldLabel
import com.pixelquest.app.ui.components.comic
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType

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
            mine = me != null && poster.userId.equals(me, ignoreCase = true),
        )
    }
    val posted = posters.mapNotNull { it.userId?.lowercase() }.toSet()
    val yetToAdd = members
        .filter { it.status == MemberStatus.ACTIVE && it.userId.lowercase() !in posted && !it.userId.equals(me, ignoreCase = true) }
        .mapNotNull { it.username }
        .sorted()
    return PartyBoard(rows, yetToAdd)
}

/** The board in the stats sheet (mock 4F): "Everyone's part", a card each, the player's on gold. */
@Composable
fun PartyBoardSection(board: PartyBoard, modifier: Modifier = Modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = modifier.fillMaxWidth().testTag(DaroodTestTags.CHAT_BOARD)) {
        PqFieldLabel(stringResource(R.string.darood_board_title), Modifier.semantics { heading() })
        board.rows.forEach { row ->
            val name = row.username ?: stringResource(R.string.darood_someone)
            val shown = if (row.mine) stringResource(R.string.darood_info_you, name) else name
            val said = stringResource(R.string.darood_board_said, shown, formatCount(row.total))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(DaroodTestTags.boardRow(row.userId ?: "someone"))
                    .clearAndSetSemantics { contentDescription = (row.medal?.let { "$it " } ?: "") + said }
                    .comic(bg = if (row.mine) PqColors.Gold else PqColors.Paper, radius = PqDimens.Radius, border = PqDimens.BorderThin, shadow = 0.dp)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(text = row.place.toString(), style = PqType.Number)
                InitialAvatar(name, 30.dp)
                Text(text = shown, style = PqType.BodyStrong, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(text = formatCount(row.total), style = PqType.Number)
            }
        }
        if (board.yetToAdd.isNotEmpty()) {
            Text(
                text = stringResource(R.string.darood_board_yet, board.yetToAdd.joinToString(", ")),
                style = PqType.Meta,
                modifier = Modifier.testTag(DaroodTestTags.CHAT_BOARD_YET),
            )
        }
    }
}
