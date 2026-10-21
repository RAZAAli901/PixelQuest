package com.pixelquest.app.chess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.R
import com.pixelquest.app.chess.domain.ChessColour
import com.pixelquest.app.chess.domain.ChessGame
import com.pixelquest.app.chess.domain.ClockSync
import com.pixelquest.app.ui.components.PqIconButton
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqType

object ChessGameTags {
    const val SCREEN = "chess_game_screen"
    const val KICKER = "chess_game_kicker"
    const val WATCHERS = "chess_game_watchers"
    const val SPECTATING = "chess_game_spectating"
    const val LOADING = "chess_game_loading"
}

/** What the game screen does with the player's taps, so it can be shown without a view model. */
data class ChessGameActions(
    val onBack: () -> Unit = {},
    val onMove: (String) -> Unit = {},
    val onResign: () -> Unit = {},
    val onOfferDraw: () -> Unit = {},
    val onAnswerDraw: (Boolean) -> Unit = {},
    val onRematch: () -> Unit = {},
    val onNextGame: () -> Unit = {}
)

@Composable
fun ChessGameRoute(onNavigateBack: () -> Unit, viewModel: ChessSessionViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    ChessGameScreen(
        state = state,
        sync = viewModel.clockSync,
        actions = ChessGameActions(
            onBack = onNavigateBack,
            onMove = viewModel::move,
            onResign = { viewModel.resign() },
            onOfferDraw = { viewModel.offerDraw() },
            onAnswerDraw = { viewModel.answerDraw(it) }
        )
    )
}

/**
 * The game (the design's "game" and "spectate" screens): the top bar with the time control and
 * the watcher count, the opponent above the board and the player below (White below for someone
 * watching), each with their clock, and the board, which only the side to move can play on.
 */
@Composable
fun ChessGameScreen(state: ChessSessionState, sync: ClockSync, actions: ChessGameActions) {
    val game = state.game
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(PqColors.Ground)
            .testTag(ChessGameTags.SCREEN)
            .verticalScroll(rememberScrollState())
            .padding(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 12.dp)
    ) {
        GameTopBar(game, state, actions.onBack)
        if (game == null) {
            Box(Modifier.fillMaxWidth().height(320.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PqColors.Ink, modifier = Modifier.testTag(ChessGameTags.LOADING))
            }
            return@Column
        }
        val bottomColour = state.myColour ?: ChessColour.WHITE
        val topColour = if (bottomColour == ChessColour.WHITE) ChessColour.BLACK else ChessColour.WHITE
        val now = rememberTicking(running = game.status.live)
        Player(game, state, topColour, now, sync)
        PlayableBoard(
            fen = state.boardFen ?: game.fen,
            myColour = state.myColour,
            enabled = state.myTurn,
            lastMove = state.lastMove,
            onMove = actions.onMove,
            flipped = bottomColour == ChessColour.BLACK,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Player(game, state, bottomColour, now, sync)
    }
}

@Composable
internal fun Player(game: ChessGame, state: ChessSessionState, colour: ChessColour, now: Long, sync: ClockSync) {
    val id = if (colour == ChessColour.WHITE) game.whiteId else game.blackId
    val raw = state.names[id]?.let { "@$it" } ?: stringResource(if (colour == ChessColour.WHITE) R.string.chess_white else R.string.chess_black)
    val name = if (id == state.me) stringResource(R.string.chess_you_named, raw) else raw
    PlayerRow(game, colour, name, state.points[id], if (colour == ChessColour.WHITE) 2 else 0, now, sync)
}

@Composable
private fun GameTopBar(game: ChessGame?, state: ChessSessionState, onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 6.dp)
    ) {
        PqIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.chess_back), onBack)
        if (state.spectating) {
            Text(
                text = stringResource(R.string.chess_spectating),
                style = PqType.Label.copy(color = PqColors.OnRed),
                modifier = Modifier
                    .testTag(ChessGameTags.SPECTATING)
                    .background(PqColors.Red)
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            )
        }
        Text(
            text = game?.let { kickerOf(it) }.orEmpty(),
            style = PqType.MetaStrong,
            modifier = Modifier.weight(1f).testTag(ChessGameTags.KICKER)
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.testTag(ChessGameTags.WATCHERS)) {
            Icon(Icons.Rounded.Visibility, contentDescription = stringResource(R.string.chess_watching_said, state.watchers), tint = PqColors.Ink)
            Text(text = state.watchers.toString(), style = PqType.MetaStrong)
        }
    }
}

/** "3 MIN · RATED", as the design's top bar has it. */
@Composable
fun kickerOf(game: ChessGame): String = stringResource(
    R.string.chess_game_kicker,
    game.timeControl.seconds / 60,
    stringResource(if (game.rated) R.string.chess_rated else R.string.chess_friendly)
).uppercase()
