-- Migration: 20261019000094_chess_live_games.sql
-- Description: A study party's games now being played (docs/chess-design.md), for
-- chess-live-games: for its active members only (not_a_member), newest first, with how far each
-- has got. The players are user ids; the phone names them from the party's members.
-- Re-runnable: CREATE OR REPLACE.
-- Rollback: DROP FUNCTION public.chess_live_games(UUID, UUID).

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
        SELECT jsonb_agg(to_jsonb(g) ORDER BY g.created_at DESC)
        FROM public.chess_games g
        WHERE g.party_id = p_party AND g.status IN ('waiting', 'active')
    ), '[]'::JSONB);
END;
$$;

REVOKE ALL ON FUNCTION public.chess_live_games(UUID, UUID) FROM public, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.chess_live_games(UUID, UUID) TO service_role;
