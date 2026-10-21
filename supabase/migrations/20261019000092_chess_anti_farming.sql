-- Migration: 20261019000092_chess_anti_farming.sql
-- Description: Anti-farming (docs/chess-design.md, The rules). A rated game changes Int points
-- (and the players' rated records) only if:
--   * the pair has had fewer than chess_config.pair_rated_per_day (10) games count today (UTC),
--     in any time control; and
--   * each side made at least chess_config.min_moves_each (4) moves, unless it ended in checkmate.
-- Otherwise it still counts in the pair's head-to-head, and the game says it didn't count
-- (rating_applied false). The same rules are ratingCounts() in _shared/chess_rating.ts.
-- Re-runnable: CREATE OR REPLACE.
-- Rollback: re-run private.chess_finish from 20261019000090; DROP FUNCTION private.chess_counts_for_rating(public.chess_games).

CREATE OR REPLACE FUNCTION private.chess_counts_for_rating(game public.chess_games)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
    SELECT
        (game.result_reason = 'checkmate' OR (game.ply / 2) >= c.min_moves_each)
        AND (
            SELECT count(*) FROM public.chess_games g
            WHERE g.rating_applied AND g.id <> game.id
              AND least(g.white_id, g.black_id) = least(game.white_id, game.black_id)
              AND greatest(g.white_id, g.black_id) = greatest(game.white_id, game.black_id)
              AND g.ended_at >= date_trunc('day', now() AT TIME ZONE 'UTC') AT TIME ZONE 'UTC'
        ) < c.pair_rated_per_day
    FROM public.chess_config c;
$$;
REVOKE ALL ON FUNCTION private.chess_counts_for_rating(public.chess_games) FROM public, anon, authenticated;

CREATE OR REPLACE FUNCTION private.chess_finish(p_game UUID, p_result TEXT, p_reason TEXT)
RETURNS public.chess_games
LANGUAGE plpgsql
VOLATILE
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    game public.chess_games%ROWTYPE;
    config public.chess_config%ROWTYPE;
    w public.chess_ratings%ROWTYPE;
    b public.chess_ratings%ROWTYPE;
    changes INTEGER[];
    low UUID;
    high UUID;
BEGIN
    SELECT * INTO game FROM public.chess_games WHERE id = p_game FOR UPDATE;
    IF NOT FOUND OR game.status NOT IN ('waiting', 'active') THEN
        RETURN game;
    END IF;
    UPDATE public.chess_games SET
        status = CASE WHEN p_reason = 'aborted' THEN 'aborted' ELSE 'finished' END,
        result = CASE WHEN p_reason = 'aborted' THEN NULL ELSE p_result END,
        result_reason = p_reason,
        ended_at = now(),
        draw_offered_by = NULL
    WHERE id = p_game RETURNING * INTO game;
    IF p_reason = 'aborted' THEN
        RETURN game;
    END IF;

    -- Head-to-head, rated or not.
    low := least(game.white_id, game.black_id);
    high := greatest(game.white_id, game.black_id);
    INSERT INTO public.chess_head_to_head AS h (user_low, user_high, time_control_sec, low_wins, high_wins, draws)
    VALUES (low, high, game.time_control_sec,
            CASE WHEN (p_result = 'white' AND game.white_id = low) OR (p_result = 'black' AND game.black_id = low) THEN 1 ELSE 0 END,
            CASE WHEN (p_result = 'white' AND game.white_id = high) OR (p_result = 'black' AND game.black_id = high) THEN 1 ELSE 0 END,
            CASE WHEN p_result = 'draw' THEN 1 ELSE 0 END)
    ON CONFLICT (user_low, user_high, time_control_sec) DO UPDATE SET
        low_wins = h.low_wins + excluded.low_wins,
        high_wins = h.high_wins + excluded.high_wins,
        draws = h.draws + excluded.draws,
        updated_at = now();

    -- Rated, and not farmed (20261019000092): otherwise only the head-to-head counts it.
    IF NOT game.rated OR NOT private.chess_counts_for_rating(game) THEN
        RETURN game;
    END IF;
    SELECT * INTO config FROM public.chess_config;
    INSERT INTO public.chess_ratings (user_id, time_control_sec, int_points, peak)
    VALUES (game.white_id, game.time_control_sec, config.start_rating, config.start_rating),
           (game.black_id, game.time_control_sec, config.start_rating, config.start_rating)
    ON CONFLICT DO NOTHING;
    -- Locked in id order, so two games ending at once can't deadlock.
    PERFORM 1 FROM public.chess_ratings r
    WHERE r.time_control_sec = game.time_control_sec AND r.user_id IN (game.white_id, game.black_id)
    ORDER BY r.user_id FOR UPDATE;
    SELECT * INTO w FROM public.chess_ratings WHERE user_id = game.white_id AND time_control_sec = game.time_control_sec;
    SELECT * INTO b FROM public.chess_ratings WHERE user_id = game.black_id AND time_control_sec = game.time_control_sec;
    changes := private.chess_rating_changes(game, w.int_points, w.games, b.int_points, b.games);

    UPDATE public.chess_ratings SET
        int_points = int_points + changes[1],
        peak = greatest(peak, int_points + changes[1]),
        games = games + 1,
        wins = wins + CASE WHEN p_result = 'white' THEN 1 ELSE 0 END,
        draws = draws + CASE WHEN p_result = 'draw' THEN 1 ELSE 0 END,
        losses = losses + CASE WHEN p_result = 'black' THEN 1 ELSE 0 END,
        updated_at = now()
    WHERE user_id = game.white_id AND time_control_sec = game.time_control_sec;
    UPDATE public.chess_ratings SET
        int_points = int_points + changes[2],
        peak = greatest(peak, int_points + changes[2]),
        games = games + 1,
        wins = wins + CASE WHEN p_result = 'black' THEN 1 ELSE 0 END,
        draws = draws + CASE WHEN p_result = 'draw' THEN 1 ELSE 0 END,
        losses = losses + CASE WHEN p_result = 'white' THEN 1 ELSE 0 END,
        updated_at = now()
    WHERE user_id = game.black_id AND time_control_sec = game.time_control_sec;
    UPDATE public.chess_games SET white_change = changes[1], black_change = changes[2], rating_applied = TRUE
    WHERE id = p_game RETURNING * INTO game;
    RETURN game;
END;
$$;
REVOKE ALL ON FUNCTION private.chess_finish(UUID, TEXT, TEXT) FROM public, anon, authenticated;
