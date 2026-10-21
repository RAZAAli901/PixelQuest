package com.pixelquest.app.testing

import com.pixelquest.app.chess.data.remote.ChallengeAnswerDto
import com.pixelquest.app.chess.data.remote.ChallengeDto
import com.pixelquest.app.chess.data.remote.ChessGameDto
import com.pixelquest.app.chess.data.remote.ChessMoveDto
import com.pixelquest.app.chess.data.remote.ChessRemote
import com.pixelquest.app.chess.data.remote.GameAnswerDto
import com.pixelquest.app.chess.data.remote.HeadToHeadDto
import com.pixelquest.app.chess.data.remote.RatingDto
import com.pixelquest.app.data.remote.SupabaseResult
import java.io.IOException
import java.time.Instant

/**
 * Chess's server in memory, for tests: games, moves and challenges as rows, with the referee's
 * order rules (turn and ply) but no real rules of chess: a test says what each move does. [offline]
 * fails every call as a network error.
 */
class FakeChessRemote(var me: String = "white") : ChessRemote {
    var offline = false
    var now: Instant = Instant.parse("2026-10-22T10:00:00Z")
    val games = linkedMapOf<String, ChessGameDto>()
    val moves = mutableMapOf<String, MutableList<ChessMoveDto>>()
    val challenges = linkedMapOf<String, ChallengeDto>()
    val ratings = mutableListOf<RatingDto>()
    val headToHead = mutableListOf<HeadToHeadDto>()
    val calls = mutableListOf<String>()
    private var nextId = 1

    private fun <T> answer(name: String, block: () -> SupabaseResult<T>): SupabaseResult<T> {
        calls += name
        if (offline) return SupabaseResult.NetworkError(IOException("offline"))
        return block()
    }

    private fun refuse(token: String, code: Int = 409) = SupabaseResult.ServerError(code, token)

    fun seedGame(white: String = "white", black: String = "black", party: String = "party", timeControlSec: Int = 180, status: String = "active"): String {
        val id = "game-${nextId++}"
        games[id] = ChessGameDto(id, party, white, black, timeControlSec, true, status, fen = "start", whiteMs = timeControlSec * 1000L, blackMs = timeControlSec * 1000L, lastMoveAt = now.toString(), createdAt = now.toString())
        return id
    }

    fun end(gameId: String, result: String, reason: String) {
        games[gameId] = games.getValue(gameId).copy(status = "finished", result = result, resultReason = reason, endedAt = now.toString())
    }

    override suspend fun createChallenge(partyId: String, toUser: String?, timeControlSec: Int, colour: String, rated: Boolean) = answer("createChallenge") {
        val id = "challenge-${nextId++}"
        val row = ChallengeDto(id, partyId, me, toUser, timeControlSec, colour, rated, "open", null, now.toString(), now.plusSeconds(600).toString())
        challenges[id] = row
        SupabaseResult.Success(ChallengeAnswerDto(challenge = row))
    }

    override suspend fun respondChallenge(challengeId: String, accept: Boolean) = answer("respondChallenge") {
        val row = challenges[challengeId] ?: return@answer refuse("not_found", 404)
        if (row.status != "open") return@answer refuse("already_answered")
        if (!accept) {
            challenges[challengeId] = row.copy(status = if (row.fromUser == me) "cancelled" else "declined")
            return@answer SupabaseResult.Success(ChallengeAnswerDto(challenge = challenges[challengeId]))
        }
        val white = if (row.colour == "black") me else row.fromUser
        val black = if (white == me) row.fromUser else me
        val game = seedGame(white, black, row.partyId, row.timeControlSec, "waiting")
        challenges[challengeId] = row.copy(status = "accepted", gameId = game, toUser = me)
        SupabaseResult.Success(ChallengeAnswerDto(challenge = challenges[challengeId], game = games[game]))
    }

