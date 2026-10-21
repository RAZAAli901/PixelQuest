-- Migration: 20261019000081_chess_read_policies.sql
-- Description: Who reads chess (docs/chess-design.md, Threats): a study party's active members
-- read its games, their moves and its challenges; nobody else does. No client inserts, updates or
-- deletes any of them: only the chess functions (service role) write.
--   * chess_config and chess_time_controls hold no one's data: anyone signed in reads them.
-- Re-runnable: DROP POLICY IF EXISTS.
-- Rollback: drop the policies and the grants.

GRANT SELECT ON public.chess_config, public.chess_time_controls TO authenticated;
DROP POLICY IF EXISTS "Anyone signed in reads the chess rules" ON public.chess_config;
CREATE POLICY "Anyone signed in reads the chess rules" ON public.chess_config FOR SELECT TO authenticated USING (true);
DROP POLICY IF EXISTS "Anyone signed in reads the time controls" ON public.chess_time_controls;
CREATE POLICY "Anyone signed in reads the time controls" ON public.chess_time_controls FOR SELECT TO authenticated USING (true);

GRANT SELECT ON public.chess_games, public.chess_moves, public.chess_challenges TO authenticated;

DROP POLICY IF EXISTS "Members read their party's games" ON public.chess_games;
CREATE POLICY "Members read their party's games" ON public.chess_games FOR SELECT TO authenticated
    USING (private.is_active_study_member(party_id));

DROP POLICY IF EXISTS "Members read their party's moves" ON public.chess_moves;
CREATE POLICY "Members read their party's moves" ON public.chess_moves FOR SELECT TO authenticated
    USING (EXISTS (SELECT 1 FROM public.chess_games g WHERE g.id = chess_moves.game_id AND private.is_active_study_member(g.party_id)));

DROP POLICY IF EXISTS "Members read their party's challenges" ON public.chess_challenges;
CREATE POLICY "Members read their party's challenges" ON public.chess_challenges FOR SELECT TO authenticated
    USING (private.is_active_study_member(party_id));
