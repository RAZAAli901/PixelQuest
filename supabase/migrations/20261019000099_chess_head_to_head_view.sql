-- Migration: 20261019000099_chess_head_to_head_view.sql
-- Description: Any two players' record (docs/chess-design.md, Head-to-head): per time control,
-- and in total (time_control_sec null), with the games played. It reads chess_head_to_head under
-- the caller's own RLS (security_invoker): only pairs they share a study party with.
-- Re-runnable: CREATE OR REPLACE VIEW.
-- Rollback: DROP VIEW public.chess_head_to_head_totals.

CREATE OR REPLACE VIEW public.chess_head_to_head_totals
WITH (security_invoker = true)
AS
SELECT user_low, user_high, time_control_sec, low_wins, high_wins, draws, low_wins + high_wins + draws AS games
FROM public.chess_head_to_head
UNION ALL
SELECT user_low, user_high, NULL::INTEGER, sum(low_wins)::INTEGER, sum(high_wins)::INTEGER, sum(draws)::INTEGER,
       sum(low_wins + high_wins + draws)::INTEGER
FROM public.chess_head_to_head
GROUP BY user_low, user_high;

REVOKE ALL ON public.chess_head_to_head_totals FROM public, anon;
GRANT SELECT ON public.chess_head_to_head_totals TO authenticated;
