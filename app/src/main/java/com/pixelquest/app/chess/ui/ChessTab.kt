package com.pixelquest.app.chess.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.chess.domain.Challenge
import com.pixelquest.app.chess.domain.ColourChoice
import com.pixelquest.app.chess.domain.Rating
import com.pixelquest.app.chess.domain.TimeControl
import com.pixelquest.app.ui.components.InitialAvatar
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqButtonStyle
import com.pixelquest.app.ui.components.PqFieldLabel
import com.pixelquest.app.ui.components.comic
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType

/** The study party's Chess tab, provided by its screen (it knows the party's chess). */
val LocalChessTab = staticCompositionLocalOf<(@Composable () -> Unit)?> { null }

object ChessTabTags {
    const val TAB = "chess_tab"
    const val CHALLENGE = "chess_tab_challenge"
    const val BOARD = "chess_tab_board"
    fun challenge(id: String) = "chess_tab_challenge_$id"
    fun row(userId: String) = "chess_tab_row_$userId"
    fun filter(tc: TimeControl?) = "chess_tab_filter_${tc?.seconds ?: "best"}"
}

/** What the Chess tab does with taps. */
data class ChessTabActions(
    val onChallenge: (to: String?, TimeControl, ColourChoice, Boolean) -> Unit = { _, _, _, _ -> },
    val onPick: (String?) -> Unit = {},
    val onAnswer: (String, Boolean) -> Unit = { _, _ -> },
    val onWatch: (String) -> Unit = {},
    val onBoardTime: (TimeControl?) -> Unit = {},
    /** A ladder row's player: the record against them (J157). */
    val onOpenRival: (String) -> Unit = {}
)

/**
 * The study party's Chess tab (J153), as the design's: a Challenge button, challenges waiting on
 * the player (or theirs, to cancel), the games being played, and the party's ladder by Int points,
 * with a challenge on every other member.
 */
@Composable
fun ChessTab(state: ChessPartyState, actions: ChessTabActions) {
    var sheetFor by rememberSaveable { mutableStateOf<String?>(null) }
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        contentPadding = PaddingValues(start = PqDimens.ScreenPad, top = 12.dp, end = PqDimens.ScreenPad, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize().testTag(ChessTabTags.TAB)
    ) {
        item(key = "challenge") {
            PqButton(
                text = stringResource(R.string.chess_challenge_title),
                onClick = { sheetFor = null; sheetOpen = true },
                icon = Icons.Rounded.SportsEsports,
                modifier = Modifier.fillMaxWidth().testTag(ChessTabTags.CHALLENGE)
            )
        }
        val waiting = state.challenges.filter { it.toUser == state.me || it.fromUser == state.me || it.toUser == null }
        if (waiting.isNotEmpty()) {
            item(key = "waiting_label") { PqFieldLabel(stringResource(R.string.chess_tab_challenges)) }
            items(waiting, key = { "c-" + it.id }) { ChallengeRow(it, state, actions.onAnswer) }
        }
        item(key = "live_label") { PqFieldLabel(stringResource(R.string.chess_tab_live, state.liveGames.size)) }
        if (state.liveGames.isEmpty()) {
            item(key = "live_none") { Text(stringResource(R.string.chess_tab_live_none), style = PqType.Meta) }
        } else {
            items(state.liveGames, key = { "g-" + it.id }) { game -> LiveGameCard(game, state, onWatch = { actions.onWatch(game.id) }) }
        }
        if (state.simple) return@LazyColumn
        item(key = "board_label") {
            Text(text = stringResource(R.string.chess_tab_board).uppercase(), style = PqType.CardTitle, modifier = Modifier.semantics { heading() })
        }
        item(key = "board_filter") {
            // The ladder in one time control, or by each member's best (J159).
            Segmented(
                options = listOf<TimeControl?>(null) + TimeControl.entries,
                selected = state.boardTimeControl,
                label = { tc -> if (tc == null) stringResource(R.string.chess_board_best) else stringResource(R.string.chess_minutes, tc.seconds / 60) },
                tag = { tc -> ChessTabTags.filter(tc) },
                onSelect = actions.onBoardTime
            )
        }
        item(key = "board") {
            Ladder(state.leaderboard, state, onChallenge = { sheetFor = it; sheetOpen = true; actions.onPick(it) }, onOpen = actions.onOpenRival)
        }
    }
    if (sheetOpen) {
        ChallengeSheet(
            state = state,
            onPick = actions.onPick,
            onSend = { to, tc, colour, rated ->
                sheetOpen = false
                actions.onChallenge(to, tc, colour, rated)
            },
            onDismiss = { sheetOpen = false },
            initialOpponent = sheetFor
        )
    }
}

