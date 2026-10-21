-- Migration: 20261019000100_chess_party_leaderboard.sql
-- Description: A study party's chess leaderboard (docs/chess-design.md): each active member's
-- Int points per time control, and their best across them (time_control_sec null), ordered by
-- points (place, ties sharing it). A member without games is at the start rating. Read under the
-- caller's own RLS (security_invoker): their own parties only.
-- Re-runnable: CREATE OR REPLACE VIEW.
-- Rollback: DROP VIEW public.chess_party_leaderboard.

CREATE OR REPLACE VIEW public.chess_party_leaderboard
WITH (security_invoker = true)
AS
WITH board AS (
    SELECT m.party_id, m.user_id, t.seconds AS time_control_sec,
           coalesce(r.int_points, c.start_rating) AS int_points,
           coalesce(r.games, 0) AS games, coalesce(r.wins, 0) AS wins, coalesce(r.draws, 0) AS draws, coalesce(r.losses, 0) AS losses
    FROM public.party_members m
    JOIN public.parties p ON p.id = m.party_id AND p.party_type = 'study' AND p.deleted_at IS NULL
    CROSS JOIN public.chess_time_controls t
    CROSS JOIN public.chess_config c
    LEFT JOIN public.chess_ratings r ON r.user_id = m.user_id AND r.time_control_sec = t.seconds
    WHERE m.status = 'active'
),
best AS (
    SELECT DISTINCT ON (party_id, user_id) party_id, user_id, NULL::INTEGER AS time_control_sec, int_points,
           (SELECT sum(b2.games) FROM board b2 WHERE b2.party_id = b.party_id AND b2.user_id = b.user_id)::INTEGER AS games,
           (SELECT sum(b2.wins) FROM board b2 WHERE b2.party_id = b.party_id AND b2.user_id = b.user_id)::INTEGER AS wins,
           (SELECT sum(b2.draws) FROM board b2 WHERE b2.party_id = b.party_id AND b2.user_id = b.user_id)::INTEGER AS draws,
           (SELECT sum(b2.losses) FROM board b2 WHERE b2.party_id = b.party_id AND b2.user_id = b.user_id)::INTEGER AS losses
    FROM board b
    ORDER BY party_id, user_id, int_points DESC
),
everything AS (
    SELECT * FROM board
    UNION ALL
    SELECT * FROM best
)
SELECT party_id, user_id, time_control_sec, int_points, games, wins, draws, losses,
       rank() OVER (PARTITION BY party_id, time_control_sec ORDER BY int_points DESC)::INTEGER AS place
FROM everything;

REVOKE ALL ON public.chess_party_leaderboard FROM public, anon;
GRANT SELECT ON public.chess_party_leaderboard TO authenticated;
