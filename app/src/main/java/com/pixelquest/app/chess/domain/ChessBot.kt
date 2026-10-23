package com.pixelquest.app.chess.domain

import com.pixelquest.app.chess.rules.Move
import com.pixelquest.app.chess.rules.Position
import kotlin.random.Random

/**
 * An offline chess bot ("Pix AI") that calculates moves locally with zero latency.
 */
object ChessBot {
    const val BOT_ID = "pix_bot"
    const val BOT_NAME = "Pix AI (Bot)"
    const val BOT_RATING = 200

    private val PIECE_VALUES = mapOf(
        'p' to 100,
        'n' to 320,
        'b' to 330,
        'r' to 500,
        'q' to 900,
        'k' to 20000,
    )

    fun chooseMove(position: Position): Move? {
        val legal = position.legalMoves()
        if (legal.isEmpty()) return null

        val scored = legal.map { move ->
            var score = 0
            val destPiece = position.pieceAt(move.to)
            if (destPiece != null) {
                val victimVal = PIECE_VALUES[destPiece.lowercaseChar()] ?: 0
                val movingVal = PIECE_VALUES[position.pieceAt(move.from)?.lowercaseChar() ?: 'p'] ?: 100
                score += victimVal * 10 - movingVal
            }
            if (move.promotion != null) {
                score += 800
            }

            val nextPos = position.apply(move)
            if (nextPos.isCheckmate()) {
                score += 50000
            } else if (nextPos.inCheck()) {
                score += 60
            }

            // Central control
            val f = move.to % 8
            val r = 7 - move.to / 8
            if (f in 2..5 && r in 2..5) {
                score += 25
            }

            // Don't hang pieces blindly
            if (nextPos.attacked(move.to, byWhite = position.whiteToMove)) {
                val movingVal = PIECE_VALUES[position.pieceAt(move.from)?.lowercaseChar() ?: 'p'] ?: 100
                score -= (movingVal * 8) / 10
            }

            // Small jitter for varied, dynamic play
            score += Random.nextInt(0, 15)

            move to score
        }

        return scored.maxByOrNull { it.second }?.first ?: legal.random()
    }
}
