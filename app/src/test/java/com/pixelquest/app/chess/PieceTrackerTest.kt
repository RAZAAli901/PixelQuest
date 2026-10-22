package com.pixelquest.app.chess

import com.pixelquest.app.chess.domain.BoardPosition
import com.pixelquest.app.chess.domain.squareIndex
import com.pixelquest.app.chess.ui.PieceTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The animations follow each piece by its id (C01, C02, C09). */
class PieceTrackerTest {
    private fun board(fen: String) = BoardPosition.of(fen).pieces
    private fun sq(name: String) = squareIndex(name)!!

    @Test
    fun aMove_keepsThePiecesId_andNothingIsCaptured() {
        val tracker = PieceTracker()
        val start = tracker.update(board("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"))
        assertEquals(32, start.pieces.size)
        val pawn = start.pieces.first { it.square == sq("e2") }.id
        val next = tracker.update(board("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"))
        assertEquals(pawn, next.pieces.first { it.square == sq("e4") }.id)
        assertTrue(next.captured.isEmpty())
        assertEquals(start.pieces.map { it.id }.toSet(), next.pieces.map { it.id }.toSet())
    }

    @Test
    fun aCapture_isReported_andTheCapturerKeepsItsId() {
        val tracker = PieceTracker()
        val before = tracker.update(board("4k3/8/8/3p4/4P3/8/8/4K3 w - - 0 1"))
        val white = before.pieces.first { it.square == sq("e4") }.id
        val after = tracker.update(board("4k3/8/8/3P4/8/8/8/4K3 b - - 0 1"))
        assertEquals(white, after.pieces.first { it.square == sq("d5") }.id)
        assertEquals(listOf('p'), after.captured.map { it.piece })
        assertEquals(sq("d5"), after.captured.single().square)
    }

    @Test
    fun castling_movesTheRookToo() {
        val tracker = PieceTracker()
        val before = tracker.update(board("4k3/8/8/8/8/8/8/4K2R w K - 0 1"))
        val rook = before.pieces.first { it.piece == 'R' }.id
        val after = tracker.update(board("4k3/8/8/8/8/8/8/5RK1 b - - 1 1"))
        assertEquals(rook, after.pieces.first { it.square == sq("f1") }.id)
        assertTrue(after.captured.isEmpty())
    }

    @Test
    fun aPromotion_isThePawnsId_markedPromoted() {
        val tracker = PieceTracker()
        val before = tracker.update(board("4k3/P7/8/8/8/8/8/4K3 w - - 0 1"))
        val pawn = before.pieces.first { it.piece == 'P' }.id
        val after = tracker.update(board("Q3k3/8/8/8/8/8/8/4K3 b - - 0 1"))
        val queen = after.pieces.first { it.piece == 'Q' }
        assertEquals(pawn, queen.id)
        assertTrue(queen.promoted)
        assertTrue(after.captured.isEmpty())
        // The next move, it's just a queen.
        val later = tracker.update(board("4k3/8/8/8/8/8/8/Q3K3 b - - 0 1"))
        assertEquals(false, later.pieces.first { it.piece == 'Q' }.promoted)
    }
}
