-- Migration: 20261019000087_chess_draws.sql
-- Description: Draw offers (docs/chess-design.md), for the service role only:
--   * chess_draw(user, game, action): in a live game, a player offers a draw ('offer'), or answers
--     the other's ('accept', 'decline'). One offer stands at a time: offering again changes
--     nothing, and offering when the other has already offered agrees the draw. An answer needs
--     the other side's offer (no_draw_offer). A move clears the other side's offer
--     (chess_commit_move).
-- Re-runnable: CREATE OR REPLACE.
-- Rollback: DROP FUNCTION public.chess_draw(UUID, UUID, TEXT).

CREATE OR REPLACE FUNCTION public.chess_draw(p_user UUID, p_game UUID, p_action TEXT)
RETURNS JSONB
LANGUAGE plpgsql
VOLATILE
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    game public.chess_games%ROWTYPE;
    offered_by_other BOOLEAN;
BEGIN
    SELECT * INTO game FROM public.chess_games WHERE id = p_game FOR UPDATE;
    IF NOT FOUND OR p_user NOT IN (game.white_id, game.black_id) THEN
        RAISE EXCEPTION 'not_a_player' USING ERRCODE = 'insufficient_privilege';
    END IF;
    IF game.status NOT IN ('waiting', 'active') THEN
        RAISE EXCEPTION 'game_over' USING ERRCODE = 'check_violation';
    END IF;
    offered_by_other := game.draw_offered_by IS NOT NULL AND game.draw_offered_by <> p_user;
    CASE p_action
        WHEN 'offer' THEN
            IF offered_by_other THEN
                game := private.chess_finish(p_game, 'draw', 'draw_agreed');
            ELSIF game.draw_offered_by IS NULL THEN
                UPDATE public.chess_games SET draw_offered_by = p_user WHERE id = p_game RETURNING * INTO game;
            END IF;
        WHEN 'accept' THEN
            IF NOT offered_by_other THEN
                RAISE EXCEPTION 'no_draw_offer' USING ERRCODE = 'check_violation';
            END IF;
            game := private.chess_finish(p_game, 'draw', 'draw_agreed');
        WHEN 'decline' THEN
            IF NOT offered_by_other THEN
                RAISE EXCEPTION 'no_draw_offer' USING ERRCODE = 'check_violation';
            END IF;
            UPDATE public.chess_games SET draw_offered_by = NULL WHERE id = p_game RETURNING * INTO game;
        ELSE
            RAISE EXCEPTION 'bad_request' USING ERRCODE = 'invalid_parameter_value';
    END CASE;
    RETURN jsonb_build_object('game', to_jsonb(game));
END;
$$;

REVOKE ALL ON FUNCTION public.chess_draw(UUID, UUID, TEXT) FROM public, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.chess_draw(UUID, UUID, TEXT) TO service_role;
