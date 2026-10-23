-- Migration: 20261019000103_chess_live_watchers.sql
-- Description: chess_live_games says how many are watching each game (docs/chess-ui-integration.md,
-- live game cards): the members who asked to watch it in the last 30 minutes (chess_watch_log).
-- A count only, never who.
-- Re-runnable: CREATE OR REPLACE.
-- Rollback: re-run chess_live_games from 20261019000094.

CREATE OR REPLACE FUNCTION public.chess_live_games(p_user UUID, p_party UUID)
RETURNS JSONB
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    IF NOT private.chess_is_study_member(p_party, p_user) THEN
        RAISE EXCEPTION 'not_a_member' USING ERRCODE = 'insufficient_privilege';
    END IF;
    RETURN coalesce((
        SELECT jsonb_agg(
            to_jsonb(g) || jsonb_build_object('watchers', (
                SELECT count(DISTINCT w.user_id) FROM public.chess_watch_log w
                WHERE w.game_id = g.id AND w.joined_at > now() - INTERVAL '30 minutes'
                  AND w.user_id <> g.white_id AND w.user_id <> g.black_id
            ))
            ORDER BY g.created_at DESC
        )
        FROM public.chess_games g
        WHERE g.party_id = p_party AND g.status IN ('waiting', 'active')
    ), '[]'::JSONB);
END;
$$;

REVOKE ALL ON FUNCTION public.chess_live_games(UUID, UUID) FROM public, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.chess_live_games(UUID, UUID) TO service_role;
