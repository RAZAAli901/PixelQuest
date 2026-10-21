package com.pixelquest.app.chess.ui

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
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.chess.domain.ChessGame
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqButtonStyle
import com.pixelquest.app.ui.components.comic
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType

object LiveGameTags {
    fun card(id: String) = "chess_live_$id"
    fun watch(id: String) = "chess_live_watch_$id"
}

/** A game being played in the party: who's playing, and Watch. */
@Composable
fun LiveGameCard(game: ChessGame, state: ChessPartyState, onWatch: () -> Unit) {
    val white = state.names[game.whiteId]?.let { "@$it" } ?: stringResource(R.string.chess_white)
    val black = state.names[game.blackId]?.let { "@$it" } ?: stringResource(R.string.chess_black)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(LiveGameTags.card(game.id))
            .comic(radius = PqDimens.Radius, border = PqDimens.BorderThin, shadow = 0.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = stringResource(R.string.chess_live_players, white, black), style = PqType.BodyStrong)
        }
        PqButton(stringResource(R.string.chess_watch), onClick = onWatch, style = PqButtonStyle.Secondary, modifier = Modifier.testTag(LiveGameTags.watch(game.id)))
    }
}
