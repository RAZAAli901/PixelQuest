-- Migration: 20261010000003_reports_foreign_key_indexes.sql
-- Description: Index the two foreign keys on public.reports (Supabase's Performance Advisor:
-- unindexed_foreign_keys). Deleting an account (delete_user_account) cascades into reports by
-- reporter and by reported profile; without these, each deletion scanned the whole table. They also
-- serve the moderation queries "reports about this player".

CREATE INDEX IF NOT EXISTS reports_reporter_id_idx ON public.reports (reporter_id);
CREATE INDEX IF NOT EXISTS reports_reported_profile_id_idx ON public.reports (reported_profile_id);
