-- Migration: 20261019000044_study_realtime.sql
-- Description: Live updates for study parties (docs/study-party.md, Privacy).
--   * study_items, study_completions and study_sessions join the supabase_realtime publication, so a
--     party's open screen hears each new or changed item, each tick and each study session as it
--     happens, for its activity feed.
--   * Realtime checks each subscriber's own row-level security before sending a change, so only a
--     study party's active members hear about it, as only they can read it.
--   * The app listens for new and changed rows only. What's taken away (an undone tick, a deleted
--     item) shows at the next refresh: on opening or pulling the screen, or after the next change.
-- Re-runnable: each table is added only if it isn't there yet.
-- Rollback: ALTER PUBLICATION supabase_realtime DROP TABLE public.study_items, public.study_completions, public.study_sessions;

DO $$
DECLARE
    t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['study_items', 'study_completions', 'study_sessions'] LOOP
        IF NOT EXISTS (
            SELECT 1 FROM pg_publication_tables
            WHERE pubname = 'supabase_realtime' AND schemaname = 'public' AND tablename = t
        ) THEN
            EXECUTE format('ALTER PUBLICATION supabase_realtime ADD TABLE public.%I', t);
        END IF;
    END LOOP;
END;
$$;
