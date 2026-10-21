-- Migration: 20261019000101_chess_profile_stats.sql
-- Description: A player's chess numbers for their profile (docs/chess-design.md): per time
-- control, their games, wins, draws, losses, Int points, peak and tier (from chess_config's
-- tiers). Read under the caller's own RLS (security_invoker): their own, and party mates'.
-- Re-runnable: CREATE OR REPLACE VIEW.
-- Rollback: DROP VIEW public.chess_profile_stats.

CREATE OR REPLACE VIEW public.chess_profile_stats
WITH (security_invoker = true)
AS
SELECT r.user_id, r.time_control_sec, r.games, r.wins, r.draws, r.losses, r.int_points, r.peak,
       (SELECT t ->> 'name' FROM jsonb_array_elements(c.tiers) t
        WHERE (t ->> 'from')::INTEGER <= r.int_points
        ORDER BY (t ->> 'from')::INTEGER DESC LIMIT 1) AS tier
FROM public.chess_ratings r
CROSS JOIN public.chess_config c;

REVOKE ALL ON public.chess_profile_stats FROM public, anon;
GRANT SELECT ON public.chess_profile_stats TO authenticated;
