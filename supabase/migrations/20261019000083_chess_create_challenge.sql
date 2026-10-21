-- Migration: 20261019000083_chess_create_challenge.sql
-- Description: The database side of chess-create-challenge (docs/chess-design.md), for the
-- service role only (the function checks the caller's sign-in and passes their id):
--   * the challenger and the one challenged (or nobody, for an open challenge) are active members
--     of the same study party (not_a_member);
--   * the challenger has no live game (already_playing);
--   * a time control and colour from the lists (bad_time_control, bad_colour);
--   * at most chess_config.challenges_per_hour challenges an hour each (rate_limited);
--   * it expires after chess_config.challenge_minutes.
-- Re-runnable: CREATE OR REPLACE.
-- Rollback: DROP FUNCTION public.chess_create_challenge(UUID, UUID, UUID, INTEGER, TEXT, BOOLEAN), private.chess_is_study_member(UUID, UUID).

CREATE OR REPLACE FUNCTION private.chess_is_study_member(party UUID, who UUID)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.party_members m JOIN public.parties p ON p.id = m.party_id
        WHERE m.party_id = party AND m.user_id = who AND m.status = 'active'
          AND p.party_type = 'study' AND p.deleted_at IS NULL
    );
$$;
REVOKE ALL ON FUNCTION private.chess_is_study_member(UUID, UUID) FROM public, anon, authenticated;

CREATE OR REPLACE FUNCTION private.chess_has_live_game(who UUID)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.chess_games g
        WHERE g.status IN ('waiting', 'active') AND (g.white_id = who OR g.black_id = who)
    );
$$;
REVOKE ALL ON FUNCTION private.chess_has_live_game(UUID) FROM public, anon, authenticated;

CREATE OR REPLACE FUNCTION public.chess_create_challenge(
    p_user UUID, p_party UUID, p_to UUID, p_time_control INTEGER, p_colour TEXT, p_rated BOOLEAN
)
RETURNS JSONB
LANGUAGE plpgsql
VOLATILE
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    config public.chess_config%ROWTYPE;
    made public.chess_challenges%ROWTYPE;
BEGIN
    SELECT * INTO config FROM public.chess_config;
    IF NOT private.chess_is_study_member(p_party, p_user) THEN
        RAISE EXCEPTION 'not_a_member' USING ERRCODE = 'insufficient_privilege';
    END IF;
    IF p_to IS NOT NULL AND (p_to = p_user OR NOT private.chess_is_study_member(p_party, p_to)) THEN
        RAISE EXCEPTION 'not_a_member' USING ERRCODE = 'insufficient_privilege';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM public.chess_time_controls t WHERE t.seconds = p_time_control) THEN
        RAISE EXCEPTION 'bad_time_control' USING ERRCODE = 'invalid_parameter_value';
    END IF;
    IF coalesce(p_colour, 'random') NOT IN ('white', 'black', 'random') THEN
        RAISE EXCEPTION 'bad_colour' USING ERRCODE = 'invalid_parameter_value';
    END IF;
    PERFORM pg_advisory_xact_lock(hashtext('chess_challenge:' || p_user::TEXT));
    IF private.chess_has_live_game(p_user) THEN
        RAISE EXCEPTION 'already_playing' USING ERRCODE = 'check_violation';
    END IF;
    IF (SELECT count(*) FROM public.chess_challenges c WHERE c.from_user = p_user AND c.created_at > now() - INTERVAL '1 hour')
        >= config.challenges_per_hour THEN
        RAISE EXCEPTION 'rate_limited' USING ERRCODE = 'check_violation';
    END IF;
    INSERT INTO public.chess_challenges (party_id, from_user, to_user, time_control_sec, colour, rated, expires_at)
    VALUES (p_party, p_user, p_to, p_time_control, coalesce(p_colour, 'random'), coalesce(p_rated, TRUE),
            now() + make_interval(mins => config.challenge_minutes))
    RETURNING * INTO made;
    RETURN to_jsonb(made);
END;
$$;

REVOKE ALL ON FUNCTION public.chess_create_challenge(UUID, UUID, UUID, INTEGER, TEXT, BOOLEAN) FROM public, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.chess_create_challenge(UUID, UUID, UUID, INTEGER, TEXT, BOOLEAN) TO service_role;
