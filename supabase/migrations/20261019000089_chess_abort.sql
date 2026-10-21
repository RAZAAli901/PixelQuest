-- Migration: 20261019000089_chess_abort.sql
-- Description: The abort rule (docs/chess-design.md): a player who hasn't made their first move
-- within chess_config.abort_seconds (30) of the game's start, or of White's first move, aborts the
-- game. No result, no rating change. For the service role only:
--   * chess_abort_if_stalled(game): checked under the game's lock, by the database's clock
--     (chess-move asks it before a first move, so a late one aborts instead);
--   * chess_abort_stalled(): every such game, for the one-minute sweep.
-- Re-runnable: CREATE OR REPLACE.
-- Rollback: DROP FUNCTION public.chess_abort_if_stalled(UUID), public.chess_abort_stalled().

CREATE OR REPLACE FUNCTION public.chess_abort_if_stalled(p_game UUID)
RETURNS JSONB
LANGUAGE plpgsql
VOLATILE
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    game public.chess_games%ROWTYPE;
    wait_s INTEGER := (SELECT abort_seconds FROM public.chess_config);
BEGIN
    SELECT * INTO game FROM public.chess_games WHERE id = p_game FOR UPDATE;
    IF FOUND AND game.status IN ('waiting', 'active') AND game.ply < 2
       AND game.last_move_at < now() - make_interval(secs => wait_s) THEN
        game := private.chess_finish(p_game, NULL, 'aborted');
        RETURN jsonb_build_object('aborted', TRUE, 'game', to_jsonb(game));
    END IF;
    RETURN jsonb_build_object('aborted', FALSE);
END;
$$;

CREATE OR REPLACE FUNCTION public.chess_abort_stalled()
RETURNS INTEGER
LANGUAGE plpgsql
VOLATILE
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    stalled UUID;
    aborted INTEGER := 0;
BEGIN
    FOR stalled IN
        SELECT g.id FROM public.chess_games g
        WHERE g.status IN ('waiting', 'active') AND g.ply < 2
          AND g.last_move_at < now() - make_interval(secs => (SELECT abort_seconds FROM public.chess_config))
        LIMIT 200
    LOOP
        IF (public.chess_abort_if_stalled(stalled) ->> 'aborted')::BOOLEAN THEN
            aborted := aborted + 1;
        END IF;
    END LOOP;
    RETURN aborted;
END;
$$;

REVOKE ALL ON FUNCTION public.chess_abort_if_stalled(UUID), public.chess_abort_stalled() FROM public, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.chess_abort_if_stalled(UUID), public.chess_abort_stalled() TO service_role;
