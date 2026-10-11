-- Migration: 20261011000001_unique_display_names.sql
-- Description: Public names are unique, ignoring case. Two players could both appear as
-- "Titan_Slayer", so anyone could pass as someone else on the leaderboard.
--   * A name stays its owner's while their row exists, also while they're off the board.
--   * Deleting the account frees it.
--   * Empty names (a linked profile that hasn't joined yet) don't count.
-- A write that takes someone else's name fails with 23505 (HTTP 409). The app says the name is taken.
--
-- Rows already sharing a name: the least recently updated one keeps it. The others lose the name
-- and leave the board, and choose a new name when they join again. No hosted project exists yet,
-- so this only tidies test data.

WITH ranked AS (
    SELECT id, row_number() OVER (PARTITION BY lower(display_name) ORDER BY updated_at, id) AS n
    FROM public.profiles
    WHERE display_name <> ''
)
UPDATE public.profiles p
SET display_name = '', leaderboard_opt_in = FALSE
FROM ranked r
WHERE p.id = r.id AND r.n > 1;

CREATE UNIQUE INDEX IF NOT EXISTS profiles_display_name_unique
    ON public.profiles (lower(display_name))
    WHERE display_name <> '';
