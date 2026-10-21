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
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    const val LEAVE = "chess_watch_leave"
    const val NEXT = "chess_watch_next"
    const val FLIP = "chess_watch_flip"
    const val CONFIRM = "chess_confirm_move"
    const val CONFIRM_YES = "chess_confirm_yes"
    fun captured(colour: ChessColour) = "chess_captured_${colour.name.lowercase()}"
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
fun ChessGameRoute(onNavigateBack: () -> Unit, onOpenGame: (String) -> Unit = {}, viewModel: ChessSessionViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val prefs by viewModel.prefs.collectAsState()
    // Sounds and haptics for each move and the end (J161).
    val view = androidx.compose.ui.platform.LocalView.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    androidx.compose.runtime.LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ChessSessionEvent.Moved -> ChessFeedback.play(ChessFeedback.cueOf(event.move), view, haptic, viewModel.prefs.value.sounds)
                is ChessSessionEvent.Ended -> ChessFeedback.play(ChessCue.END, view, haptic, viewModel.prefs.value.sounds)
                else -> Unit
            }
        }
    }
    ChessGameScreen(
        state = state,
        sync = viewModel.clockSync,
        prefs = prefs,
        actions = ChessGameActions(
            onBack = onNavigateBack,
            onMove = viewModel::move,
            onResign = { viewModel.resign() },
            onOfferDraw = { viewModel.offerDraw() },
            onAnswerDraw = { viewModel.answerDraw(it) },
            onRematch = viewModel::rematch,
            onNextGame = { viewModel.nextLiveGame()?.let(onOpenGame) }
        )
    )
}

/**
 * The game (the design's "game" and "spectate" screens): the top bar with the time control and
 * the watcher count, the opponent above the board and the player below (White below for someone
 * watching), each with their clock, and the board, which only the side to move can play on.
 */
@Composable
fun ChessGameScreen(state: ChessSessionState, sync: ClockSync, actions: ChessGameActions, prefs: com.pixelquest.app.chess.data.ChessPrefs = com.pixelquest.app.chess.data.ChessPrefs()) {
    // Confirm moves (J160): the move waits under the board for a yes.
    var confirming by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf<String?>(null) }
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
        // A watcher may turn the board round (J155); a player sees their own side below.
        var watcherFlipped by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
        val bottomColour = state.myColour ?: if (watcherFlipped) ChessColour.BLACK else ChessColour.WHITE
        val topColour = if (bottomColour == ChessColour.WHITE) ChessColour.BLACK else ChessColour.WHITE
        val now = rememberTicking(running = game.status.live)
        Player(game, state, topColour, now, sync)
        PlayableBoard(
            fen = state.boardFen ?: game.fen,
            myColour = state.myColour,
            // Watching: the board only shows (no moves can be sent from it).
            enabled = state.myTurn && !state.spectating,
            lastMove = state.lastMove,
            onMove = { uci -> if (prefs.confirmMoves) confirming = uci else actions.onMove(uci) },
            flipped = bottomColour == ChessColour.BLACK,
            autoQueen = prefs.autoQueen,
            animate = prefs.animate,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Player(game, state, bottomColour, now, sync)
        confirming?.let { uci ->
            ConfirmMove(uci, onConfirm = { confirming = null; actions.onMove(uci) }, onCancel = { confirming = null })
        }
        MoveList(state.moves)
        if (!state.spectating && game.status.live) GameActions(state, actions)
        if (state.spectating) SpectatorActions(onLeave = actions.onBack, onNext = actions.onNextGame, onFlip = { watcherFlipped = !watcherFlipped })
        if (!game.status.live) GameOverCard(game, state, actions.onRematch)
    }
}

@Composable
internal fun Player(game: ChessGame, state: ChessSessionState, colour: ChessColour, now: Long, sync: ClockSync) {
    val id = if (colour == ChessColour.WHITE) game.whiteId else game.blackId
    val raw = state.names[id]?.let { "@$it" } ?: stringResource(if (colour == ChessColour.WHITE) R.string.chess_white else R.string.chess_black)
    val name = if (id == state.me) stringResource(R.string.chess_you_named, raw) else raw
    val material = com.pixelquest.app.chess.domain.Material.of(com.pixelquest.app.chess.domain.BoardPosition.of(state.boardFen ?: game.fen))
    val white = colour == ChessColour.WHITE
    val taken = if (white) material.byWhite else material.byBlack
    val lead = if (white) material.advantage else -material.advantage
    PlayerRow(game, colour, name, state.points[id], if (white) 2 else 0, now, sync) { Captured(taken, lead, colour) }
}

/** What a player has taken, as glyphs, and their material lead (J146). */
@Composable
internal fun Captured(pieces: List<Char>, lead: Int, colour: ChessColour) {
    val said = stringResource(R.string.chess_captured_said, pieces.size, lead.coerceAtLeast(0))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .testTag(ChessGameTags.captured(colour))
            .semantics(mergeDescendants = true) { contentDescription = said }
    ) {
        Text(
            text = pieces.joinToString("") { ChessLook.glyph(it) },
            style = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, lineHeight = 16.sp, color = PqColors.Ink2)
        )
        if (lead > 0) Text(text = "+$lead", style = PqType.MetaStrong)
    }
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

/** Watching (J155), as the design's spectate screen: Leave, Next live game, and Flip board. */
@Composable
private fun SpectatorActions(onLeave: () -> Unit, onNext: () -> Unit, onFlip: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
        com.pixelquest.app.ui.components.PqButton(stringResource(R.string.chess_leave), onClick = onLeave, style = com.pixelquest.app.ui.components.PqButtonStyle.Secondary, icon = Icons.AutoMirrored.Rounded.Logout, modifier = Modifier.weight(1f).testTag(ChessGameTags.LEAVE))
        com.pixelquest.app.ui.components.PqButton(stringResource(R.string.chess_next_live), onClick = onNext, style = com.pixelquest.app.ui.components.PqButtonStyle.Secondary, icon = Icons.Rounded.SkipNext, modifier = Modifier.weight(1f).testTag(ChessGameTags.NEXT))
    }
    com.pixelquest.app.ui.components.PqButton(stringResource(R.string.chess_flip), onClick = onFlip, style = com.pixelquest.app.ui.components.PqButtonStyle.Secondary, icon = Icons.Rounded.SwapVert, modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp).testTag(ChessGameTags.FLIP))
}

/** "Play e2e4?" with Confirm and Cancel, when the player asks to confirm moves (J160). */
@Composable
private fun ConfirmMove(uci: String, onConfirm: () -> Unit, onCancel: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp).testTag(ChessGameTags.CONFIRM)
    ) {
        Text(text = stringResource(R.string.chess_confirm_move, uci.substring(0, 2) + "-" + uci.substring(2)), style = PqType.BodyStrong, modifier = Modifier.weight(1f))
        com.pixelquest.app.ui.components.PqButton(stringResource(R.string.pq_cancel), onClick = onCancel, style = com.pixelquest.app.ui.components.PqButtonStyle.Secondary)
        com.pixelquest.app.ui.components.PqButton(stringResource(R.string.chess_confirm), onClick = onConfirm, modifier = Modifier.testTag(ChessGameTags.CONFIRM_YES))
    }
}
