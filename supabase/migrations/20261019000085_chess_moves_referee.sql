-- Migration: 20261019000085_chess_moves_referee.sql
-- Description: The database side of chess-move (docs/chess-design.md, Moves and Clocks), for the
-- service role only. The function checks the move with chess.js; the database keeps the order
-- and the time:
--   * chess_game_state(user, game): the game, its moves so far (UCI) and the server's time, for
--     its players only (not_a_player).
--   * chess_commit_move(...): under the game's lock, a move is taken only from the player whose
--     turn it is (not_your_turn), as the next ply (wrong_ply), in a live game (game_over). The
--     same move sent again at the same ply returns what was stored. Time used is now() minus the
--     last move, by the database's clock; each player's first move uses none. A move that comes
--     after the mover's time ran out isn't taken: the game ends on time instead (as the function
--     says it ends: a win, or a draw if the other side can't mate). A move clears the other
--     side's draw offer. When chess.js says the move ends the game, it ends here, in the same step.
-- Re-runnable: CREATE OR REPLACE.
-- Rollback: DROP FUNCTION public.chess_game_state(UUID, UUID), public.chess_commit_move(UUID, UUID, INTEGER, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT).

CREATE OR REPLACE FUNCTION public.chess_game_state(p_user UUID, p_game UUID)
RETURNS JSONB
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    game public.chess_games%ROWTYPE;
BEGIN
    SELECT * INTO game FROM public.chess_games WHERE id = p_game;
    IF NOT FOUND OR p_user NOT IN (game.white_id, game.black_id) THEN
        RAISE EXCEPTION 'not_a_player' USING ERRCODE = 'insufficient_privilege';
    END IF;
    RETURN jsonb_build_object(
        'game', to_jsonb(game),
        'moves', coalesce((SELECT jsonb_agg(m.uci ORDER BY m.ply) FROM public.chess_moves m WHERE m.game_id = p_game), '[]'::JSONB),
        'now', now()
    );
END;
$$;

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
            status = 'finished', result = p_flag_result, result_reason = p_flag_reason, ended_at = now(),
            white_ms = CASE WHEN white_moves THEN 0 ELSE white_ms END,
            black_ms = CASE WHEN white_moves THEN black_ms ELSE 0 END,
            draw_offered_by = NULL
        WHERE id = p_game RETURNING * INTO game;
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
        status = CASE WHEN p_end_result IS NULL THEN 'active' ELSE 'finished' END,
        result = p_end_result,
        result_reason = p_end_reason,
        started_at = coalesce(started_at, now()),
        ended_at = CASE WHEN p_end_result IS NULL THEN NULL ELSE now() END,
        draw_offered_by = CASE WHEN draw_offered_by = p_user THEN draw_offered_by END
    WHERE id = p_game RETURNING * INTO game;
    RETURN jsonb_build_object('game', to_jsonb(game), 'move', to_jsonb(stored));
END;
$$;

REVOKE ALL ON FUNCTION public.chess_game_state(UUID, UUID) FROM public, anon, authenticated;
REVOKE ALL ON FUNCTION public.chess_commit_move(UUID, UUID, INTEGER, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT) FROM public, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.chess_game_state(UUID, UUID) TO service_role;
GRANT EXECUTE ON FUNCTION public.chess_commit_move(UUID, UUID, INTEGER, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT) TO service_role;
