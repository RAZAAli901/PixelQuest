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
val CHESS_CHAT_TOKENS = setOf("chess_open_challenge")

object ChessChatTags {
    fun card(refId: String) = "chess_chat_card_$refId"
    fun accept(refId: String) = "chess_chat_accept_$refId"
}

/**
 * A chess line in the chat as a card (J152): an open challenge, from whom and for what game,
 * which any other member can take with Accept, and its sender can cancel. One that's been taken
 * or has run out says so.
 */
@Composable
fun ChessChatCard(token: String, refId: String, state: ChessPartyState, onAnswer: (String, Boolean) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag(ChessChatTags.card(refId))
            .semantics(mergeDescendants = false) { }
            .comic(bg = PqColors.Paper, radius = PqDimens.Radius, shadow = 0.dp)
            .padding(12.dp)
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
                        R.string.chess_chat_open_body, from,
                        stringResource(R.string.chess_minutes, challenge.timeControl.seconds / 60),
                        stringResource(if (challenge.rated) R.string.chess_rated else R.string.chess_friendly)
                    ),
                    style = PqType.Body
                )
                if (challenge.fromUser == state.me) {
                    PqButton(stringResource(R.string.chess_chat_cancel), onClick = { onAnswer(refId, false) }, style = PqButtonStyle.Secondary, enabled = !state.sending, modifier = Modifier.fillMaxWidth())
                } else {
                    PqButton(stringResource(R.string.chess_accept), onClick = { onAnswer(refId, true) }, enabled = !state.sending, modifier = Modifier.fillMaxWidth().testTag(ChessChatTags.accept(refId)))
                }
            }
        }
    }
}
