package com.pixelquest.app.chess

import com.pixelquest.app.chess.rules.Move
import com.pixelquest.app.chess.rules.Position
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The phone's hint rules (J141), checked against the standard perft counts. */
class ChessRulesTest {
    private fun perft(p: Position, depth: Int): Long =
        if (depth == 0) 1 else p.legalMoves().sumOf { perft(p.apply(it), depth - 1) }

    @Test
    fun perft_fromTheStart() {
        val start = Position.of(Position.START)!!
        assertEquals(20L, perft(start, 1))
        assertEquals(400L, perft(start, 2))
        assertEquals(8902L, perft(start, 3))
    }

    @Test
    fun perft_kiwipete_castlesEnPassantAndPromotions() {
        val kiwipete = Position.of("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1")!!
        assertEquals(48L, perft(kiwipete, 1))
        assertEquals(2039L, perft(kiwipete, 2))
        assertEquals(97862L, perft(kiwipete, 3))
    }

    @Test
    fun perft_position3_andPosition4() {
        assertEquals(2812L, perft(Position.of("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1")!!, 3))
        assertEquals(9467L, perft(Position.of("r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1")!!, 3))
    }

    @Test
    fun hints_check_promotion_andMate() {
        val mated = Position.of("r1bqkb1r/pppp1Qpp/2n2n2/4p3/2B1P3/8/PPPP1PPP/RNB1K1NR b KQkq - 0 4")!!
        assertTrue(mated.inCheck())
        assertTrue(mated.isCheckmate())
        val start = Position.of(Position.START)!!
        assertEquals(setOf("e2e3", "e2e4"), start.movesFrom(Move.index("e2")!!).map { it.uci }.toSet())
        val promo = Position.of("8/P7/8/8/8/8/8/k6K w - - 0 1")!!
        assertTrue(promo.promotes(Move.index("a7")!!, Move.index("a8")!!))
        assertFalse(start.promotes(Move.index("e2")!!, Move.index("e4")!!))
        assertEquals(null, Position.of("garbage"))
        assertEquals("e7e8q", Move.ofUci("e7e8q")!!.uci)
    }
}
