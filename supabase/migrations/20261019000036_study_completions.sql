-- Migration: 20261019000036_study_completions.sql
-- Description: Who finished which study item (docs/study-party.md, Completing).
--   * One row per member per item (study_completions_one_each): a member ticks an item off once.
--   * completed_at is the moment; local_date and tz are the member's own day and time zone then,
--     which decide the week it counts in, as a Darood entry's do.
--   * party_id is the item's party, kept on the row so a party's completions are found, and
--     followed live, by party (the guard in 20261019000041 fills it from the item).
--   * client_id is the phone's own key: a completion sent again (its answer lost) is the same row,
--     never a second (study_completions_client_id).
--   * Deleted with the item, the party or the account.
--   * Row-level security on, with no policy yet: nobody reads or writes a row until 20261019000039.
-- Re-runnable: IF NOT EXISTS.
-- Rollback: DROP TABLE IF EXISTS public.study_completions;

CREATE TABLE IF NOT EXISTS public.study_completions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    item_id UUID NOT NULL REFERENCES public.study_items(id) ON DELETE CASCADE,
    party_id UUID NOT NULL REFERENCES public.parties(id) ON DELETE CASCADE,
    user_id UUID NOT NULL DEFAULT auth.uid() REFERENCES auth.users(id) ON DELETE CASCADE,
    completed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    local_date DATE NOT NULL,
    tz TEXT NOT NULL,
    client_id UUID,
    CONSTRAINT study_completions_one_each UNIQUE (item_id, user_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS study_completions_client_id ON public.study_completions (user_id, client_id) WHERE client_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS study_completions_party ON public.study_completions (party_id, user_id, local_date);

ALTER TABLE public.study_completions ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.study_completions FROM anon;

COMMENT ON TABLE public.study_completions IS 'Who finished which study item, when, and on which of their own days.';
