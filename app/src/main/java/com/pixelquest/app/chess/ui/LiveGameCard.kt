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
    fun meta(id: String) = "chess_live_meta_$id"
}

/**
 * A game being played in the party (J154): who's playing, its time control, how many moves in,
 * how many are watching, and Watch.
 */
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
            Text(
                text = listOf(
                    stringResource(R.string.chess_minutes, game.timeControl.seconds / 60),
                    androidx.compose.ui.res.pluralStringResource(R.plurals.chess_live_moves, (game.ply + 1) / 2, (game.ply + 1) / 2),
                    androidx.compose.ui.res.pluralStringResource(R.plurals.chess_live_watching, game.watchers, game.watchers)
                ).joinToString(" · "),
                style = PqType.Meta,
                modifier = Modifier.testTag(LiveGameTags.meta(game.id))
            )
        }
        PqButton(stringResource(R.string.chess_watch), onClick = onWatch, style = PqButtonStyle.Secondary, modifier = Modifier.testTag(LiveGameTags.watch(game.id)))
    }
}
