-- Migration: 20261019000045_study_reminders.sql
-- Description: A study party member's reminders (docs/study-reminders.md): "Quiz this week",
--   "Exam on Thursday", "Hand in at 10:00". Reminders never touch points, XP, levels or streaks.
--   * One row per reminder, in a party (deleted with it). created_by is the member who made it,
--     and its `me` reminders go when that account is deleted.
--   * kind: quiz, assignment, exam or custom. scope: week (week_start), day or time (due_at).
--   * tz: the creator's time zone name, which says which day and week the reminder means.
--   * repeat: none, daily or weekly. audience: party (every active member) or me (its creator).
--   * alert_offsets: the creator's own alert times, in minutes before the reminder's moment;
--     null means the defaults.
--   * Row-level security is on from the start, with no policy yet: nobody reads or writes a row
--     until the policies come (20261019000047 and 20261019000048). The limits come in 20261019000046.
-- Re-runnable: IF NOT EXISTS.
-- Rollback: DROP TABLE IF EXISTS public.study_reminders;

CREATE TABLE IF NOT EXISTS public.study_reminders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    party_id UUID NOT NULL REFERENCES public.parties(id) ON DELETE CASCADE,
    created_by UUID DEFAULT auth.uid() REFERENCES auth.users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    kind TEXT NOT NULL,
    scope TEXT NOT NULL,
    due_at TIMESTAMP WITH TIME ZONE,
    week_start DATE,
    tz TEXT NOT NULL,
    repeat TEXT NOT NULL DEFAULT 'none',
    audience TEXT NOT NULL DEFAULT 'party',
    alert_offsets INTEGER[],
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT study_reminder_kind CHECK (kind IN ('quiz', 'assignment', 'exam', 'custom')),
    CONSTRAINT study_reminder_scope CHECK (scope IN ('week', 'day', 'time')),
    CONSTRAINT study_reminder_repeat CHECK (repeat IN ('none', 'daily', 'weekly')),
    CONSTRAINT study_reminder_audience CHECK (audience IN ('party', 'me'))
);

CREATE INDEX IF NOT EXISTS study_reminders_party ON public.study_reminders (party_id, created_at);
CREATE INDEX IF NOT EXISTS study_reminders_created_by ON public.study_reminders (created_by);

ALTER TABLE public.study_reminders ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.study_reminders FROM anon;

COMMENT ON TABLE public.study_reminders IS 'A study party member''s reminders, for themselves or the whole party.';
COMMENT ON COLUMN public.study_reminders.tz IS 'The creator''s time zone: which day and week the reminder means.';
COMMENT ON COLUMN public.study_reminders.alert_offsets IS 'The creator''s alert times, minutes before the reminder''s moment; null for the defaults.';
