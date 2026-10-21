-- Migration: 20261019000080_chess_constraints.sql
-- Description: Chess's rules as constraints (docs/chess-design.md), so not even a bug in a
-- function can store a game that breaks them:
--   * the time control is one of chess_time_controls;
--   * white and black are different players;
--   * status, result and reason come from fixed lists, and a finished game has a result and a
--     reason (an aborted one, the reason only);
--   * a game's turn is 'w' or 'b', and a challenge's colour and status come from fixed lists;
--   * one live game (waiting or active) per player: a trigger, under a lock per player.
-- Re-runnable: DROP CONSTRAINT IF EXISTS, CREATE OR REPLACE.
-- Rollback: drop the constraints and private.chess_one_live_game().

ALTER TABLE public.chess_games DROP CONSTRAINT IF EXISTS chess_games_time_control;
ALTER TABLE public.chess_games ADD CONSTRAINT chess_games_time_control
    FOREIGN KEY (time_control_sec) REFERENCES public.chess_time_controls(seconds);
ALTER TABLE public.chess_games DROP CONSTRAINT IF EXISTS chess_games_two_players;
ALTER TABLE public.chess_games ADD CONSTRAINT chess_games_two_players CHECK (white_id <> black_id);
ALTER TABLE public.chess_games DROP CONSTRAINT IF EXISTS chess_games_status;
ALTER TABLE public.chess_games ADD CONSTRAINT chess_games_status CHECK (status IN ('waiting', 'active', 'finished', 'aborted'));
ALTER TABLE public.chess_games DROP CONSTRAINT IF EXISTS chess_games_turn;
ALTER TABLE public.chess_games ADD CONSTRAINT chess_games_turn CHECK (turn IN ('w', 'b'));
ALTER TABLE public.chess_games DROP CONSTRAINT IF EXISTS chess_games_result;
ALTER TABLE public.chess_games ADD CONSTRAINT chess_games_result CHECK (result IS NULL OR result IN ('white', 'black', 'draw'));
ALTER TABLE public.chess_games DROP CONSTRAINT IF EXISTS chess_games_reason;
ALTER TABLE public.chess_games ADD CONSTRAINT chess_games_reason CHECK (result_reason IS NULL OR result_reason IN (
    'checkmate', 'resign', 'timeout', 'stalemate', 'insufficient_material', 'threefold_repetition',
    'fifty_moves', 'draw_agreed', 'aborted', 'timeout_vs_insufficient'
));
ALTER TABLE public.chess_games DROP CONSTRAINT IF EXISTS chess_games_ended;
ALTER TABLE public.chess_games ADD CONSTRAINT chess_games_ended CHECK (
    (status IN ('waiting', 'active') AND result IS NULL AND result_reason IS NULL AND ended_at IS NULL)
    OR (status = 'finished' AND result IS NOT NULL AND result_reason IS NOT NULL AND result_reason <> 'aborted' AND ended_at IS NOT NULL)
    OR (status = 'aborted' AND result IS NULL AND result_reason = 'aborted' AND ended_at IS NOT NULL)
);
ALTER TABLE public.chess_games DROP CONSTRAINT IF EXISTS chess_games_draw_offer;
ALTER TABLE public.chess_games ADD CONSTRAINT chess_games_draw_offer CHECK (draw_offered_by IS NULL OR draw_offered_by IN (white_id, black_id));

ALTER TABLE public.chess_challenges DROP CONSTRAINT IF EXISTS chess_challenges_time_control;
ALTER TABLE public.chess_challenges ADD CONSTRAINT chess_challenges_time_control
    FOREIGN KEY (time_control_sec) REFERENCES public.chess_time_controls(seconds);
ALTER TABLE public.chess_challenges DROP CONSTRAINT IF EXISTS chess_challenges_colour;
ALTER TABLE public.chess_challenges ADD CONSTRAINT chess_challenges_colour CHECK (colour IN ('white', 'black', 'random'));
ALTER TABLE public.chess_challenges DROP CONSTRAINT IF EXISTS chess_challenges_status;
ALTER TABLE public.chess_challenges ADD CONSTRAINT chess_challenges_status CHECK (status IN ('open', 'accepted', 'declined', 'cancelled', 'expired'));
ALTER TABLE public.chess_challenges DROP CONSTRAINT IF EXISTS chess_challenges_not_self;
ALTER TABLE public.chess_challenges ADD CONSTRAINT chess_challenges_not_self CHECK (to_user IS NULL OR to_user <> from_user);

CREATE OR REPLACE FUNCTION private.chess_one_live_game()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    IF NEW.status NOT IN ('waiting', 'active') THEN
        RETURN NEW;
    END IF;
    -- In id order, so two games made at once for the same players can't deadlock.
    PERFORM pg_advisory_xact_lock(hashtext('chess_live:' || least(NEW.white_id, NEW.black_id)::TEXT));
    PERFORM pg_advisory_xact_lock(hashtext('chess_live:' || greatest(NEW.white_id, NEW.black_id)::TEXT));
    IF EXISTS (
        SELECT 1 FROM public.chess_games g
        WHERE g.id <> NEW.id AND g.status IN ('waiting', 'active')
          AND (g.white_id IN (NEW.white_id, NEW.black_id) OR g.black_id IN (NEW.white_id, NEW.black_id))
    ) THEN
        RAISE EXCEPTION 'already_playing' USING ERRCODE = 'unique_violation',
            DETAIL = 'A player can have one live game at a time.';
    END IF;
    RETURN NEW;
END;
$$;
REVOKE ALL ON FUNCTION private.chess_one_live_game() FROM public, anon, authenticated;

DROP TRIGGER IF EXISTS chess_one_live_game ON public.chess_games;
CREATE TRIGGER chess_one_live_game
    BEFORE INSERT OR UPDATE OF status, white_id, black_id ON public.chess_games
    FOR EACH ROW
    EXECUTE FUNCTION private.chess_one_live_game();
