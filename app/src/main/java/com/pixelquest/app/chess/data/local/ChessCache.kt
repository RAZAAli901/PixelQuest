package com.pixelquest.app.chess.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.pixelquest.app.party.data.local.PartyEntity
import kotlinx.coroutines.flow.Flow

// Chess's cache on the phone (docs/chess-design.md): ratings, head-to-head records and finished
// games, so the screens have something while the server answers. A live game is never cached as
// truth: it always comes from the server. Signing out clears all of it.

/** A player's Int points in one time control (seconds). */
@Entity(tableName = "chess_rating", primaryKeys = ["userId", "timeControl"])
data class ChessRatingEntity(
    val userId: String,
    val timeControl: Int,
    val points: Int,
    val games: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val peak: Int
)

/** The player's record against [them]: [timeControl] 0 is all time controls together. */
@Entity(tableName = "chess_head_to_head", primaryKeys = ["me", "them", "timeControl"])
data class ChessHeadToHeadEntity(
    val me: String,
    val them: String,
    val timeControl: Int,
    val myWins: Int,
    val theirWins: Int,
    val draws: Int
)

/** A game that's over, as it ended: kept for the party's recent games. */
@Entity(
    tableName = "chess_finished_game",
    foreignKeys = [ForeignKey(entity = PartyEntity::class, parentColumns = ["id"], childColumns = ["partyId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["partyId", "endedAt"])]
)
data class ChessFinishedGameEntity(
    @androidx.room.PrimaryKey val id: String,
    val partyId: String,
    val whiteId: String,
    val blackId: String,
    val timeControl: Int,
    val rated: Boolean,
    val result: String?,
    val reason: String?,
    val fen: String,
    val ply: Int,
    val endedAt: Long,
    val whiteChange: Int?,
    val blackChange: Int?
)

@Dao
interface ChessDao {
    @Query("SELECT * FROM chess_rating WHERE userId = :userId ORDER BY timeControl")
    fun observeRatings(userId: String): Flow<List<ChessRatingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRatings(rows: List<ChessRatingEntity>)

    @Query("DELETE FROM chess_rating WHERE userId = :userId")
    suspend fun deleteRatings(userId: String)

    /** A player's ratings as the server has them now: replaced whole. */
    @Transaction
    suspend fun replaceRatings(userId: String, rows: List<ChessRatingEntity>) {
        deleteRatings(userId)
        upsertRatings(rows)
    }

    @Query("SELECT * FROM chess_head_to_head WHERE me = :me AND them = :them ORDER BY timeControl")
    suspend fun headToHead(me: String, them: String): List<ChessHeadToHeadEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHeadToHead(rows: List<ChessHeadToHeadEntity>)

    @Query("SELECT * FROM chess_finished_game WHERE partyId = :partyId ORDER BY endedAt DESC LIMIT :limit")
    fun observeFinishedGames(partyId: String, limit: Int): Flow<List<ChessFinishedGameEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFinishedGames(rows: List<ChessFinishedGameEntity>)

    /** A finished game, unless its party has left the phone. */
    @Transaction
    suspend fun keepFinishedGame(game: ChessFinishedGameEntity): Boolean {
        if (partyCount(game.partyId) == 0) return false
        upsertFinishedGames(listOf(game))
        return true
    }

    @Query("SELECT COUNT(*) FROM darood_party WHERE id = :partyId")
    suspend fun partyCount(partyId: String): Int

    /** Signing out: none of it stays (finished games go with their parties too). */
    @Transaction
    suspend fun clearAll() {
        clearRatings()
        clearHeadToHead()
        clearFinishedGames()
    }

    @Query("DELETE FROM chess_rating")
    suspend fun clearRatings()

    @Query("DELETE FROM chess_head_to_head")
    suspend fun clearHeadToHead()

    @Query("DELETE FROM chess_finished_game")
    suspend fun clearFinishedGames()
}
