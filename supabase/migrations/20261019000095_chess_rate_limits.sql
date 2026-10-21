-- Migration: 20261019000095_chess_rate_limits.sql
-- Description: Chess's rate limits (docs/chess-design.md, The rules), from chess_config:
--   * challenges: 10 an hour each (chess_create_challenge, 20261019000083);
--   * moves: at most 5 a second in a game (a trigger on chess_moves: rate_limited);
--   * watching: 20 joins an hour each. A member who isn't playing calls chess_watch(game) first,
--     which logs the join (chess_watch_log); the game's channel lets in its players, and members
--     with a join logged in the last 6 hours.
-- Re-runnable: IF NOT EXISTS, CREATE OR REPLACE, DROP ... IF EXISTS.
-- Rollback: drop the trigger, public.chess_watch(UUID), public.chess_watch_log; re-run
--   private.chess_may_watch from 20261019000093.

CREATE OR REPLACE FUNCTION private.chess_move_rate()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    IF (SELECT count(*) FROM public.chess_moves m WHERE m.game_id = NEW.game_id AND m.created_at > clock_timestamp() - INTERVAL '1 second')
        >= (SELECT moves_per_second FROM public.chess_config) THEN
        RAISE EXCEPTION 'rate_limited' USING ERRCODE = 'check_violation';
    END IF;
    NEW.created_at := clock_timestamp();
    RETURN NEW;
END;
$$;
REVOKE ALL ON FUNCTION private.chess_move_rate() FROM public, anon, authenticated;

DROP TRIGGER IF EXISTS chess_move_rate ON public.chess_moves;
CREATE TRIGGER chess_move_rate
    BEFORE INSERT ON public.chess_moves
    FOR EACH ROW
    EXECUTE FUNCTION private.chess_move_rate();

CREATE TABLE IF NOT EXISTS public.chess_watch_log (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    game_id UUID NOT NULL REFERENCES public.chess_games(id) ON DELETE CASCADE,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS chess_watch_log_user ON public.chess_watch_log (user_id, joined_at DESC);
ALTER TABLE public.chess_watch_log ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.chess_watch_log FROM anon, authenticated;

CREATE OR REPLACE FUNCTION public.chess_watch(game_id UUID)
RETURNS JSONB
LANGUAGE plpgsql
VOLATILE
SECURITY DEFINER
SET search_path = ''
AS $$
#variable_conflict use_column
DECLARE
    caller UUID := (SELECT auth.uid());
    game public.chess_games%ROWTYPE;
BEGIN
    SELECT * INTO game FROM public.chess_games g WHERE g.id = chess_watch.game_id;
    IF caller IS NULL OR NOT FOUND OR NOT private.chess_is_study_member(game.party_id, caller) THEN
        RAISE EXCEPTION 'not_found' USING ERRCODE = 'no_data_found';
    END IF;
    IF caller IN (game.white_id, game.black_id) THEN
        RETURN jsonb_build_object('status', 'player');
    END IF;
    PERFORM pg_advisory_xact_lock(hashtext('chess_watch:' || caller::TEXT));
    IF (SELECT count(*) FROM public.chess_watch_log w WHERE w.user_id = caller AND w.joined_at > now() - INTERVAL '1 hour')
        >= (SELECT spectator_joins_per_hour FROM public.chess_config) THEN
        RAISE EXCEPTION 'rate_limited' USING ERRCODE = 'check_violation';
    END IF;
    INSERT INTO public.chess_watch_log (user_id, game_id) VALUES (caller, game.id);
    RETURN jsonb_build_object('status', 'watching');
END;
$$;
REVOKE ALL ON FUNCTION public.chess_watch(UUID) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.chess_watch(UUID) TO authenticated;

CREATE OR REPLACE FUNCTION private.chess_may_watch(topic TEXT)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.chess_games g
        WHERE g.id = private.chess_game_of_topic(topic) AND private.is_active_study_member(g.party_id)
          AND (
              (SELECT auth.uid()) IN (g.white_id, g.black_id)
              OR EXISTS (
                  SELECT 1 FROM public.chess_watch_log w
                  WHERE w.game_id = g.id AND w.user_id = (SELECT auth.uid()) AND w.joined_at > now() - INTERVAL '6 hours'
              )
          )
    );
$$;
REVOKE ALL ON FUNCTION private.chess_may_watch(TEXT) FROM public, anon;
GRANT EXECUTE ON FUNCTION private.chess_may_watch(TEXT) TO authenticated;
