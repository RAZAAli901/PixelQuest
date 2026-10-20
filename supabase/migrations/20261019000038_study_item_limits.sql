-- Migration: 20261019000038_study_item_limits.sql
-- Description: The limits on a study item (docs/study-party.md, Items), held by the table itself,
-- so no way of writing an item gets round them.
--   * study_item_title: 2 to 80 characters, with no spaces at either end.
--   * study_item_subject: none, or 1 to 40 characters, trimmed.
--   * study_item_notes: none, or up to 500 characters.
--   * study_item_quiz_due: a quiz has a due moment; an assignment may have none.
-- Re-runnable: DROP CONSTRAINT IF EXISTS first.
-- Rollback: ALTER TABLE public.study_items DROP CONSTRAINT IF EXISTS study_item_title, ... (each below).

ALTER TABLE public.study_items DROP CONSTRAINT IF EXISTS study_item_title;
ALTER TABLE public.study_items ADD CONSTRAINT study_item_title
    CHECK (char_length(title) BETWEEN 2 AND 80 AND title = btrim(title));

ALTER TABLE public.study_items DROP CONSTRAINT IF EXISTS study_item_subject;
ALTER TABLE public.study_items ADD CONSTRAINT study_item_subject
    CHECK (subject IS NULL OR (char_length(subject) BETWEEN 1 AND 40 AND subject = btrim(subject)));

ALTER TABLE public.study_items DROP CONSTRAINT IF EXISTS study_item_notes;
ALTER TABLE public.study_items ADD CONSTRAINT study_item_notes
    CHECK (notes IS NULL OR char_length(notes) <= 500);

ALTER TABLE public.study_items DROP CONSTRAINT IF EXISTS study_item_quiz_due;
ALTER TABLE public.study_items ADD CONSTRAINT study_item_quiz_due
    CHECK (kind <> 'quiz' OR due_at IS NOT NULL);
