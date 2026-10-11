-- Migration: 20261011000002_named_board_rows.sql
-- Description: Nobody is on the leaderboard without a name.
--   * A row could be opted in with an empty display_name, and the board showed a blank.
--     A direct API write could do that. The app avoids it since Day 33: before, its push fell back
--     to "Hero", which names being unique would now refuse.
--   * Rows like that leave the board now.
--   * The check refuses new ones (23514, HTTP 400).

UPDATE public.profiles
SET leaderboard_opt_in = FALSE
WHERE leaderboard_opt_in AND display_name = '';

ALTER TABLE public.profiles
    ADD CONSTRAINT on_the_board_with_a_name CHECK (NOT leaderboard_opt_in OR display_name <> '');
