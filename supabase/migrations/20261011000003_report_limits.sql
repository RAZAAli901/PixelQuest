-- Migration: 20261011000003_report_limits.sql
-- Description: Reports can't be used to spam.
--   * A player reports another player at most once a UTC day. A repeat fails with 23505 (HTTP 409),
--     and the app says it's already reported.
--   * A player can't report themselves (23514, HTTP 400).
--   * created_at is the server's clock. A client could set it, which would get round the daily limit.
-- Existing rows: same-day duplicates keep the earliest, and self-reports are removed.

DELETE FROM public.reports r
USING public.reports o
WHERE r.reporter_id = o.reporter_id
  AND r.reported_profile_id = o.reported_profile_id
  AND (r.created_at AT TIME ZONE 'UTC')::date = (o.created_at AT TIME ZONE 'UTC')::date
  AND (r.created_at, r.id) > (o.created_at, o.id);

DELETE FROM public.reports WHERE reporter_id = reported_profile_id;

ALTER TABLE public.reports
    ADD CONSTRAINT reports_not_yourself CHECK (reporter_id <> reported_profile_id);

CREATE OR REPLACE FUNCTION public.stamp_report_time()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = ''
AS $$
BEGIN
    NEW.created_at := now();
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS stamp_report_time ON public.reports;
CREATE TRIGGER stamp_report_time
    BEFORE INSERT ON public.reports
    FOR EACH ROW
    EXECUTE FUNCTION public.stamp_report_time();

CREATE UNIQUE INDEX IF NOT EXISTS reports_once_a_day
    ON public.reports (reporter_id, reported_profile_id, ((created_at AT TIME ZONE 'UTC')::date));
