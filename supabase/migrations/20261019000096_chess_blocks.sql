-- Migration: 20261019000096_chess_blocks.sql
-- Description: The block list in chess (docs/chess-design.md, Threats): two members either of
-- whom blocked the other (member_blocks, 20261019000066) can't challenge each other, and neither
-- can take the other's open challenge (blocked). Nothing says which of them blocked.
-- Re-runnable: CREATE OR REPLACE.
-- Rollback: re-run chess_create_challenge (083) and chess_respond_challenge (084); DROP FUNCTION private.chess_blocked(UUID, UUID).

CREATE OR REPLACE FUNCTION private.chess_blocked(a UUID, b UUID)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.member_blocks m
        WHERE (m.blocker_id = a AND m.blocked_id = b) OR (m.blocker_id = b AND m.blocked_id = a)
    );
$$;
REVOKE ALL ON FUNCTION private.chess_blocked(UUID, UUID) FROM public, anon, authenticated;

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
    IF p_to IS NOT NULL AND private.chess_blocked(p_user, p_to) THEN
        RAISE EXCEPTION 'blocked' USING ERRCODE = 'insufficient_privilege';
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

CREATE OR REPLACE FUNCTION public.chess_respond_challenge(p_user UUID, p_challenge UUID, p_accept BOOLEAN)
RETURNS JSONB
LANGUAGE plpgsql
VOLATILE
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    c public.chess_challenges%ROWTYPE;
    game public.chess_games%ROWTYPE;
    opponent UUID;
    challenger_white BOOLEAN;
BEGIN
    SELECT * INTO c FROM public.chess_challenges WHERE id = p_challenge FOR UPDATE;
    IF NOT FOUND OR NOT private.chess_is_study_member(c.party_id, p_user) THEN
        RAISE EXCEPTION 'not_found' USING ERRCODE = 'no_data_found';
    END IF;
    IF c.status <> 'open' THEN
        RAISE EXCEPTION 'already_answered' USING ERRCODE = 'check_violation';
    END IF;
    IF c.expires_at <= now() THEN
        UPDATE public.chess_challenges SET status = 'expired', answered_at = now() WHERE id = c.id;
        -- Kept: the expiry stands even though the answer is a refusal.
        RETURN jsonb_build_object('error', 'expired');
    END IF;

    IF p_user = c.from_user THEN
        IF p_accept THEN
            RAISE EXCEPTION 'not_a_player' USING ERRCODE = 'insufficient_privilege';
        END IF;
        UPDATE public.chess_challenges SET status = 'cancelled', answered_at = now() WHERE id = c.id RETURNING * INTO c;
        RETURN jsonb_build_object('challenge', to_jsonb(c));
    END IF;
    IF c.to_user IS NOT NULL AND c.to_user <> p_user THEN
        RAISE EXCEPTION 'not_a_player' USING ERRCODE = 'insufficient_privilege';
    END IF;
    IF NOT p_accept THEN
        IF c.to_user IS NULL THEN
            -- An open challenge isn't one member's to decline: it stays open for the others.
            RETURN jsonb_build_object('challenge', to_jsonb(c));
        END IF;
        UPDATE public.chess_challenges SET status = 'declined', answered_at = now() WHERE id = c.id RETURNING * INTO c;
        RETURN jsonb_build_object('challenge', to_jsonb(c));
    END IF;

    opponent := p_user;
    IF private.chess_blocked(c.from_user, opponent) THEN
        RAISE EXCEPTION 'blocked' USING ERRCODE = 'insufficient_privilege';
    END IF;
    IF NOT private.chess_is_study_member(c.party_id, c.from_user) THEN
        RAISE EXCEPTION 'not_a_member' USING ERRCODE = 'insufficient_privilege';
    END IF;
    IF private.chess_has_live_game(c.from_user) OR private.chess_has_live_game(opponent) THEN
        RAISE EXCEPTION 'already_playing' USING ERRCODE = 'check_violation';
    END IF;
    challenger_white := CASE c.colour WHEN 'white' THEN TRUE WHEN 'black' THEN FALSE ELSE random() < 0.5 END;
    INSERT INTO public.chess_games (party_id, white_id, black_id, time_control_sec, rated, white_ms, black_ms, last_move_at)
    VALUES (
        c.party_id,
        CASE WHEN challenger_white THEN c.from_user ELSE opponent END,
        CASE WHEN challenger_white THEN opponent ELSE c.from_user END,
        c.time_control_sec, c.rated, c.time_control_sec * 1000, c.time_control_sec * 1000, now()
    )
    RETURNING * INTO game;
    UPDATE public.chess_challenges SET status = 'accepted', answered_at = now(), to_user = opponent, game_id = game.id
    WHERE id = c.id RETURNING * INTO c;
    -- Playing now: their other open challenges are off.
    UPDATE public.chess_challenges SET status = 'cancelled', answered_at = now()
    WHERE status = 'open' AND id <> c.id AND from_user IN (c.from_user, opponent);
    RETURN jsonb_build_object('challenge', to_jsonb(c), 'game', to_jsonb(game));
END;
$$;

REVOKE ALL ON FUNCTION public.chess_respond_challenge(UUID, UUID, BOOLEAN) FROM public, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.chess_respond_challenge(UUID, UUID, BOOLEAN) TO service_role;
