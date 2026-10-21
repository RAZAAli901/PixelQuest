-- Migration: 20261019000063_chat_realtime.sql
-- Description: Live updates for study chats (docs/study-chat.md, On the phone).
--   * chat_messages and chat_files join the supabase_realtime publication, so an open chat hears
--     each new message, each delete (a soft delete is a change of its row) and each file becoming
--     ready or rejected, as it happens.
--   * Realtime checks each subscriber's own row-level security before sending a change, so only a
--     study party's active members hear about its messages and ready files, and only an uploader
--     about their own pending or rejected one.
--   * The app takes a change only as a sign to fetch: what it shows always comes from its own
--     read of the tables, under the same rules.
-- Re-runnable: each table is added only if it isn't there yet.
-- Rollback: ALTER PUBLICATION supabase_realtime DROP TABLE public.chat_messages, public.chat_files;

DO $$
DECLARE
    t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['chat_messages', 'chat_files'] LOOP
        IF NOT EXISTS (
            SELECT 1 FROM pg_publication_tables
            WHERE pubname = 'supabase_realtime' AND schemaname = 'public' AND tablename = t
        ) THEN
            EXECUTE format('ALTER PUBLICATION supabase_realtime ADD TABLE public.%I', t);
        END IF;
    END LOOP;
END;
$$;
