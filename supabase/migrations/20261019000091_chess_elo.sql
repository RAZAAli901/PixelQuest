-- Migration: 20261019000091_chess_elo.sql
-- Description: The Int points maths (docs/chess-design.md, Ratings and records), as finish_game
-- applies it: Elo, per time control.
--   * private.chess_elo(...): pure (IMMUTABLE, reads no table). K is 40 for a player's first 10
--     rated games in the time control, then 32, and 16 above 1600. The change is
--     round(K * (S - E)), with E = 1 / (1 + 10^((Rb - Ra) / 400)); new points never go below the
--     floor (100). The same maths is supabase/functions/_shared/chess_rating.ts.
--   * private.chess_rating_changes(...) now uses it with chess_config's values.
-- Re-runnable: CREATE OR REPLACE.
-- Rollback: re-run the stub in 20261019000090; DROP FUNCTION private.chess_elo(...).

CREATE OR REPLACE FUNCTION private.chess_elo(
    ra INTEGER, ga INTEGER, rb INTEGER, gb INTEGER, score_a NUMERIC,
    k_new INTEGER, k_new_games INTEGER, k_normal INTEGER, k_high INTEGER, k_high_above INTEGER, floor_points INTEGER
)
RETURNS INTEGER[]
LANGUAGE sql
IMMUTABLE
SET search_path = ''
AS $$
    WITH k AS (
        SELECT
            CASE WHEN ga < k_new_games THEN k_new WHEN ra > k_high_above THEN k_high ELSE k_normal END AS ka,
            CASE WHEN gb < k_new_games THEN k_new WHEN rb > k_high_above THEN k_high ELSE k_normal END AS kb,
            1 / (1 + power(10::NUMERIC, (rb - ra)::NUMERIC / 400)) AS ea
    )
    SELECT ARRAY[
        greatest(floor_points, ra + round(k.ka * (score_a - k.ea))::INTEGER) - ra,
        greatest(floor_points, rb + round(k.kb * ((1 - score_a) - (1 - k.ea)))::INTEGER) - rb
    ]
    FROM k;
$$;
REVOKE ALL ON FUNCTION private.chess_elo(INTEGER, INTEGER, INTEGER, INTEGER, NUMERIC, INTEGER, INTEGER, INTEGER, INTEGER, INTEGER, INTEGER) FROM public, anon, authenticated;

CREATE OR REPLACE FUNCTION private.chess_rating_changes(game public.chess_games, white_points INTEGER, white_games INTEGER, black_points INTEGER, black_games INTEGER)
RETURNS INTEGER[]
LANGUAGE sql
STABLE
SET search_path = ''
AS $$
    SELECT private.chess_elo(
        white_points, white_games, black_points, black_games,
        CASE game.result WHEN 'white' THEN 1 WHEN 'draw' THEN 0.5 ELSE 0 END,
        c.k_new, c.k_new_games, c.k_normal, c.k_high, c.k_high_above, c.rating_floor
    )
    FROM public.chess_config c;
$$;
REVOKE ALL ON FUNCTION private.chess_rating_changes(public.chess_games, INTEGER, INTEGER, INTEGER, INTEGER) FROM public, anon, authenticated;
