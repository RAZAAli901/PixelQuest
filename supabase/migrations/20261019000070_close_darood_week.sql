-- Migration: 20261019000070_close_darood_week.sql
-- Description: Closing a Darood Party's week (docs/darood-weekly.md, Results).
--   * close_darood_week(party_id, week_start): any active member may ask; a Monday whose week has
--     ended in the party's own zone (week_not_over otherwise). Its results are written once
--     (private.week_ranking), and asking again changes nothing. Returns how many rows it holds.
--   * close_due_darood_weeks(): for the server's schedule (service role): last week, closed for
--     every Darood Party whose week has ended in its zone.
--   * Schedule: pg_cron, hourly, when the extension is there (hosted needs it turned on);
--     otherwise the phones' asks close it.
-- Re-runnable: CREATE OR REPLACE.
-- Rollback: DROP FUNCTION public.close_darood_week(UUID, DATE), public.close_due_darood_weeks().

CREATE OR REPLACE FUNCTION private.close_week_now(party UUID, monday DATE)
RETURNS INTEGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    zone TEXT;
    held INTEGER;
BEGIN
    SELECT week_tz INTO zone FROM public.parties WHERE id = party AND party_type = 'darood' AND deleted_at IS NULL;
    IF zone IS NULL THEN
        RAISE EXCEPTION 'no_such_party' USING ERRCODE = 'no_data_found';
    END IF;
    IF (now() AT TIME ZONE zone)::DATE <= monday + 6 THEN
        RAISE EXCEPTION 'week_not_over' USING ERRCODE = 'check_violation';
    END IF;
    PERFORM pg_advisory_xact_lock(hashtext('close_darood_week:' || party::TEXT || monday::TEXT));
    IF NOT EXISTS (SELECT 1 FROM public.darood_week_results WHERE party_id = party AND week_start = monday) THEN
        INSERT INTO public.darood_week_results (party_id, week_start, user_id, position, week_total, active_days, members)
        SELECT party, monday, r.user_id, r."position", r.week_total, r.active_days, count(*) OVER ()
        FROM private.week_ranking(party, monday) r;
    END IF;
    SELECT count(*) INTO held FROM public.darood_week_results WHERE party_id = party AND week_start = monday;
    RETURN held;
END;
$$;

CREATE OR REPLACE FUNCTION public.close_darood_week(party_id UUID, week_start DATE)
RETURNS INTEGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
#variable_conflict use_column
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM public.party_members m
        WHERE m.party_id = close_darood_week.party_id AND m.user_id = (SELECT auth.uid()) AND m.status = 'active'
    ) THEN
        RAISE EXCEPTION 'not_a_member' USING ERRCODE = 'insufficient_privilege';
    END IF;
    IF EXTRACT(ISODOW FROM close_darood_week.week_start) <> 1 THEN
        RAISE EXCEPTION 'bad_week' USING ERRCODE = 'invalid_parameter_value';
    END IF;
    RETURN private.close_week_now(close_darood_week.party_id, close_darood_week.week_start);
END;
$$;

CREATE OR REPLACE FUNCTION public.close_due_darood_weeks()
RETURNS INTEGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    p RECORD;
    closed INTEGER := 0;
    monday DATE;
BEGIN
    FOR p IN SELECT id, week_tz FROM public.parties WHERE party_type = 'darood' AND deleted_at IS NULL LOOP
        monday := date_trunc('week', (now() AT TIME ZONE p.week_tz)::DATE)::DATE - 7;
        IF NOT EXISTS (SELECT 1 FROM public.darood_week_results WHERE party_id = p.id AND week_start = monday) THEN
            PERFORM private.close_week_now(p.id, monday);
            closed := closed + 1;
        END IF;
    END LOOP;
    RETURN closed;
END;
$$;

REVOKE ALL ON FUNCTION private.close_week_now(UUID, DATE) FROM public, anon, authenticated;
REVOKE ALL ON FUNCTION public.close_darood_week(UUID, DATE) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.close_darood_week(UUID, DATE) TO authenticated;
REVOKE ALL ON FUNCTION public.close_due_darood_weeks() FROM public, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.close_due_darood_weeks() TO service_role;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_cron') THEN
        PERFORM cron.schedule('close-darood-weeks', '5 * * * *', 'SELECT public.close_due_darood_weeks()');
    END IF;
END;
$$;
