-- Migration: 20261019000084_chess_respond_challenge.sql
-- Description: The database side of chess-respond-challenge (docs/chess-design.md), for the
-- service role only:
--   * the one challenged, or for an open challenge any other active member, accepts or declines;
--     the challenger's own "no" cancels it;
--   * an answered challenge stays answered (already_answered); a late one expires (expired);
--   * accepting makes the game: the colours as asked (random by the server), both clocks at the
--     time control, waiting for White's first move (the abort timer runs from now). Neither player
--     may have a live game; their other open challenges are cancelled.
--   * Someone outside the party is told not_found, so it says nothing about the challenge.
-- Re-runnable: CREATE OR REPLACE.
-- Rollback: DROP FUNCTION public.chess_respond_challenge(UUID, UUID, BOOLEAN).

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
