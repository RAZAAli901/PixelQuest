-- Migration: 20261019000105_chess_removed_players.sql
-- Description: A player removed from the party (or who left it) loses the game at once
-- (docs/chess-design.md, Threats; found by Phase F's security tests): chess_game_state, which
-- every move, timeout claim and the app's own reads go through, answers only active members.
-- Their clock keeps running, so the game still ends, on time or by the sweep.
--   * Refusing what isn't the caller's to see ("not_found") is a client error (400), not a server
--     one: chess_respond_challenge and chess_watch raised it with no_data_found, which PostgREST
--     answers as a 500, so the functions said server_error.
-- Re-runnable: CREATE OR REPLACE.
-- Rollback: re-run chess_game_state (085), chess_watch (095) and chess_respond_challenge (096).

CREATE OR REPLACE FUNCTION public.chess_game_state(p_user UUID, p_game UUID)
RETURNS JSONB
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    game public.chess_games%ROWTYPE;
BEGIN
    SELECT * INTO game FROM public.chess_games WHERE id = p_game;
    IF NOT FOUND OR p_user NOT IN (game.white_id, game.black_id) OR NOT private.chess_is_study_member(game.party_id, p_user) THEN
        RAISE EXCEPTION 'not_a_player' USING ERRCODE = 'insufficient_privilege';
    END IF;
    RETURN jsonb_build_object(
        'game', to_jsonb(game),
        'moves', coalesce((SELECT jsonb_agg(m.uci ORDER BY m.ply) FROM public.chess_moves m WHERE m.game_id = p_game), '[]'::JSONB),
        'now', now()
    );
END;
$$;

REVOKE ALL ON FUNCTION public.chess_game_state(UUID, UUID) FROM public, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.chess_game_state(UUID, UUID) TO service_role;

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
        RAISE EXCEPTION 'not_found' USING ERRCODE = 'invalid_parameter_value';
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
        RAISE EXCEPTION 'not_found' USING ERRCODE = 'invalid_parameter_value';
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
