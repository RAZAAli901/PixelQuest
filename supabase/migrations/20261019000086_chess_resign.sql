-- Migration: 20261019000086_chess_resign.sql
-- Description: Resigning (docs/chess-design.md), for the service role only:
--   * private.chess_finish(game, result, reason): the one way a game ends (a live one only; an
--     ended game stays as it is, so asking twice changes nothing). Ratings and records join it in
--     20261019000090 (finish_game).
--   * chess_resign(user, game): a player of a live game resigns; the other side wins.
-- Re-runnable: CREATE OR REPLACE.
-- Rollback: DROP FUNCTION public.chess_resign(UUID, UUID), private.chess_finish(UUID, TEXT, TEXT).

CREATE OR REPLACE FUNCTION private.chess_finish(p_game UUID, p_result TEXT, p_reason TEXT)
RETURNS public.chess_games
LANGUAGE plpgsql
VOLATILE
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    game public.chess_games%ROWTYPE;
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
    RETURN game;
END;
$$;
REVOKE ALL ON FUNCTION private.chess_finish(UUID, TEXT, TEXT) FROM public, anon, authenticated;

CREATE OR REPLACE FUNCTION public.chess_resign(p_user UUID, p_game UUID)
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
    IF NOT FOUND OR p_user NOT IN (game.white_id, game.black_id) THEN
        RAISE EXCEPTION 'not_a_player' USING ERRCODE = 'insufficient_privilege';
    END IF;
    IF game.status NOT IN ('waiting', 'active') THEN
        RAISE EXCEPTION 'game_over' USING ERRCODE = 'check_violation';
    END IF;
    game := private.chess_finish(p_game, CASE WHEN p_user = game.white_id THEN 'black' ELSE 'white' END, 'resign');
    RETURN jsonb_build_object('game', to_jsonb(game));
END;
$$;

REVOKE ALL ON FUNCTION public.chess_resign(UUID, UUID) FROM public, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.chess_resign(UUID, UUID) TO service_role;
