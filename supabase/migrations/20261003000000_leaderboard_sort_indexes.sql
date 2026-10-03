-- Migration: 20261003000000_leaderboard_sort_indexes.sql
-- Description: Indexes matching the two leaderboard orders, so pages, "around you" ranges and rank
-- counts stay fast as the table grows. Partial on leaderboard_opt_in = TRUE, the only rows the
-- leaderboard ever reads.
--
-- Streaks: current_streak DESC, longest_streak DESC, id ASC
-- Levels:  level DESC, total_xp DESC, id ASC

CREATE INDEX IF NOT EXISTS profiles_leaderboard_streak_idx
    ON public.profiles (current_streak DESC, longest_streak DESC, id ASC)
    WHERE leaderboard_opt_in = TRUE;

CREATE INDEX IF NOT EXISTS profiles_leaderboard_level_idx
    ON public.profiles (level DESC, total_xp DESC, id ASC)
    WHERE leaderboard_opt_in = TRUE;
