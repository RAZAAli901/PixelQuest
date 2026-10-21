-- Migration: 20261019000104_chess_chat_events.sql
-- Description: Chess's other lines in the study chat (docs/chess-ui-integration.md, J156), each
-- pointing at what it's about (ref_id, 20261019000102):
--   * "chess_challenge_sent": a challenge to one member (the challenge's id);
--   * "chess_game_started": a game made from a challenge (the game's id);
--   * "chess_game_result": a game that finished (the game's id). An aborted game posts nothing.
-- Re-runnable: CREATE OR REPLACE, DROP TRIGGER IF EXISTS.
-- Rollback: drop the triggers and their functions; re-run chess_open_challenge_line from 102.

CREATE OR REPLACE FUNCTION private.chess_open_challenge_line()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    PERFORM private.chess_chat_line(NEW.party_id, CASE WHEN NEW.to_user IS NULL THEN 'chess_open_challenge' ELSE 'chess_challenge_sent' END, NEW.id);
    RETURN NULL;
END;
$$;
REVOKE ALL ON FUNCTION private.chess_open_challenge_line() FROM public, anon, authenticated;

CREATE OR REPLACE FUNCTION private.chess_game_lines()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        PERFORM private.chess_chat_line(NEW.party_id, 'chess_game_started', NEW.id);
    ELSIF NEW.status = 'finished' AND OLD.status IS DISTINCT FROM 'finished' THEN
        PERFORM private.chess_chat_line(NEW.party_id, 'chess_game_result', NEW.id);
    END IF;
    RETURN NULL;
END;
$$;
REVOKE ALL ON FUNCTION private.chess_game_lines() FROM public, anon, authenticated;

DROP TRIGGER IF EXISTS chess_game_lines ON public.chess_games;
CREATE TRIGGER chess_game_lines
    AFTER INSERT OR UPDATE OF status ON public.chess_games
    FOR EACH ROW
    EXECUTE FUNCTION private.chess_game_lines();
