-- Migration: 20261019000052_chat_reads.sql
-- Description: How far each member has read a study party's chat (docs/study-chat.md, Messages):
--   their last_read_at per party, for the unread divider and the unread counts.
--   * One row per member and party, both deleted with the party or the account.
--   * A member's own row only: written by mark_chat_read (20261019000054), read by its member.
--   * Row-level security on, no policy yet.
-- Re-runnable: IF NOT EXISTS.
-- Rollback: DROP TABLE IF EXISTS public.chat_reads;

CREATE TABLE IF NOT EXISTS public.chat_reads (
    party_id UUID NOT NULL REFERENCES public.parties(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    last_read_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    PRIMARY KEY (party_id, user_id)
);

CREATE INDEX IF NOT EXISTS chat_reads_user ON public.chat_reads (user_id);

ALTER TABLE public.chat_reads ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.chat_reads FROM anon, authenticated;

COMMENT ON TABLE public.chat_reads IS 'Each member''s last read moment in a study party''s chat, for unread counts.';
