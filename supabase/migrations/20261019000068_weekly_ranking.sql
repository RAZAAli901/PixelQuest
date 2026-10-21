-- Migration: 20261019000068_weekly_ranking.sql
-- Description: A Darood Party's ranking for one week (docs/darood-weekly.md, Positions).
--   * weekly_ranking(party_id, week_start): week_start is that week's Monday in the party's zone
--     (bad_week otherwise). Each member active in the party now, with their total for the week
--     (entries whose local_date falls Monday to Sunday), the days they added on, and their
--     position: highest first, ties sharing one (1, 2, 2, 4); 0 entries ranks last with 0.
--   * Read by the party's active members only (not_a_member).
-- Re-runnable: CREATE OR REPLACE.
-- Rollback: DROP FUNCTION public.weekly_ranking(UUID, DATE).

CREATE OR REPLACE FUNCTION public.weekly_ranking(party_id UUID, week_start DATE)
RETURNS TABLE (user_id UUID, "position" INTEGER, week_total BIGINT, active_days INTEGER)
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
#variable_conflict use_column
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM public.party_members m JOIN public.parties p ON p.id = m.party_id
        WHERE m.party_id = weekly_ranking.party_id AND m.user_id = (SELECT auth.uid()) AND m.status = 'active'
          AND p.party_type = 'darood' AND p.deleted_at IS NULL
    ) THEN
        RAISE EXCEPTION 'not_a_member' USING ERRCODE = 'insufficient_privilege';
    END IF;
    IF EXTRACT(ISODOW FROM weekly_ranking.week_start) <> 1 THEN
        RAISE EXCEPTION 'bad_week' USING ERRCODE = 'invalid_parameter_value';
    END IF;
    RETURN QUERY SELECT * FROM private.week_ranking(weekly_ranking.party_id, weekly_ranking.week_start);
END;
$$;

-- The ranking itself, with no caller check: for weekly_ranking, and for closing a week (069 on).
CREATE OR REPLACE FUNCTION private.week_ranking(party UUID, monday DATE)
RETURNS TABLE (user_id UUID, "position" INTEGER, week_total BIGINT, active_days INTEGER)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
    WITH totals AS (
        SELECT m.user_id,
               coalesce(sum(e.amount), 0)::BIGINT AS week_total,
               count(DISTINCT e.local_date)::INTEGER AS active_days
        FROM public.party_members m
        LEFT JOIN public.darood_entries e
               ON e.party_id = m.party_id AND e.user_id = m.user_id
              AND e.local_date BETWEEN monday AND monday + 6
        WHERE m.party_id = party AND m.status = 'active'
        GROUP BY m.user_id
    )
    SELECT t.user_id, (rank() OVER (ORDER BY t.week_total DESC))::INTEGER, t.week_total, t.active_days
    FROM totals t
    ORDER BY 2, t.active_days DESC;
$$;

REVOKE ALL ON FUNCTION private.week_ranking(UUID, DATE) FROM public, anon, authenticated;
REVOKE ALL ON FUNCTION public.weekly_ranking(UUID, DATE) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.weekly_ranking(UUID, DATE) TO authenticated;
