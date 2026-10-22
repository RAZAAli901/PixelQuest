package com.pixelquest.app.config

/**
 * Centralized, named configuration constants replacing magic numbers across network,
 * persistence, UI timeouts, and game mechanics.
 */
object NetworkConfig {
    const val DEFAULT_TIMEOUT_MS = 15_000L
    const val LEADERBOARD_TIMEOUT_MS = 10_000L
    const val GEMINI_TIMEOUT_MS = 25_000L
    const val FLOW_STOP_TIMEOUT_MS = 5_000L
    const val RETRY_BACKOFF_INITIAL_MS = 1_000L
    const val RETRY_BACKOFF_MAX_MS = 30_000L
}

object GameConfig {
    const val BASE_XP_PER_TASK = 50
    const val BONUS_XP_PER_STREAK_DAY = 10
    const val ALL_FIVE_BONUS_XP = 100
    const val STREAK_GRACE_PERIOD_HOURS = 4L
    const val DEFAULT_DAYS_PER_LEVEL = 7
}

object PagingConfig {
    const val DEFAULT_PAGE_SIZE = 20L
    const val PLAYERS_AROUND_YOU_RADIUS = 3
}
