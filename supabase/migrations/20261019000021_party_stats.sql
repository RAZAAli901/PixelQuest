-- Migration: 20261019000021_party_stats.sql
-- Description: A party's numbers (docs/darood-design.md, The total, contribution and consistency).
--   * party_stats, one row per party: its total, its active members, how many entries, the
--     milestone and whether it's finished, and the last day anyone posted.
--   * party_member_stats, one row per person who has posted in a party: their total, the days
--     they posted on (active days), and their last day. People who left or were removed keep their
--     row: their entries still count. An account since deleted shows as one row with no user.
--     username is filled in where the reader may see it (a co-member's).
-- Both are security_invoker views: whoever reads them gets only what the tables' own row-level
-- security lets them see, so a party's stats are for its active members alone, as its entries are.
-- Re-runnable: CREATE OR REPLACE VIEW.
-- Rollback: DROP VIEW IF EXISTS public.party_member_stats; DROP VIEW IF EXISTS public.party_stats;

CREATE OR REPLACE VIEW public.party_stats
WITH (security_invoker = true)
AS
SELECT
    p.id AS party_id,
    p.name,
    p.leader_id,
    p.max_members,
    p.milestone,
    p.milestone_set_at,
    p.completed_at,
    coalesce(e.total, 0) AS total,
    coalesce(e.entry_count, 0) AS entry_count,
    e.last_entry_date,
    coalesce(m.member_count, 0) AS member_count
FROM public.parties p
LEFT JOIN (
    SELECT party_id, sum(amount)::BIGINT AS total, count(*)::INTEGER AS entry_count, max(local_date) AS last_entry_date
    FROM public.darood_entries
    GROUP BY party_id
) e ON e.party_id = p.id
LEFT JOIN (
    SELECT party_id, count(*)::INTEGER AS member_count
    FROM public.party_members
    WHERE status = 'active'
    GROUP BY party_id
) m ON m.party_id = p.id;

CREATE OR REPLACE VIEW public.party_member_stats
WITH (security_invoker = true)
AS
SELECT
    e.party_id,
    e.user_id,
    u.username,
    pm.role,
    pm.status,
    sum(e.amount)::BIGINT AS total,
    count(DISTINCT e.local_date)::INTEGER AS active_days,
    max(e.local_date) AS last_entry_date
FROM public.darood_entries e
LEFT JOIN public.party_members pm ON pm.party_id = e.party_id AND pm.user_id = e.user_id
LEFT JOIN public.usernames u ON u.user_id = e.user_id
GROUP BY e.party_id, e.user_id, u.username, pm.role, pm.status;

REVOKE ALL ON public.party_stats, public.party_member_stats FROM anon;
REVOKE INSERT, UPDATE, DELETE, TRUNCATE, REFERENCES, TRIGGER ON public.party_stats, public.party_member_stats FROM authenticated;
GRANT SELECT ON public.party_stats, public.party_member_stats TO authenticated;
