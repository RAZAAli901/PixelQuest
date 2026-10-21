-- Migration: 20261019000093_chess_spectators.sql
-- Description: Watching chess live (docs/chess-design.md, Realtime): one private channel per
-- game, "chess:game:<id>".
--   * Every change to a game (a move, the clocks, an offer, the end) is sent on it by the
--     database, with the move that made it, so players and watchers see the same thing at once.
--   * Channel authorisation (RLS on realtime.messages): only active members of the game's study
--     party may join, hear it, or share their presence on it. Presence gives the watcher count.
--   * chess_games and chess_moves also join the supabase_realtime publication, under their own
--     RLS, for a client that follows the tables instead.
-- Re-runnable: CREATE OR REPLACE, DROP ... IF EXISTS, tables added only once.
-- Rollback: drop the trigger, private.chess_announce(), the two policies and the publication entries.

CREATE OR REPLACE FUNCTION private.chess_game_of_topic(topic TEXT)
RETURNS UUID
LANGUAGE sql
IMMUTABLE
SET search_path = ''
AS $$
    SELECT CASE WHEN topic ~ '^chess:game:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
        THEN substr(topic, 12)::UUID END;
$$;

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
    );
$$;
REVOKE ALL ON FUNCTION private.chess_game_of_topic(TEXT), private.chess_may_watch(TEXT) FROM public, anon;
GRANT EXECUTE ON FUNCTION private.chess_game_of_topic(TEXT), private.chess_may_watch(TEXT) TO authenticated;

DROP POLICY IF EXISTS "Party members hear a chess game" ON realtime.messages;
CREATE POLICY "Party members hear a chess game" ON realtime.messages FOR SELECT TO authenticated
    USING (realtime.topic() LIKE 'chess:game:%' AND private.chess_may_watch(realtime.topic()));
DROP POLICY IF EXISTS "Party members are seen watching a chess game" ON realtime.messages;
CREATE POLICY "Party members are seen watching a chess game" ON realtime.messages FOR INSERT TO authenticated
    WITH CHECK (realtime.topic() LIKE 'chess:game:%' AND extension = 'presence' AND private.chess_may_watch(realtime.topic()));

CREATE OR REPLACE FUNCTION private.chess_announce()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    PERFORM realtime.send(
        jsonb_build_object(
            'game', to_jsonb(NEW),
            'move', (SELECT to_jsonb(m) FROM public.chess_moves m WHERE m.game_id = NEW.id AND m.ply = NEW.ply AND NEW.ply > OLD.ply)
        ),
        'game', 'chess:game:' || NEW.id::TEXT, TRUE
    );
    RETURN NULL;
END;
$$;
REVOKE ALL ON FUNCTION private.chess_announce() FROM public, anon, authenticated;

DROP TRIGGER IF EXISTS chess_announce ON public.chess_games;
CREATE TRIGGER chess_announce
    AFTER UPDATE ON public.chess_games
    FOR EACH ROW
    EXECUTE FUNCTION private.chess_announce();

DO $$
DECLARE
    t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['chess_games', 'chess_moves', 'chess_challenges'] LOOP
        IF NOT EXISTS (
            SELECT 1 FROM pg_publication_tables
            WHERE pubname = 'supabase_realtime' AND schemaname = 'public' AND tablename = t
        ) THEN
            EXECUTE format('ALTER PUBLICATION supabase_realtime ADD TABLE public.%I', t);
        END IF;
    END LOOP;
END;
$$;
