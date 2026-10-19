-- Migration: 20261019000012_entry_daily_cap.sql
-- Description: At most 500,000 a member a day in a party, and a real day (docs/darood-design.md,
-- Entries). Honour system, but typos and abuse stop here.
--   * The day is the member's own: local_date in tz. tz must be a time zone Postgres knows
--     ('bad_time_zone', 22023), and local_date can't be after today there, nor more than 7 days
--     before it ('bad_day', 23514): an entry made offline still gets in once the phone reconnects.
--   * A member's entries in one party on one local_date add up to 500,000 at most
--     ('daily_limit', 23514, HTTP 400). Another party's count is its own.
--   * Named to run after darood_entry_guard (triggers run in name order), which locks the party's
--     row: the day's sum can't race another entry in the same party.
-- Re-runnable: CREATE OR REPLACE, and the trigger is dropped first.
-- Rollback: DROP TRIGGER IF EXISTS darood_entry_limits ON public.darood_entries;
--   DROP FUNCTION IF EXISTS public.darood_entry_limits();

CREATE OR REPLACE FUNCTION public.darood_entry_limits()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    today DATE;
    so_far BIGINT;
BEGIN
    BEGIN
        today := (now() AT TIME ZONE NEW.tz)::DATE;
    EXCEPTION WHEN invalid_parameter_value THEN
        RAISE EXCEPTION 'bad_time_zone' USING ERRCODE = 'invalid_parameter_value',
            DETAIL = 'tz must be a time zone name such as Asia/Karachi.';
    END;
    IF NEW.local_date > today OR NEW.local_date < today - 7 THEN
        RAISE EXCEPTION 'bad_day' USING ERRCODE = 'check_violation',
            DETAIL = 'An entry is for today, or up to 7 days back, in the member''s time zone.';
    END IF;

    SELECT coalesce(sum(e.amount), 0) INTO so_far
    FROM public.darood_entries e
    WHERE e.party_id = NEW.party_id AND e.user_id = NEW.user_id AND e.local_date = NEW.local_date;

    IF so_far + NEW.amount > 500000 THEN
        RAISE EXCEPTION 'daily_limit' USING ERRCODE = 'check_violation',
            DETAIL = format('At most 500,000 a day; %s posted already that day.', so_far);
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS darood_entry_limits ON public.darood_entries;
CREATE TRIGGER darood_entry_limits
    BEFORE INSERT ON public.darood_entries
    FOR EACH ROW
    EXECUTE FUNCTION public.darood_entry_limits();

REVOKE ALL ON FUNCTION public.darood_entry_limits() FROM public, anon, authenticated;
