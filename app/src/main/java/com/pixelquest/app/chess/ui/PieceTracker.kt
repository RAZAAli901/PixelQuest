package com.pixelquest.app.chess.ui

import kotlin.math.abs

/** A piece on the board with an id that stays with it from move to move (the animations' key). */
data class TrackedPiece(val id: Int, val square: Int, val piece: Char, val promoted: Boolean = false)

/** One position's pieces, the pieces it took off the board, and its number (for one-off effects). */
data class TrackedFrame(val serial: Int, val pieces: List<TrackedPiece>, val captured: List<TrackedPiece>)

/**
 * Follows each piece from one position to the next, so a move animates the piece that moved
 * (C01): a piece still on its square keeps its id; one that moved is the nearest of its kind that
 * left a square; a new queen (or rook, bishop, knight) takes the id of a pawn of its colour that
 * left, marked promoted (C09); and what's gone was captured (C02). Castling moves the rook too.
 * The first position, or a whole new one, just gets fresh ids.
 */
class PieceTracker {
    private var current: List<TrackedPiece> = emptyList()
    private var nextId = 0
    private var serial = 0

    fun update(board: List<Char?>): TrackedFrame {
        val first = serial == 0
        serial++
        val placed = arrayOfNulls<TrackedPiece>(64)
        val left = current.toMutableList()
        // 1. Still on its square.
        match(board, placed, left) { square, piece, old -> old.square == square && old.piece == piece }
        if (!first) {
            // 2. Moved: the nearest of the same kind that left a square.
            match(board, placed, left) { _, piece, old -> old.piece == piece }
            // 3. Promoted: a pawn of that colour that left becomes it.
            match(board, placed, left, promoted = true) { _, piece, old -> old.piece == if (piece.isUpperCase()) 'P' else 'p' }
        }
        // 4. Anything else is new.
        for (square in 0 until 64) {
            val piece = board.getOrNull(square)
            if (piece != null && placed[square] == null) placed[square] = TrackedPiece(nextId++, square, piece)
        }
        current = placed.filterNotNull()
        return TrackedFrame(serial, current, if (first) emptyList() else left)
    }

    /** Gives each square still without a piece the nearest piece from [left] that [fits]. */
    private fun match(
        board: List<Char?>,
        placed: Array<TrackedPiece?>,
        left: MutableList<TrackedPiece>,
        promoted: Boolean = false,
        fits: (square: Int, piece: Char, old: TrackedPiece) -> Boolean
    ) {
        for (square in 0 until 64) {
            val piece = board.getOrNull(square)?.takeIf { placed[square] == null }
            val old = piece?.let { p -> left.filter { fits(square, p, it) }.minByOrNull { distance(it.square, square) } }
            if (piece != null && old != null) {
                placed[square] = TrackedPiece(old.id, square, piece, promoted)
                left.remove(old)
            }
        }
    }

    private fun distance(a: Int, b: Int) = abs(a / 8 - b / 8) + abs(a % 8 - b % 8)
}
