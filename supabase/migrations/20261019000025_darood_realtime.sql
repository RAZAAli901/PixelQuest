-- Migration: 20261019000025_darood_realtime.sql
-- Description: Live updates for Darood Parties (docs/darood-design.md).
--   * darood_entries and party_members join the supabase_realtime publication, so a party's open
--     chat hears each new entry, and each join, leave or removal, as it happens.
--   * Realtime checks each subscriber's own row-level security before sending a change, so only a
--     party's active members hear about it, as only they can read it.
--   * The app also refreshes when the screen opens or is pulled, in case a change was missed.
-- Re-runnable: each table is added only if it isn't there yet.
-- Rollback: ALTER PUBLICATION supabase_realtime DROP TABLE public.darood_entries, public.party_members;

DO $$
DECLARE
    t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['darood_entries', 'party_members'] LOOP
        IF NOT EXISTS (
            SELECT 1 FROM pg_publication_tables
            WHERE pubname = 'supabase_realtime' AND schemaname = 'public' AND tablename = t
        ) THEN
            EXECUTE format('ALTER PUBLICATION supabase_realtime ADD TABLE public.%I', t);
        END IF;
    END LOOP;
END;
$$;
