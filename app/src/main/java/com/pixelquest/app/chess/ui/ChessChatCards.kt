package com.pixelquest.app.chess.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.chess.domain.ChallengeStatus
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqButtonStyle
import com.pixelquest.app.ui.components.comic
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType

/**
 * How the study chat draws the server's chess lines (a token and the id it's about): provided by
 * the study party screen, which knows the party's chess. Without one, they're plain lines.
 */
val LocalChessChatCards = staticCompositionLocalOf<(@Composable (token: String, refId: String) -> Unit)?> { null }

/** The chess lines the chat draws as cards. */
val CHESS_CHAT_TOKENS = setOf("chess_open_challenge", "chess_challenge_sent", "chess_game_started", "chess_game_result")

object ChessChatTags {
    fun watch(refId: String) = "chess_chat_watch_$refId"
    fun card(refId: String) = "chess_chat_card_$refId"
    fun accept(refId: String) = "chess_chat_accept_$refId"
}

/**
 * A chess line in the chat as a card (J152): an open challenge, from whom and for what game,
 * which any other member can take with Accept, and its sender can cancel. One that's been taken
 * or has run out says so.
 */
@Composable
fun ChessChatCard(
    token: String,
    refId: String,
    state: ChessPartyState,
    onAnswer: (String, Boolean) -> Unit,
    onNeedGame: (String) -> Unit = {},
    onOpenGame: (String) -> Unit = {},
) {
    if (token == "chess_game_started" || token == "chess_game_result") {
        androidx.compose.runtime.LaunchedEffect(refId) { onNeedGame(refId) }
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag(ChessChatTags.card(refId))
            .semantics(mergeDescendants = false) { }
            .comic(bg = PqColors.Paper, radius = PqDimens.Radius, shadow = 0.dp)
            .padding(12.dp),
    ) {
        when (token) {
            "chess_open_challenge" -> {
                val challenge = state.challenges.firstOrNull { it.id == refId }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.SportsEsports, contentDescription = null, tint = PqColors.Ink)
                    Text(text = stringResource(R.string.chess_chat_open_title), style = PqType.BodyStrong)
                }
                if (challenge == null || challenge.status != ChallengeStatus.OPEN) {
                    Text(text = stringResource(R.string.chess_chat_open_gone), style = PqType.Meta)
                    return@Column
                }
                val from = state.names[challenge.fromUser]?.let { "@$it" } ?: stringResource(R.string.darood_someone)
                Text(
                    text = stringResource(
                        R.string.chess_chat_open_body,
                        from,
                        stringResource(R.string.chess_minutes, challenge.timeControl.seconds / 60),
                        stringResource(if (challenge.rated) R.string.chess_rated else R.string.chess_friendly),
                    ),
                    style = PqType.Body,
                )
                if (challenge.fromUser == state.me) {
                    PqButton(stringResource(R.string.chess_chat_cancel), onClick = { onAnswer(refId, false) }, style = PqButtonStyle.Secondary, enabled = !state.sending, modifier = Modifier.fillMaxWidth())
                } else {
                    PqButton(stringResource(R.string.chess_accept), onClick = { onAnswer(refId, true) }, enabled = !state.sending, modifier = Modifier.fillMaxWidth().testTag(ChessChatTags.accept(refId)))
                }
            }
            // The other lines (J156): a challenge sent, a game started, a result.
            "chess_challenge_sent" -> {
                val challenge = state.challenges.firstOrNull { it.id == refId }
                val from = challenge?.let { c -> state.names[c.fromUser]?.let { "@$it" } } ?: stringResource(R.string.darood_someone)
                val to = challenge?.toUser?.let { id -> state.names[id]?.let { "@$it" } } ?: stringResource(R.string.darood_someone)
                CardTitle(stringResource(R.string.chess_chat_sent_title))
                Text(text = challenge?.let { stringResource(R.string.chess_chat_sent_body, from, to, stringResource(R.string.chess_minutes, it.timeControl.seconds / 60)) } ?: stringResource(R.string.chess_chat_open_gone), style = PqType.Body)
                if (challenge != null && challenge.status == ChallengeStatus.OPEN) {
                    if (challenge.fromUser.equals(state.me, ignoreCase = true)) {
                        PqButton(stringResource(R.string.chess_chat_cancel), onClick = { onAnswer(refId, false) }, style = PqButtonStyle.Secondary, enabled = !state.sending, modifier = Modifier.fillMaxWidth())
                    } else if (challenge.toUser?.equals(state.me, ignoreCase = true) == true) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            PqButton(stringResource(R.string.chess_decline), onClick = { onAnswer(refId, false) }, style = PqButtonStyle.Secondary, enabled = !state.sending, modifier = Modifier.weight(1f))
                            PqButton(stringResource(R.string.chess_accept), onClick = { onAnswer(refId, true) }, enabled = !state.sending, modifier = Modifier.weight(1f).testTag(ChessChatTags.accept(refId)))
                        }
                    }
                }
            }
            "chess_game_started", "chess_game_result" -> {
                val game = state.games[refId] ?: state.liveGames.firstOrNull { it.id == refId }
                val white = game?.let { g -> state.names[g.whiteId]?.let { "@$it" } } ?: stringResource(R.string.chess_white)
                val black = game?.let { g -> state.names[g.blackId]?.let { "@$it" } } ?: stringResource(R.string.chess_black)
                if (token == "chess_game_started") {
                    CardTitle(stringResource(R.string.chess_chat_started_title))
                    Text(text = stringResource(R.string.chess_live_players, white, black), style = PqType.Body)
                    if (game == null || game.status.live) {
                        val isPlayer = state.me != null && game != null && (game.whiteId.equals(state.me, ignoreCase = true) || game.blackId.equals(state.me, ignoreCase = true))
                        val btnText = if (isPlayer) stringResource(R.string.chess_play) else stringResource(R.string.chess_watch)
                        PqButton(btnText, onClick = { onOpenGame(refId) }, style = PqButtonStyle.Secondary, modifier = Modifier.fillMaxWidth().testTag(ChessChatTags.watch(refId)))
                    }
                } else {
                    CardTitle(stringResource(R.string.chess_chat_result_title))
                    Text(
                        text = when (game?.result) {
                            com.pixelquest.app.chess.domain.GameResult.WHITE -> stringResource(R.string.chess_chat_beat, white, black)
                            com.pixelquest.app.chess.domain.GameResult.BLACK -> stringResource(R.string.chess_chat_beat, black, white)
                            com.pixelquest.app.chess.domain.GameResult.DRAW -> stringResource(R.string.chess_chat_drew, white, black)
                            null -> stringResource(R.string.chess_live_players, white, black)
                        },
                        style = PqType.BodyStrong,
                    )
                    game?.reason?.let { Text(text = reasonText(it), style = PqType.Meta) }
                }
            }
        }
    }
}

@Composable
private fun CardTitle(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Rounded.SportsEsports, contentDescription = null, tint = PqColors.Ink)
        Text(text = text, style = PqType.BodyStrong)
    }
}
