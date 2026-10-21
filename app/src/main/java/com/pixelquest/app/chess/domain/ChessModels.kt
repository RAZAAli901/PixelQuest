package com.pixelquest.app.chess.domain

import java.time.Instant

// Chess in study parties (docs/chess-design.md): what the server says, as the app holds it. The
// server referees everything; nothing here decides a move, a clock or a result.

/** The time controls the server allows (chess_config): minutes a side, no increment. */
enum class TimeControl(val seconds: Int) {
    ONE(60), THREE(180), FIVE(300), TEN(600);

    val millis: Long get() = seconds * 1_000L

    companion object {
        fun of(seconds: Int): TimeControl? = entries.firstOrNull { it.seconds == seconds }
    }
}

enum class ChessColour { WHITE, BLACK }

/** The colour a challenger asks for. */
enum class ColourChoice(val wire: String) {
    WHITE("white"), BLACK("black"), RANDOM("random");

    companion object {
        fun of(wire: String?): ColourChoice = entries.firstOrNull { it.wire == wire } ?: RANDOM
    }
}

enum class GameStatus(val wire: String) {
    /** Made on accept: the clocks are set, not yet running. */
    WAITING("waiting"),
    ACTIVE("active"),
    FINISHED("finished"),
    ABORTED("aborted");

    val live: Boolean get() = this == WAITING || this == ACTIVE

    companion object {
        fun of(wire: String?): GameStatus = entries.firstOrNull { it.wire == wire } ?: FINISHED
    }
}

enum class GameResult(val wire: String) {
    WHITE("white"), BLACK("black"), DRAW("draw");

    companion object {
        fun of(wire: String?): GameResult? = entries.firstOrNull { it.wire == wire }
    }
}

enum class ResultReason(val wire: String) {
    CHECKMATE("checkmate"),
    RESIGN("resign"),
    TIMEOUT("timeout"),
    STALEMATE("stalemate"),
    INSUFFICIENT_MATERIAL("insufficient_material"),
    THREEFOLD_REPETITION("threefold_repetition"),
    FIFTY_MOVES("fifty_moves"),
    DRAW_AGREED("draw_agreed"),
    ABORTED("aborted"),
    TIMEOUT_VS_INSUFFICIENT("timeout_vs_insufficient");

    companion object {
        fun of(wire: String?): ResultReason? = entries.firstOrNull { it.wire == wire }
    }
}

/** A game as the server last said it: its position, whose turn, and both clocks at [lastMoveAt]. */
data class ChessGame(
    val id: String,
    val partyId: String,
    val whiteId: String,
    val blackId: String,
    val timeControl: TimeControl,
    val rated: Boolean,
    val status: GameStatus,
    val result: GameResult?,
    val reason: ResultReason?,
    val fen: String,
    val turn: ChessColour,
    val whiteMs: Long,
    val blackMs: Long,
    val ply: Int,
    val lastMoveAt: Instant?,
    val createdAt: Instant,
    val startedAt: Instant?,
    val endedAt: Instant?,
    /** Who has a draw offer standing, if anyone. */
    val drawOfferedBy: String? = null
) {
    fun colourOf(userId: String?): ChessColour? = when (userId) {
        whiteId -> ChessColour.WHITE
        blackId -> ChessColour.BLACK
        else -> null
    }

    fun opponentOf(userId: String): String? = when (userId) {
        whiteId -> blackId
        blackId -> whiteId
        else -> null
    }
}

/** One move the server took: ply 1 is White's first. */
data class ChessMove(
    val gameId: String,
    val ply: Int,
    val san: String,
    val uci: String,
    val fenAfter: String,
    val whiteMsAfter: Long,
    val blackMsAfter: Long,
    val createdAt: Instant
)

enum class ChallengeStatus(val wire: String) {
    OPEN("open"), ACCEPTED("accepted"), DECLINED("declined"), CANCELLED("cancelled"), EXPIRED("expired");

    companion object {
        fun of(wire: String?): ChallengeStatus = entries.firstOrNull { it.wire == wire } ?: EXPIRED
    }
}

/** A challenge to one member ([toUser]), or open to the party's room (null). */
data class Challenge(
    val id: String,
    val partyId: String,
    val fromUser: String,
    val toUser: String?,
    val timeControl: TimeControl,
    val colour: ColourChoice,
    val rated: Boolean,
    val status: ChallengeStatus,
    val expiresAt: Instant,
    val createdAt: Instant,
    /** The game it became, once accepted. */
    val gameId: String? = null
)

/** A player's Int points for one time control, and their record in it. */
data class Rating(
    val userId: String,
    val timeControl: TimeControl,
    val points: Int,
    val games: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val peak: Int
)

/** Two players' record against each other: [timeControl] null for all time controls together. */
data class HeadToHead(
    val me: String,
    val them: String,
    val timeControl: TimeControl?,
    val myWins: Int,
    val theirWins: Int,
    val draws: Int
) {
    val games: Int get() = myWins + theirWins + draws
}

/** What a live game's channel brings (docs/chess-design.md, Realtime). */
sealed interface GameEvent {
    /** The game as it now stands, after a move, an end or an offer. */
    data class Updated(val game: ChessGame, val move: ChessMove?) : GameEvent

    /** How many are watching. */
    data class Watchers(val count: Int) : GameEvent

    /** The connection dropped; the repository is trying again. */
    data object Disconnected : GameEvent
}
