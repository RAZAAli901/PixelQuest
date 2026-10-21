package com.pixelquest.app.chess.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqButtonStyle
import com.pixelquest.app.ui.components.PqConfirmDialog
import com.pixelquest.app.ui.components.comic
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType

object GameActionTags {
    const val OFFER_DRAW = "chess_offer_draw"
    const val RESIGN = "chess_resign"
    const val DRAW_BANNER = "chess_draw_banner"
    const val ACCEPT_DRAW = "chess_accept_draw"
    const val DECLINE_DRAW = "chess_decline_draw"
}

/**
 * A player's choices under the board (J148), as the design's two buttons: Offer draw and Resign.
 * Resigning asks first. An offer already made shows as such; the opponent's offer is a banner
 * with Accept and Decline, said aloud by TalkBack when it arrives.
 */
@Composable
fun GameActions(state: ChessSessionState, actions: ChessGameActions) {
    var confirming by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
        if (state.drawOffered) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(GameActionTags.DRAW_BANNER)
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .comic(bg = PqColors.Gold, radius = PqDimens.Radius, shadow = 0.dp)
                    .padding(12.dp)
            ) {
                Text(text = stringResource(R.string.chess_draw_offered), style = PqType.BodyStrong)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PqButton(stringResource(R.string.chess_accept), onClick = { actions.onAnswerDraw(true) }, enabled = !state.sending, modifier = Modifier.weight(1f).testTag(GameActionTags.ACCEPT_DRAW))
                    PqButton(stringResource(R.string.chess_decline), onClick = { actions.onAnswerDraw(false) }, style = PqButtonStyle.Secondary, enabled = !state.sending, modifier = Modifier.weight(1f).testTag(GameActionTags.DECLINE_DRAW))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            PqButton(
                text = stringResource(if (state.iOfferedDraw) R.string.chess_draw_sent else R.string.chess_offer_draw),
                onClick = actions.onOfferDraw,
                style = PqButtonStyle.Secondary,
                icon = Icons.Rounded.Handshake,
                enabled = !state.sending && !state.iOfferedDraw,
                modifier = Modifier.weight(1f).testTag(GameActionTags.OFFER_DRAW)
            )
            PqButton(
                text = stringResource(R.string.chess_resign),
                onClick = { confirming = true },
                style = PqButtonStyle.Secondary,
                icon = Icons.Rounded.Flag,
                enabled = !state.sending,
                modifier = Modifier.weight(1f).testTag(GameActionTags.RESIGN)
            )
        }
    }
    if (confirming) {
        PqConfirmDialog(
            title = stringResource(R.string.chess_resign_title),
            message = stringResource(R.string.chess_resign_message),
            confirm = stringResource(R.string.chess_resign),
            onConfirm = {
                confirming = false
                actions.onResign()
            },
            onDismiss = { confirming = false }
        )
    }
}
