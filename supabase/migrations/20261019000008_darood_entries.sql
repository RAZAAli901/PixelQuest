-- Migration: 20261019000008_darood_entries.sql
-- Description: Darood entries (docs/darood-design.md, Entries): one row per number a member posts.
--   * user_id defaults to the caller. If that account is deleted it empties (ON DELETE SET NULL),
--     and the entry stays in the party's total.
--   * local_date is the member's own day when they posted, and tz their time zone then, so a day
--     is counted where they are.
--   * created_at is the server's time, never the phone's.
-- Indexes: the chat pages a party's entries newest first; the daily cap and the stats read one
-- member's entries by day.
-- Re-runnable: IF NOT EXISTS.
-- Rollback: DROP TABLE IF EXISTS public.darood_entries CASCADE;

CREATE TABLE IF NOT EXISTS public.darood_entries (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    party_id UUID NOT NULL REFERENCES public.parties(id) ON DELETE CASCADE,
    user_id UUID DEFAULT auth.uid() REFERENCES auth.users(id) ON DELETE SET NULL,
    amount INTEGER NOT NULL,
    local_date DATE NOT NULL,
    tz TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS darood_entries_party_newest ON public.darood_entries (party_id, created_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS darood_entries_member_day ON public.darood_entries (party_id, user_id, local_date);

-- Closed to clients until its policies come, as parties is.
ALTER TABLE public.darood_entries ENABLE ROW LEVEL SECURITY;

COMMENT ON TABLE public.darood_entries IS 'Darood Party entries: a member''s number for a day. Never edited or deleted by clients.';
COMMENT ON COLUMN public.darood_entries.local_date IS 'The member''s own date when they posted, in tz.';