@Composable
private fun ChallengeRow(challenge: Challenge, state: ChessPartyState, onAnswer: (String, Boolean) -> Unit) {
    val mine = challenge.fromUser == state.me
    val from = state.names[challenge.fromUser]?.let { "@$it" } ?: stringResource(R.string.darood_someone)
    val to = challenge.toUser?.let { id -> state.names[id]?.let { "@$it" } } ?: stringResource(R.string.chess_challenge_room)
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(ChessTabTags.challenge(challenge.id))
            .comic(radius = PqDimens.Radius, border = PqDimens.BorderThin, shadow = 0.dp)
            .padding(12.dp)
    ) {
        Text(
            text = if (mine) stringResource(R.string.chess_tab_mine, to) else stringResource(R.string.chess_tab_theirs, from),
            style = PqType.BodyStrong
        )
        Text(
            text = stringResource(R.string.chess_minutes, challenge.timeControl.seconds / 60) + " · " +
                stringResource(if (challenge.rated) R.string.chess_rated else R.string.chess_friendly),
            style = PqType.Meta
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (mine) {
                PqButton(stringResource(R.string.chess_chat_cancel), onClick = { onAnswer(challenge.id, false) }, style = PqButtonStyle.Secondary, enabled = !state.sending, modifier = Modifier.weight(1f))
            } else {
                if (challenge.toUser != null) PqButton(stringResource(R.string.chess_decline), onClick = { onAnswer(challenge.id, false) }, style = PqButtonStyle.Secondary, enabled = !state.sending, modifier = Modifier.weight(1f))
                PqButton(stringResource(R.string.chess_accept), onClick = { onAnswer(challenge.id, true) }, enabled = !state.sending, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** The party's ladder by Int points, each other member with a challenge button. */
@Composable
private fun Ladder(rows: List<Rating>, state: ChessPartyState, onChallenge: (String) -> Unit, onOpen: (String) -> Unit = {}) {
    Column(Modifier.fillMaxWidth().testTag(ChessTabTags.BOARD).comic(radius = PqDimens.Radius, shadow = PqDimens.Shadow)) {
        if (rows.isEmpty()) {
            Text(stringResource(R.string.chess_tab_board_none), style = PqType.Meta, modifier = Modifier.padding(12.dp))
        }
        var place = 0
        var last: Int? = null
        rows.forEachIndexed { i, row ->
            if (row.points != last) place = i + 1
            last = row.points
            val me = row.userId == state.me
            val name = state.names[row.userId]?.let { "@$it" } ?: stringResource(R.string.darood_someone)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(ChessTabTags.row(row.userId))
                    .then(if (me) Modifier.comic(bg = PqColors.Sky, radius = 0.dp, border = 0.dp, shadow = 0.dp) else Modifier)
                    .then(if (me) Modifier else Modifier.clickable(onClickLabel = stringResource(R.string.chess_h2h_title)) { onOpen(row.userId) })
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(text = place.toString(), style = PqType.Number)
                InitialAvatar(name.removePrefix("@"), 32.dp, ChessLook.avatar(i))
                Column(Modifier.weight(1f)) {
                    Text(text = if (me) stringResource(R.string.chess_you_named, name) else name, style = PqType.BodyStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = stringResource(R.string.chess_record_line, row.wins, row.draws, row.losses), style = PqType.Meta)
                }
                Text(text = com.pixelquest.app.darood.ui.formatCount(row.points), style = PqType.Number)
                if (!me) {
                    com.pixelquest.app.ui.components.PqIconButton(Icons.Rounded.SportsEsports, stringResource(R.string.chess_challenge_named, name), { onChallenge(row.userId) })
                }
            }
        }
    }
}
