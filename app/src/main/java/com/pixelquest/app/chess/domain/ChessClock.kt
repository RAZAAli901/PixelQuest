package com.pixelquest.app.chess.domain

import java.time.Instant

/**
 * The clocks as the phone shows them (docs/chess-design.md, Clocks): counted down from the
 * server's last numbers, never deciding anything. The server keeps time; the phone only has to
 * know how far its own clock is from the server's.
 *
 * Each game update says when the server stamped the last move ([ChessGame.lastMoveAt]). Seen on
 * the phone a little later, it gives a lower bound on the server's clock minus the phone's: the
 * best of the recent ones (the least delayed) is the estimate. So a phone whose clock is minutes
 * off still shows the right time left.
 */
class ClockSync(private val keep: Int = 8) {
    private val samples = ArrayDeque<Long>()

    /** The server-minus-phone offset now believed, in ms (0 until there's a sample). */
    val offsetMs: Long get() = samples.maxOrNull() ?: 0L

    /** A server timestamp [server], seen on the phone at [phoneMs]. */
    fun sample(server: Instant, phoneMs: Long) {
        samples.addLast(server.toEpochMilli() - phoneMs)
        while (samples.size > keep) samples.removeFirst()
    }

    /** The server's time, as best known, at the phone's [phoneMs]. */
    fun serverNow(phoneMs: Long): Long = phoneMs + offsetMs
}

object ChessClock {
    /**
     * [colour]'s time left at the phone's [phoneMs], in ms: the server's number, less the time
     * since the last move if it's that side's turn and its clock is running (each side's first
     * move uses none). Never below 0; a finished game shows its final clocks.
     */
    fun remaining(game: ChessGame, colour: ChessColour, phoneMs: Long, sync: ClockSync): Long {
        val base = game.millisOf(colour)
        val running = game.status == GameStatus.ACTIVE && game.turn == colour && game.ply >= 2
        val since = game.lastMoveAt?.toEpochMilli() ?: return base
        if (!running) return base
        return (base - (sync.serverNow(phoneMs) - since)).coerceAtLeast(0)
    }

    /** "4:59", or "0:09.4" under 10 seconds. */
    fun format(ms: Long): String {
        val total = ms.coerceAtLeast(0)
        val minutes = total / 60_000
        val seconds = (total % 60_000) / 1000
        return if (total < 10_000) "%d:%02d.%d".format(minutes, seconds, (total % 1000) / 100) else "%d:%02d".format(minutes, seconds)
    }
}
