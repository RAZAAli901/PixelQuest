package com.pixelquest.app.chess.data.remote

import com.pixelquest.app.chess.domain.Challenge
import com.pixelquest.app.chess.domain.ChallengeStatus
import com.pixelquest.app.chess.domain.ChessColour
import com.pixelquest.app.chess.domain.ChessGame
import com.pixelquest.app.chess.domain.ChessMove
import com.pixelquest.app.chess.domain.ColourChoice
import com.pixelquest.app.chess.domain.GameResult
import com.pixelquest.app.chess.domain.GameStatus
import com.pixelquest.app.chess.domain.HeadToHead
import com.pixelquest.app.chess.domain.Rating
import com.pixelquest.app.chess.domain.ResultReason
import com.pixelquest.app.chess.domain.TimeControl
import com.pixelquest.app.party.data.remote.parseInstant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Chess's rows and answers as the server sends them (docs/chess-design.md), and their mappings to
// the domain. A row the app can't make sense of (an unknown time control) maps to null and is
// left out, rather than shown wrong.

/** A row of public.chess_games, or a function's "game". */
@Serializable
data class ChessGameDto(
    val id: String,
    @SerialName("party_id") val partyId: String,
    @SerialName("white_id") val whiteId: String,
    @SerialName("black_id") val blackId: String,
    @SerialName("time_control_sec") val timeControlSec: Int,
    val rated: Boolean = true,
    val status: String,
    val result: String? = null,
    @SerialName("result_reason") val resultReason: String? = null,
    val fen: String,
    val turn: String = "w",
    val ply: Int = 0,
    @SerialName("white_ms") val whiteMs: Long,
    @SerialName("black_ms") val blackMs: Long,
    @SerialName("last_move_at") val lastMoveAt: String? = null,
    @SerialName("draw_offered_by") val drawOfferedBy: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("ended_at") val endedAt: String? = null,
    @SerialName("white_change") val whiteChange: Int? = null,
    @SerialName("black_change") val blackChange: Int? = null,
    @SerialName("rating_applied") val ratingApplied: Boolean = false,
    val watchers: Int = 0
) {
    fun toDomain(): ChessGame? {
        val tc = TimeControl.of(timeControlSec) ?: return null
        return ChessGame(
        id = id, partyId = partyId, whiteId = whiteId, blackId = blackId,
        timeControl = tc,
        rated = rated, status = GameStatus.of(status), result = GameResult.of(result), reason = ResultReason.of(resultReason),
        fen = fen, turn = if (turn == "b") ChessColour.BLACK else ChessColour.WHITE,
        whiteMs = whiteMs, blackMs = blackMs, ply = ply,
        lastMoveAt = lastMoveAt?.let(::parseInstant), createdAt = parseInstant(createdAt),
        startedAt = startedAt?.let(::parseInstant), endedAt = endedAt?.let(::parseInstant),
        drawOfferedBy = drawOfferedBy, whiteChange = whiteChange, blackChange = blackChange, ratingApplied = ratingApplied, watchers = watchers
        )
    }
}

/** A row of public.chess_moves. */
@Serializable
data class ChessMoveDto(
    @SerialName("game_id") val gameId: String,
    val ply: Int,
    val san: String,
    val uci: String,
    @SerialName("fen_after") val fenAfter: String,
    @SerialName("white_ms_after") val whiteMsAfter: Long,
    @SerialName("black_ms_after") val blackMsAfter: Long,
    @SerialName("created_at") val createdAt: String
) {
    fun toDomain() = ChessMove(gameId, ply, san, uci, fenAfter, whiteMsAfter, blackMsAfter, parseInstant(createdAt))
}

/** A row of public.chess_challenges. */
@Serializable
data class ChallengeDto(
    val id: String,
    @SerialName("party_id") val partyId: String,
    @SerialName("from_user") val fromUser: String,
    @SerialName("to_user") val toUser: String? = null,
    @SerialName("time_control_sec") val timeControlSec: Int,
    val colour: String = "random",
    val rated: Boolean = true,
    val status: String,
    @SerialName("game_id") val gameId: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("expires_at") val expiresAt: String
) {
    fun toDomain(): Challenge? {
        val tc = TimeControl.of(timeControlSec) ?: return null
        return Challenge(id, partyId, fromUser, toUser, tc, ColourChoice.of(colour), rated, ChallengeStatus.of(status), parseInstant(expiresAt), parseInstant(createdAt), gameId)
    }
}

/** A row of public.chess_ratings, or of chess_party_leaderboard (the best: no time control). */
@Serializable
data class RatingDto(
    @SerialName("user_id") val userId: String,
    @SerialName("time_control_sec") val timeControlSec: Int? = null,
    @SerialName("int_points") val intPoints: Int,
    val games: Int = 0,
    val wins: Int = 0,
    val draws: Int = 0,
    val losses: Int = 0,
    val peak: Int? = null,
    val place: Int? = null
) {
    /** [best] stands for the time control of a "best of all" row. */
    fun toDomain(best: TimeControl = TimeControl.FIVE): Rating? {
        val tc = if (timeControlSec == null) best else TimeControl.of(timeControlSec) ?: return null
        return Rating(userId, tc, intPoints, games, wins, draws, losses, peak ?: intPoints)
    }
}

/** A row of public.chess_head_to_head_totals: a pair, low id first. */
@Serializable
data class HeadToHeadDto(
    @SerialName("user_low") val userLow: String,
    @SerialName("user_high") val userHigh: String,
    @SerialName("time_control_sec") val timeControlSec: Int? = null,
    @SerialName("low_wins") val lowWins: Int,
    @SerialName("high_wins") val highWins: Int,
    val draws: Int
) {
    /** The record from [me]'s side; null if [me] isn't one of the pair. */
    fun toDomain(me: String): HeadToHead? {
        val tc = timeControlSec?.let { TimeControl.of(it) ?: return null }
        return when (me) {
            userLow -> HeadToHead(me, userHigh, tc, lowWins, highWins, draws)
            userHigh -> HeadToHead(me, userLow, tc, highWins, lowWins, draws)
            else -> null
        }
    }
}

/** What chess_game_state and the move functions answer with. */
@Serializable
data class GameAnswerDto(
    val game: ChessGameDto? = null,
    val move: ChessMoveDto? = null,
    val repeated: Boolean = false,
    val error: String? = null
)

@Serializable
data class ChallengeAnswerDto(val challenge: ChallengeDto? = null, val game: ChessGameDto? = null, val error: String? = null)

@Serializable
data class LiveGamesDto(val games: List<ChessGameDto> = emptyList())
