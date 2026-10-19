-- Migration: 20261019000029_party_day_stats.sql
-- Description: A party's numbers for one day, for the chat's "You today | Party today" strip
--   (docs/darood-design.md, The total, contribution and consistency).
--   * party_day_stats: one row per party, day and poster, with that poster's total and entry count
--     for the day. The day is each entry's own local_date, the poster's day where they were.
--   * A security_invoker view, like party_stats: whoever reads it gets only what darood_entries'
--     row-level security lets them see, so a party's days are for its active members alone, and a
--     deleted party's are gone with it.
-- Re-runnable: CREATE OR REPLACE VIEW.
-- Rollback: DROP VIEW IF EXISTS public.party_day_stats;

CREATE OR REPLACE VIEW public.party_day_stats
WITH (security_invoker = true)
AS
SELECT
    e.party_id,
    e.local_date,
    e.user_id,
    sum(e.amount)::BIGINT AS total,
    count(*)::INTEGER AS entry_count
FROM public.darood_entries e
GROUP BY e.party_id, e.local_date, e.user_id;

REVOKE ALL ON public.party_day_stats FROM anon;
REVOKE INSERT, UPDATE, DELETE, TRUNCATE, REFERENCES, TRIGGER ON public.party_day_stats FROM authenticated;
GRANT SELECT ON public.party_day_stats TO authenticated;
