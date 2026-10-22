package com.pixelquest.app.chess.ui

import com.pixelquest.app.ui.motion.IntDeltaChip
import com.pixelquest.app.ui.motion.RewardBurst
import com.pixelquest.app.ui.motion.RollingNumber
import com.pixelquest.app.ui.motion.cellFlash
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.chess.domain.ChessColour
import com.pixelquest.app.chess.domain.ChessConfig
import com.pixelquest.app.chess.domain.ChessGame
import com.pixelquest.app.chess.domain.GameStatus
import com.pixelquest.app.chess.domain.HeadToHead
import com.pixelquest.app.chess.domain.Outcome
import com.pixelquest.app.chess.domain.ResultReason
import com.pixelquest.app.chess.domain.outcomeFor
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.comic
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType

object GameOverTags {
    const val CARD = "chess_game_over"
    const val CHANGE = "chess_game_over_change"
    const val TIER = "chess_game_over_tier"
    const val RECORD = "chess_game_over_record"
    const val REMATCH = "chess_rematch"
}

/** How a game ended, said plainly. */
@Composable
fun reasonText(reason: ResultReason?): String = stringResource(
    when (reason) {
        ResultReason.CHECKMATE -> R.string.chess_reason_checkmate
        ResultReason.RESIGN -> R.string.chess_reason_resign
        ResultReason.TIMEOUT -> R.string.chess_reason_timeout
        ResultReason.STALEMATE -> R.string.chess_reason_stalemate
        ResultReason.INSUFFICIENT_MATERIAL -> R.string.chess_reason_material
        ResultReason.THREEFOLD_REPETITION -> R.string.chess_reason_repetition
        ResultReason.FIFTY_MOVES -> R.string.chess_reason_fifty
        ResultReason.DRAW_AGREED -> R.string.chess_reason_agreed
        ResultReason.ABORTED -> R.string.chess_reason_aborted
        ResultReason.TIMEOUT_VS_INSUFFICIENT -> R.string.chess_reason_timeout_draw
        null -> R.string.chess_reason_aborted
    }
)

/**
 * The end of a game (J149), as the design's "over" screen: the result and why, the player's Int
 * change and new tier if it moved, then the head-to-head counter with the cell that just went up
 * marked +1, and Rematch as the one main action. Someone watching sees the result only.
 */
@Composable
fun GameOverCard(game: ChessGame, state: ChessSessionState, onRematch: () -> Unit, celebrate: Boolean = false) {
    val me = state.me
    val outcome = me?.let { game.outcomeFor(it) }
    val title = when {
        game.status == GameStatus.ABORTED -> stringResource(R.string.chess_over_aborted)
        outcome == Outcome.WIN -> stringResource(R.string.chess_over_win)
        outcome == Outcome.LOSS -> stringResource(R.string.chess_over_loss)
        outcome == Outcome.DRAW -> stringResource(R.string.chess_over_draw)
        game.result == com.pixelquest.app.chess.domain.GameResult.WHITE -> stringResource(R.string.chess_over_white)
        game.result == com.pixelquest.app.chess.domain.GameResult.BLACK -> stringResource(R.string.chess_over_black)
        else -> stringResource(R.string.chess_over_draw)
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
            .testTag(GameOverTags.CARD)
            .comic(bg = if (outcome == Outcome.WIN) PqColors.Gold else PqColors.Paper, radius = PqDimens.RadiusLg, shadow = PqDimens.Shadow)
            .padding(14.dp)
    ) {
        // A win, just now: the title bursts (M06).
        RewardBurst(trigger = if (celebrate && outcome == Outcome.WIN) 1 else 0) {
            Text(text = title.uppercase(), style = PqType.CardTitle, modifier = Modifier.semantics { heading() })
        }
        Text(text = reasonText(game.reason), style = PqType.Body)
        val colour = game.colourOf(me)
        val change = when (colour) {
            ChessColour.WHITE -> game.whiteChange
            ChessColour.BLACK -> game.blackChange
            null -> null
        }
        // Simple Mode: the result, without Int points or tiers (J164).
        if (colour != null && game.rated && game.status == GameStatus.FINISHED && !state.simple) {
            when {
                !game.ratingApplied -> Text(text = stringResource(R.string.chess_over_not_counted), style = PqType.BodyStrong, modifier = Modifier.testTag(GameOverTags.CHANGE))
                // The Int change pops in as a chip (C07).
                change != null -> IntDeltaChip(
                    stringResource(R.string.chess_over_change, if (change >= 0) "+$change" else "\u2212${-change}"),
                    good = change >= 0,
                    modifier = Modifier.testTag(GameOverTags.CHANGE)
                )
            }
            val now = state.points[me]
            if (now != null && change != null && change != 0) {
                val before = ChessConfig.tierOf(now - change)
                val after = ChessConfig.tierOf(now)
                if (before != after) {
                    Text(text = stringResource(R.string.chess_over_tier, stringResource(tierName(after))), style = PqType.MetaStrong, modifier = Modifier.testTag(GameOverTags.TIER))
                }
            }
        }
        val record = state.headToHead.firstOrNull { it.timeControl == null }
        if (record != null && outcome != null) RecordCells(record, outcome, state.names[record.them], celebrate)
        if (outcome != null) {
            PqButton(
                text = stringResource(if (state.rematchSent) R.string.chess_rematch_sent else R.string.chess_rematch),
                onClick = onRematch,
                icon = Icons.Rounded.Replay,
                enabled = !state.rematchSent,
                modifier = Modifier.fillMaxWidth().testTag(GameOverTags.REMATCH)
            )
        }
    }
}

/** YOU · DRAWS · THEM, the cell this game added to marked +1. */
@Composable
private fun RecordCells(record: HeadToHead, outcome: Outcome, them: String?, celebrate: Boolean) {
    val cells = listOf(
        Triple(record.myWins, stringResource(R.string.chess_record_you), outcome == Outcome.WIN),
        Triple(record.draws, stringResource(R.string.chess_record_draws), outcome == Outcome.DRAW),
        Triple(record.theirWins, (them ?: stringResource(R.string.chess_record_them)).uppercase(), outcome == Outcome.LOSS)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .testTag(GameOverTags.RECORD)
            .comic(radius = PqDimens.Radius, border = PqDimens.BorderThin, shadow = 0.dp)
    ) {
        cells.forEachIndexed { i, (value, label, up) ->
            // The cell this game added to rolls up from its old count and flashes (C07).
            var shown by remember(value) { mutableIntStateOf(if (up && celebrate) value - 1 else value) }
            LaunchedEffect(value) { if (shown != value) { kotlinx.coroutines.delay(700); shown = value } }
            Box(Modifier.weight(1f).background(if (i == 0) PqColors.Gold else PqColors.Paper).cellFlash(up && celebrate).padding(10.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    RollingNumber(shown, PqType.display(34), format = { it.toString() })
                    Text(text = label, style = PqType.Label)
                }
                if (up) Text(text = "+1", style = PqType.Button.copy(color = PqColors.Live), modifier = Modifier.align(Alignment.TopEnd))
            }
            if (i < cells.lastIndex) Box(Modifier.width(2.dp).fillMaxHeight().background(PqColors.Ink))
        }
    }
}
