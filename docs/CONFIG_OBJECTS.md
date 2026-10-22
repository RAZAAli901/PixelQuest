# Configuration Objects

Magic numbers for timeouts, network retries, and game mechanics are centralized in `com.pixelquest.app.config.AppConfig.kt`.

## 1. Network & Timeout Config (`NetworkConfig`)

- `DEFAULT_TIMEOUT_MS`: Standard remote Supabase calls (15,000ms).
- `LEADERBOARD_TIMEOUT_MS`: Snappy leaderboard fetching timeout (10,000ms).
- `GEMINI_TIMEOUT_MS`: Generative AI inference timeout (25,000ms).
- `FLOW_STOP_TIMEOUT_MS`: `SharingStarted.WhileSubscribed` timeout for screen flows (5,000ms).
- `RETRY_BACKOFF_INITIAL_MS`: Initial exponential retry delay (1,000ms).
- `RETRY_BACKOFF_MAX_MS`: Maximum exponential backoff ceiling (30,000ms).

## 2. Gameplay Config (`GameConfig`)

- `BASE_XP_PER_TASK`: Default experience points awarded per completed quest (50 XP).
- `BONUS_XP_PER_STREAK_DAY`: Scaling bonus points awarded per active consecutive streak day (10 XP/day).
- `ALL_FIVE_BONUS_XP`: Bonus XP for completing all five daily prayers (100 XP).
- `STREAK_GRACE_PERIOD_HOURS`: Morning grace period hours before nightly streak evaluation fails a streak (4 hours).
- `DEFAULT_DAYS_PER_LEVEL`: Baseline target days needed to achieve a hero level promotion (7 days).

## 3. Paging Config (`PagingConfig`)

- `DEFAULT_PAGE_SIZE`: Default query limit for list endpoints (20 items).
- `PLAYERS_AROUND_YOU_RADIUS`: Number of neighboring players loaded above and below the current user rank (3 players).
