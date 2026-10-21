package com.pixelquest.app.chess.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.pixelquest.app.R
import com.pixelquest.app.chess.domain.ChessMove

/** A piece's name in words. */
@StringRes
fun pieceName(piece: Char): Int = when (piece.lowercaseChar()) {
    'k' -> R.string.chess_piece_king
    'q' -> R.string.chess_piece_queen
    'r' -> R.string.chess_piece_rook
    'b' -> R.string.chess_piece_bishop
    'n' -> R.string.chess_piece_knight
    else -> R.string.chess_piece_pawn
}

/** A square for TalkBack (J165): "e4, white pawn", or "e4, empty", and "a move here" for a dot. */
@Composable
fun squareSaid(name: String, piece: Char?, target: Boolean): String {
    val what = piece?.let {
        stringResource(R.string.chess_square_piece, stringResource(if (it.isUpperCase()) R.string.chess_white else R.string.chess_black), stringResource(pieceName(it))).lowercase()
    } ?: stringResource(R.string.chess_square_empty)
    val base = stringResource(R.string.chess_square_said, name, what)
    return if (target) base + ", " + stringResource(R.string.chess_square_target) else base
}

/**
 * A move in words (J165): "White knight to f3", "Black pawn takes on d5, check", "White queen
 * takes on f7, checkmate". The piece is read from the move's notation, the side from its number.
 */
@Composable
fun moveSaid(move: ChessMove): String {
    val white = move.ply % 2 == 1
    val san = move.san
    val side = stringResource(if (white) R.string.chess_white else R.string.chess_black)
    if (san.startsWith("O-O")) {
        val castle = stringResource(if (san.startsWith("O-O-O")) R.string.chess_said_castle_long else R.string.chess_said_castle_short, side)
        return castle + suffix(san)
    }
    val piece = san.first().takeIf { it in "KQRBN" } ?: 'P'
    val to = move.uci.substring(2, 4)
    val text = stringResource(
        if ('x' in san) R.string.chess_said_takes else R.string.chess_said_to,
        side, stringResource(pieceName(piece)).lowercase(), to
    )
    val promoted = move.uci.getOrNull(4)?.let { ", " + stringResource(R.string.chess_said_promotes, stringResource(pieceName(it)).lowercase()) }.orEmpty()
    return text + promoted + suffix(san)
}

@Composable
private fun suffix(san: String): String = when {
    san.endsWith("#") -> ", " + stringResource(R.string.chess_said_mate)
    san.endsWith("+") -> ", " + stringResource(R.string.chess_said_check)
    else -> ""
}
