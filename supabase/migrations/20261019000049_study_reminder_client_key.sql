-- Migration: 20261019000049_study_reminder_client_key.sql
-- Description: A reminder made on a phone carries the phone's own key (docs/study-reminders.md,
--   On the phone), as ticks and study time do: sent again after its answer was lost, the server
--   refuses the second (409, a unique key) and the phone reads back the first, so a party never
--   gets the same reminder, and its alerts, twice.
--   * client_id: optional, set only when the reminder is added; one per creator.
--   * It never changes: there's no update grant for it.
-- Re-runnable: IF NOT EXISTS.
-- Rollback: DROP INDEX public.study_reminders_client_key;
--   ALTER TABLE public.study_reminders DROP COLUMN client_id;

ALTER TABLE public.study_reminders ADD COLUMN IF NOT EXISTS client_id UUID;

CREATE UNIQUE INDEX IF NOT EXISTS study_reminders_client_key
    ON public.study_reminders (created_by, client_id)
    WHERE client_id IS NOT NULL;

GRANT INSERT (client_id) ON public.study_reminders TO authenticated;
