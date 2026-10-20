-- Migration: 20261019000035_study_items.sql
-- Description: A study party's items: its assignments and quizzes (docs/study-party.md, Items).
--   * One row per item, in a party (deleted with it). created_by is the member who added it,
--     emptied (the item kept) if that account is deleted.
--   * kind is assignment or quiz; due_at is a moment with its time zone; pinned is the leader's.
--   * Row-level security is on from the start, with no policy yet: nobody reads or writes a row
--     until the policies come (20261019000039 and after). The limits come in 20261019000038.
-- Re-runnable: IF NOT EXISTS.
-- Rollback: DROP TABLE IF EXISTS public.study_items;

CREATE TABLE IF NOT EXISTS public.study_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    party_id UUID NOT NULL REFERENCES public.parties(id) ON DELETE CASCADE,
    kind TEXT NOT NULL,
    title TEXT NOT NULL,
    subject TEXT,
    notes TEXT,
    due_at TIMESTAMP WITH TIME ZONE,
    pinned BOOLEAN NOT NULL DEFAULT false,
    created_by UUID DEFAULT auth.uid() REFERENCES auth.users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT study_item_kind CHECK (kind IN ('assignment', 'quiz'))
);

CREATE INDEX IF NOT EXISTS study_items_party_due ON public.study_items (party_id, due_at);
CREATE INDEX IF NOT EXISTS study_items_created_by ON public.study_items (created_by);

ALTER TABLE public.study_items ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.study_items FROM anon;

COMMENT ON TABLE public.study_items IS 'A study party''s assignments and quizzes: what is due, and when.';
COMMENT ON COLUMN public.study_items.pinned IS 'The leader''s highlight, shown at the top of the party.';
