package com.pixelquest.app.chess.domain

/**
 * Chess's rules on the phone (docs/chess-design.md, The rules): the same values as the server's
 * chess_config, which decides. The phone uses them to show things (a tier, how long a challenge
 * lasts), never to referee.
 */
object ChessConfig {
    const val START_RATING = 200
    const val RATING_FLOOR = 100
    const val K_NEW = 40
    const val K_NEW_GAMES = 10
    const val K_NORMAL = 32
    const val K_HIGH = 16
    const val K_HIGH_ABOVE = 1600
    const val CHALLENGE_MINUTES = 10
    const val ABORT_SECONDS = 30
    const val PAIR_RATED_PER_DAY = 10
    const val MIN_MOVES_EACH = 4
    const val CHALLENGES_PER_HOUR = 10
    const val MOVES_PER_SECOND = 5
    const val SPECTATOR_JOINS_PER_HOUR = 20

    val TIME_CONTROLS: List<TimeControl> = TimeControl.entries

    /** The tiers, lowest first, and the Int points each starts at. */
    val TIERS: List<Pair<Tier, Int>> = listOf(
        Tier.PAWN to 0,
        Tier.KNIGHT to 900,
        Tier.BISHOP to 1100,
        Tier.ROOK to 1300,
        Tier.QUEEN to 1500,
        Tier.KING to 1800,
    )

    /** The tier [points] fall in. */
    fun tierOf(points: Int): Tier = TIERS.last { points >= it.second }.first

    /** The Int points the next tier starts at, or null at the top. */
    fun nextTierAt(points: Int): Int? = TIERS.firstOrNull { it.second > points }?.second
}

/** The rating tiers (their names are string resources: chess_tier_*). */
enum class Tier { PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING }
