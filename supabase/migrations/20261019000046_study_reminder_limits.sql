-- Migration: 20261019000046_study_reminder_limits.sql
-- Description: The limits on a study reminder (docs/study-reminders.md), held by the table itself,
-- so no way of writing one gets round them.
--   * study_reminder_title: 2 to 80 characters, with no spaces at either end.
--   * study_reminder_when: a week reminder has its week (a Monday) and no moment; a day or time
--     reminder has its moment and no week.
--   * study_reminder_daily: only a day or time reminder repeats daily (a week can't).
--   * study_reminder_alerts: none (the defaults), or 1 to 4 alert times, each 0 to 14 days
--     (20,160 minutes) before.
--   * study_reminder_tz: a time zone name of a sane length; the guard (20261019000048) checks it's real.
-- Re-runnable: DROP CONSTRAINT IF EXISTS first.
-- Rollback: ALTER TABLE public.study_reminders DROP CONSTRAINT IF EXISTS study_reminder_title, ... (each below).

ALTER TABLE public.study_reminders DROP CONSTRAINT IF EXISTS study_reminder_title;
ALTER TABLE public.study_reminders ADD CONSTRAINT study_reminder_title
    CHECK (char_length(title) BETWEEN 2 AND 80 AND title = btrim(title));

ALTER TABLE public.study_reminders DROP CONSTRAINT IF EXISTS study_reminder_when;
ALTER TABLE public.study_reminders ADD CONSTRAINT study_reminder_when
    CHECK (
        (scope = 'week' AND week_start IS NOT NULL AND EXTRACT(ISODOW FROM week_start) = 1 AND due_at IS NULL)
        OR (scope IN ('day', 'time') AND due_at IS NOT NULL AND week_start IS NULL)
    );

ALTER TABLE public.study_reminders DROP CONSTRAINT IF EXISTS study_reminder_daily;
ALTER TABLE public.study_reminders ADD CONSTRAINT study_reminder_daily
    CHECK (repeat <> 'daily' OR scope IN ('day', 'time'));

ALTER TABLE public.study_reminders DROP CONSTRAINT IF EXISTS study_reminder_alerts;
ALTER TABLE public.study_reminders ADD CONSTRAINT study_reminder_alerts
    CHECK (
        alert_offsets IS NULL
        OR (cardinality(alert_offsets) BETWEEN 1 AND 4 AND 0 <= ALL (alert_offsets) AND 20160 >= ALL (alert_offsets))
    );

ALTER TABLE public.study_reminders DROP CONSTRAINT IF EXISTS study_reminder_tz;
ALTER TABLE public.study_reminders ADD CONSTRAINT study_reminder_tz
    CHECK (char_length(tz) BETWEEN 1 AND 64);
