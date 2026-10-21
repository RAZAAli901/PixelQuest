-- Migration: 20261019000090_chess_finish_game.sql
-- Description: finish_game (docs/chess-design.md, Ratings and records): the one step a chess
-- game ends in. private.chess_finish now, under the game's lock and only for a live game (asked
-- twice, the second changes nothing):
--   * sets the result, the reason and the end time;
--   * for any finished game (not an aborted one): the pair's head-to-head, per time control;
--   * for a rated one: both players' records in that time control (games, wins, draws, losses,
--     peak), their rows made at the config's start rating if new, locked in id order. Each
--     side's Int points change comes in 20261019000091 (the Elo function); until then it's 0.
--   * the game keeps each side's change (white_change, black_change) and whether it counted.
-- chess_commit_move now ends a game through it too, on a mating move or a flag.
-- Re-runnable: ADD COLUMN IF NOT EXISTS, CREATE OR REPLACE.
-- Rollback: re-run 20261019000085 and 086; drop the three columns.

ALTER TABLE public.chess_games ADD COLUMN IF NOT EXISTS white_change INTEGER;
ALTER TABLE public.chess_games ADD COLUMN IF NOT EXISTS black_change INTEGER;
ALTER TABLE public.chess_games ADD COLUMN IF NOT EXISTS rating_applied BOOLEAN NOT NULL DEFAULT FALSE;

-- The Int points change for each side: 0 until the Elo function arrives (20261019000091).
CREATE OR REPLACE FUNCTION private.chess_rating_changes(game public.chess_games, white_points INTEGER, white_games INTEGER, black_points INTEGER, black_games INTEGER)
RETURNS INTEGER[]
LANGUAGE sql
STABLE
SET search_path = ''
AS $$
    SELECT ARRAY[0, 0];
$$;
REVOKE ALL ON FUNCTION private.chess_rating_changes(public.chess_games, INTEGER, INTEGER, INTEGER, INTEGER) FROM public, anon, authenticated;

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

    IF NOT game.rated THEN
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

CREATE OR REPLACE FUNCTION public.chess_commit_move(
    p_user UUID, p_game UUID, p_ply INTEGER, p_uci TEXT, p_san TEXT, p_fen_after TEXT,
    p_end_result TEXT, p_end_reason TEXT, p_flag_result TEXT, p_flag_reason TEXT
)
RETURNS JSONB
LANGUAGE plpgsql
VOLATILE
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    game public.chess_games%ROWTYPE;
    stored public.chess_moves%ROWTYPE;
    white_moves BOOLEAN;
    used BIGINT := 0;
    left_ms BIGINT;
BEGIN
    SELECT * INTO game FROM public.chess_games WHERE id = p_game FOR UPDATE;
    IF NOT FOUND OR p_user NOT IN (game.white_id, game.black_id) THEN
        RAISE EXCEPTION 'not_a_player' USING ERRCODE = 'insufficient_privilege';
    END IF;
    -- Sent again after a lost answer: what was stored, once.
    SELECT * INTO stored FROM public.chess_moves WHERE game_id = p_game AND ply = p_ply;
    IF FOUND THEN
        IF stored.uci = p_uci AND (p_ply % 2 = 1) = (p_user = game.white_id) THEN
            RETURN jsonb_build_object('game', to_jsonb(game), 'move', to_jsonb(stored), 'repeated', TRUE);
        END IF;
        RAISE EXCEPTION 'wrong_ply' USING ERRCODE = 'check_violation';
    END IF;
    IF game.status NOT IN ('waiting', 'active') THEN
        RAISE EXCEPTION 'game_over' USING ERRCODE = 'check_violation';
    END IF;
    IF p_ply <> game.ply + 1 THEN
        RAISE EXCEPTION 'wrong_ply' USING ERRCODE = 'check_violation';
    END IF;
    white_moves := game.turn = 'w';
    IF white_moves <> (p_user = game.white_id) THEN
        RAISE EXCEPTION 'not_your_turn' USING ERRCODE = 'check_violation';
    END IF;

    -- Each side's first move uses no time; after that, the time since the last move.
    IF game.ply >= 2 THEN
        used := (extract(EPOCH FROM (now() - game.last_move_at)) * 1000)::BIGINT;
    END IF;
    left_ms := CASE WHEN white_moves THEN game.white_ms ELSE game.black_ms END - used;
    IF left_ms <= 0 THEN
        UPDATE public.chess_games SET
            white_ms = CASE WHEN white_moves THEN 0 ELSE white_ms END,
            black_ms = CASE WHEN white_moves THEN black_ms ELSE 0 END
        WHERE id = p_game;
        game := private.chess_finish(p_game, p_flag_result, p_flag_reason);
        RETURN jsonb_build_object('game', to_jsonb(game), 'flagged', TRUE);
    END IF;

    INSERT INTO public.chess_moves (game_id, ply, san, uci, fen_after, white_ms_after, black_ms_after)
    VALUES (p_game, p_ply, p_san, p_uci, p_fen_after,
            CASE WHEN white_moves THEN left_ms ELSE game.white_ms END,
            CASE WHEN white_moves THEN game.black_ms ELSE left_ms END)
    RETURNING * INTO stored;
    UPDATE public.chess_games SET
        fen = p_fen_after,
        turn = CASE WHEN white_moves THEN 'b' ELSE 'w' END,
        ply = p_ply,
        white_ms = stored.white_ms_after,
        black_ms = stored.black_ms_after,
        last_move_at = now(),
        status = 'active',
        started_at = coalesce(started_at, now()),
        draw_offered_by = CASE WHEN draw_offered_by = p_user THEN draw_offered_by END
    WHERE id = p_game RETURNING * INTO game;
    -- The move that ends it: the result, records and ratings, all in this same step.
    IF p_end_result IS NOT NULL THEN
        game := private.chess_finish(p_game, p_end_result, p_end_reason);
    END IF;
    RETURN jsonb_build_object('game', to_jsonb(game), 'move', to_jsonb(stored));
END;
$$;

REVOKE ALL ON FUNCTION public.chess_commit_move(UUID, UUID, INTEGER, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT) FROM public, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.chess_commit_move(UUID, UUID, INTEGER, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT) TO service_role;
