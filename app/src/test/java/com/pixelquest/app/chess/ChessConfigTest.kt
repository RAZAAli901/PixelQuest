package com.pixelquest.app.chess

import com.pixelquest.app.R
import com.pixelquest.app.chess.domain.ChessConfig
import com.pixelquest.app.chess.domain.Tier
import com.pixelquest.app.chess.domain.TimeControl
import com.pixelquest.app.chess.ui.tierName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

/** The phone's chess rules match the server's one config (J124). */
class ChessConfigTest {
    @Test
    fun tiers_byIntPoints() {
        assertEquals(Tier.PAWN, ChessConfig.tierOf(ChessConfig.RATING_FLOOR))
        assertEquals(Tier.PAWN, ChessConfig.tierOf(ChessConfig.START_RATING))
        assertEquals(Tier.KNIGHT, ChessConfig.tierOf(900))
        assertEquals(Tier.ROOK, ChessConfig.tierOf(1499))
        assertEquals(Tier.KING, ChessConfig.tierOf(2400))
        assertEquals(900, ChessConfig.nextTierAt(800))
        assertNull(ChessConfig.nextTierAt(1800))
        assertEquals(R.string.chess_tier_queen, tierName(Tier.QUEEN))
    }

    @Test
    fun theSameValuesAsTheServer() {
        val sql = File("../supabase/migrations/20261019000075_chess_games.sql").takeIf { it.exists() }
            ?: File("supabase/migrations/20261019000075_chess_games.sql")
        val text = sql.readText()
        fun default(column: String) = Regex(column + """ INTEGER NOT NULL DEFAULT (\d+)""").find(text)!!.groupValues[1].toInt()
        assertEquals(ChessConfig.START_RATING, default("start_rating"))
        assertEquals(ChessConfig.RATING_FLOOR, default("rating_floor"))
        assertEquals(ChessConfig.K_NEW, default("k_new"))
        assertEquals(ChessConfig.K_HIGH_ABOVE, default("k_high_above"))
        assertEquals(ChessConfig.CHALLENGE_MINUTES, default("challenge_minutes"))
        assertEquals(ChessConfig.ABORT_SECONDS, default("abort_seconds"))
        assertEquals(ChessConfig.PAIR_RATED_PER_DAY, default("pair_rated_per_day"))
        assertEquals(ChessConfig.SPECTATOR_JOINS_PER_HOUR, default("spectator_joins_per_hour"))
        val tiers = Regex("""\{"name":"(\w+)","from":(\d+)\}""").findAll(text).map { it.groupValues[1].uppercase() to it.groupValues[2].toInt() }.toList()
        assertEquals(ChessConfig.TIERS.map { it.first.name to it.second }, tiers)
        assertEquals(listOf(60, 180, 300, 600), TimeControl.entries.map { it.seconds })
    }
}