    override suspend fun move(gameId: String, ply: Int, uci: String) = answer("move") {
        val game = games[gameId] ?: return@answer refuse("not_a_player", 403)
        val stored = moves[gameId]?.firstOrNull { it.ply == ply }
        if (stored != null) return@answer if (stored.uci == uci) SupabaseResult.Success(GameAnswerDto(game, stored, repeated = true)) else refuse("wrong_ply")
        if (game.status != "active" && game.status != "waiting") return@answer refuse("game_over")
        if (ply != game.ply + 1) return@answer refuse("wrong_ply")
        val turnOf = if (game.turn == "w") game.whiteId else game.blackId
        if (turnOf != me) return@answer refuse("not_your_turn")
        val move = ChessMoveDto(gameId, ply, uci, uci, "fen-$ply", game.whiteMs, game.blackMs, now.toString())
        moves.getOrPut(gameId) { mutableListOf() } += move
        games[gameId] = game.copy(ply = ply, turn = if (game.turn == "w") "b" else "w", status = "active", fen = "fen-$ply", lastMoveAt = now.toString())
        SupabaseResult.Success(GameAnswerDto(games[gameId], move))
    }

    override suspend fun resign(gameId: String) = answer("resign") {
        val game = games[gameId] ?: return@answer refuse("not_a_player", 403)
        if (game.status != "active" && game.status != "waiting") return@answer refuse("game_over")
        end(gameId, if (me == game.whiteId) "black" else "white", "resign")
        SupabaseResult.Success(GameAnswerDto(games[gameId]))
    }

    override suspend fun draw(gameId: String, action: String) = answer("draw") {
        val game = games[gameId] ?: return@answer refuse("not_a_player", 403)
        val byOther = game.drawOfferedBy != null && game.drawOfferedBy != me
        when (action) {
            "offer" -> if (byOther) end(gameId, "draw", "draw_agreed") else games[gameId] = game.copy(drawOfferedBy = me)
            "accept" -> if (byOther) end(gameId, "draw", "draw_agreed") else return@answer refuse("no_draw_offer")
            else -> if (byOther) games[gameId] = game.copy(drawOfferedBy = null) else return@answer refuse("no_draw_offer")
        }
        SupabaseResult.Success(GameAnswerDto(games[gameId]))
    }

    override suspend fun claimTimeout(gameId: String) = answer("claimTimeout") {
        val game = games[gameId] ?: return@answer refuse("not_a_player", 403)
        val left = if (game.turn == "w") game.whiteMs else game.blackMs
        if (left > 0) return@answer refuse("not_timed_out")
        end(gameId, if (game.turn == "w") "black" else "white", "timeout")
        SupabaseResult.Success(GameAnswerDto(games[gameId]))
    }

    override suspend fun liveGames(partyId: String) = answer("liveGames") {
        SupabaseResult.Success(games.values.filter { it.partyId == partyId && (it.status == "active" || it.status == "waiting") })
    }

    override suspend fun game(gameId: String) = answer("game") { SupabaseResult.Success(games[gameId]) }
    override suspend fun moves(gameId: String) = answer("moves") { SupabaseResult.Success(moves[gameId].orEmpty().toList()) }
    override suspend fun myLiveGame(me: String) = answer("myLiveGame") {
        SupabaseResult.Success(games.values.firstOrNull { (it.whiteId == me || it.blackId == me) && (it.status == "active" || it.status == "waiting") })
    }
    override suspend fun openChallenges(partyId: String) = answer("openChallenges") {
        SupabaseResult.Success(challenges.values.filter { it.partyId == partyId && it.status == "open" })
    }
    override suspend fun challenge(challengeId: String) = answer("challenge") { SupabaseResult.Success(challenges[challengeId]) }
    override suspend fun ratings(userId: String) = answer("ratings") { SupabaseResult.Success(ratings.filter { it.userId == userId }) }
    override suspend fun headToHead(me: String, other: String) = answer("headToHead") {
        SupabaseResult.Success(headToHead.filter { setOf(it.userLow, it.userHigh) == setOf(me, other) })
    }
    override suspend fun leaderboard(partyId: String, timeControlSec: Int?) = answer("leaderboard") {
        SupabaseResult.Success(ratings.filter { it.timeControlSec == timeControlSec }.sortedByDescending { it.intPoints })
    }
    override suspend fun finishedGames(partyId: String, limit: Int) = answer("finishedGames") {
        SupabaseResult.Success(games.values.filter { it.partyId == partyId && it.status == "finished" }.take(limit))
    }
    override suspend fun watch(gameId: String) = answer("watch") { SupabaseResult.Success(Unit) }
}
