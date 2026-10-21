-- Migration: 20261019000088_chess_timeouts.sql
-- Description: Games ended on time (docs/chess-design.md, Clocks), for the service role only:
--   * private.chess_left_ms(game): the side to move's time left now, by the database's clock
--     (each side's first move uses none, so before ply 2 nothing runs).
--   * chess_claim_timeout(user, game, result, reason): a player of a live game claims the other
--     side's clock ran out; refused (not_timed_out) unless it has, by the server's time. With no
--     user, it's the sweep's, for any live game. The ending (a win, or a draw against bare
--     material) comes from the function, which reads the position with chess.js.
--   * chess_due_games(): live games whose clock has run out, for the one-minute sweep.
-- Re-runnable: CREATE OR REPLACE.
-- Rollback: DROP FUNCTION public.chess_claim_timeout(UUID, UUID, TEXT, TEXT), public.chess_due_games(), private.chess_left_ms(public.chess_games).

CREATE OR REPLACE FUNCTION private.chess_left_ms(game public.chess_games)
RETURNS BIGINT
LANGUAGE sql
STABLE
SET search_path = ''
AS $$
    SELECT CASE WHEN game.turn = 'w' THEN game.white_ms ELSE game.black_ms END
        - CASE WHEN game.ply >= 2 THEN (extract(EPOCH FROM (now() - game.last_move_at)) * 1000)::BIGINT ELSE 0 END;
$$;
REVOKE ALL ON FUNCTION private.chess_left_ms(public.chess_games) FROM public, anon, authenticated;

CREATE OR REPLACE FUNCTION public.chess_claim_timeout(p_user UUID, p_game UUID, p_result TEXT, p_reason TEXT)
RETURNS JSONB
LANGUAGE plpgsql
VOLATILE
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    game public.chess_games%ROWTYPE;
BEGIN
    SELECT * INTO game FROM public.chess_games WHERE id = p_game FOR UPDATE;
    IF NOT FOUND OR (p_user IS NOT NULL AND p_user NOT IN (game.white_id, game.black_id)) THEN
        RAISE EXCEPTION 'not_a_player' USING ERRCODE = 'insufficient_privilege';
    END IF;
    IF game.status NOT IN ('waiting', 'active') THEN
        RAISE EXCEPTION 'game_over' USING ERRCODE = 'check_violation';
    END IF;
    IF p_reason NOT IN ('timeout', 'timeout_vs_insufficient') OR private.chess_left_ms(game) > 0 THEN
        RAISE EXCEPTION 'not_timed_out' USING ERRCODE = 'check_violation';
    END IF;
    UPDATE public.chess_games SET
        white_ms = CASE WHEN turn = 'w' THEN 0 ELSE white_ms END,
        black_ms = CASE WHEN turn = 'b' THEN 0 ELSE black_ms END
    WHERE id = p_game;
    game := private.chess_finish(p_game, p_result, p_reason);
    RETURN jsonb_build_object('game', to_jsonb(game));
END;
$$;

CREATE OR REPLACE FUNCTION public.chess_due_games()
RETURNS TABLE (id UUID, fen TEXT, turn TEXT)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
    SELECT g.id, g.fen, g.turn FROM public.chess_games g
    WHERE g.status = 'active' AND g.ply >= 2 AND private.chess_left_ms(g) <= 0
    ORDER BY g.last_move_at
    LIMIT 200;
$$;

REVOKE ALL ON FUNCTION public.chess_claim_timeout(UUID, UUID, TEXT, TEXT), public.chess_due_games() FROM public, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.chess_claim_timeout(UUID, UUID, TEXT, TEXT), public.chess_due_games() TO service_role;
