-- Migration: 20261019000074_race_board_goals.sql
-- Description: The race board with goals that go on (docs/darood-weekly.md, The race board): as
-- before, each listed party's name, size, total, current goal, progress toward it and when it
-- finished it; and now which goal it's on (goal_number) and when each earlier goal was reached
-- (goals_reached: [{goal, target, reached_at}], oldest first). Still no one's name or numbers.
-- Re-runnable: CREATE OR REPLACE (columns only added at the end).
-- Rollback: re-run the view from 20261019000033_party_rules_for_every_kind.sql (DROP VIEW first).

CREATE OR REPLACE VIEW public.race_board
WITH (security_invoker = false)
AS
SELECT
    p.name,
    coalesce(m.member_count, 0) AS member_count,
    coalesce(e.total, 0) AS total,
    p.milestone,
    least(coalesce(e.total, 0)::NUMERIC / p.milestone, 1)::REAL AS progress,
    p.completed_at,
    EXISTS (
        SELECT 1 FROM public.party_members me
        WHERE me.party_id = p.id AND me.user_id = (SELECT auth.uid()) AND me.status = 'active'
    ) AS mine,
    coalesce(g.goal_number, 1) AS goal_number,
    coalesce(g.goals_reached, '[]'::JSONB) AS goals_reached
FROM public.parties p
LEFT JOIN (
    SELECT party_id, sum(amount)::BIGINT AS total
    FROM public.darood_entries
    GROUP BY party_id
) e ON e.party_id = p.id
LEFT JOIN (
    SELECT party_id, count(*)::INTEGER AS member_count
    FROM public.party_members
    WHERE status = 'active'
    GROUP BY party_id
) m ON m.party_id = p.id
LEFT JOIN (
    SELECT
        party_id,
        max(sequence) AS goal_number,
        jsonb_agg(jsonb_build_object('goal', sequence, 'target', target, 'reached_at', reached_at) ORDER BY sequence)
            FILTER (WHERE reached_at IS NOT NULL) AS goals_reached
    FROM public.party_milestones
    GROUP BY party_id
) g ON g.party_id = p.id
WHERE p.is_public_race
  AND p.party_type = 'darood'
  AND p.deleted_at IS NULL
  AND p.milestone IS NOT NULL
  AND (SELECT auth.uid()) IS NOT NULL;

REVOKE ALL ON public.race_board FROM public, anon;
REVOKE INSERT, UPDATE, DELETE, TRUNCATE, REFERENCES, TRIGGER ON public.race_board FROM authenticated;
GRANT SELECT ON public.race_board TO authenticated;
