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
        for (square in 0 until 64) {
            val piece = board.getOrNull(square) ?: continue
            left.firstOrNull { it.square == square && it.piece == piece }?.let { old ->
                placed[square] = old.copy(promoted = false)
                left.remove(old)
            }
        }
        // 2. Moved: the nearest of the same kind that left a square.
        if (!first) for (square in 0 until 64) {
            val piece = board.getOrNull(square) ?: continue
            if (placed[square] != null) continue
            left.filter { it.piece == piece }.minByOrNull { distance(it.square, square) }?.let { old ->
                placed[square] = old.copy(square = square, promoted = false)
                left.remove(old)
            }
        }
        // 3. Promoted: a pawn of that colour that left becomes it.
        if (!first) for (square in 0 until 64) {
            val piece = board.getOrNull(square) ?: continue
            if (placed[square] != null) continue
            val pawn = if (piece.isUpperCase()) 'P' else 'p'
            left.filter { it.piece == pawn }.minByOrNull { distance(it.square, square) }?.let { old ->
                placed[square] = TrackedPiece(old.id, square, piece, promoted = true)
                left.remove(old)
            }
        }
        // 4. Anything else is new.
        for (square in 0 until 64) {
            val piece = board.getOrNull(square) ?: continue
            if (placed[square] == null) placed[square] = TrackedPiece(nextId++, square, piece)
        }
        current = placed.filterNotNull()
        return TrackedFrame(serial, current, if (first) emptyList() else left)
    }

    private fun distance(a: Int, b: Int) = abs(a / 8 - b / 8) + abs(a % 8 - b % 8)
}
