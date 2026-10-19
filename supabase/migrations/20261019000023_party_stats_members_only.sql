-- Migration: 20261019000023_party_stats_members_only.sql
-- Description: party_stats is for a party's active members only.
--   * The view starts from parties, which someone invited may read (to decide), and their hidden
--     entries simply added up to nothing: an invitation showed a party with a total of 0. Now a row
--     appears only to the party's active members, like its entries and party_member_stats.
-- Re-runnable: CREATE OR REPLACE VIEW (same columns).
-- Rollback: re-run 20261019000021_party_stats.sql.

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
) m ON m.party_id = p.id
WHERE private.is_active_member(p.id);
