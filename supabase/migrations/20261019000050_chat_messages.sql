-- Migration: 20261019000050_chat_messages.sql
-- Description: A study party's chat messages (docs/study-chat.md, Messages).
--   * One row per message, in a party (deleted with it). sender_id is the member who sent it; an
--     account deleted takes its messages with it.
--   * kind: text, file (a shared file's message, file_id set in 20261019000051) or system (the
--     server's own lines; body is a token the app words).
--   * reply_to: a message it answers; a reply stays if that one goes (set null).
--   * client_id: the phone's own key, so a message sent again after its answer was lost is stored once.
--   * deleted_at: a soft delete. The text and file are gone for everyone; the row stays, so
--     replies still make sense. Nobody edits a message.
--   * Row-level security is on from the start, with no policy yet: nobody reads or writes a row
--     until the policies come (20261019000054 and 20261019000055).
-- Re-runnable: IF NOT EXISTS.
-- Rollback: DROP TABLE IF EXISTS public.chat_messages;

CREATE TABLE IF NOT EXISTS public.chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    party_id UUID NOT NULL REFERENCES public.parties(id) ON DELETE CASCADE,
    sender_id UUID DEFAULT auth.uid() REFERENCES auth.users(id) ON DELETE CASCADE,
    kind TEXT NOT NULL DEFAULT 'text',
    body TEXT,
    file_id UUID,
    reply_to UUID REFERENCES public.chat_messages(id) ON DELETE SET NULL,
    client_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chat_message_kind CHECK (kind IN ('text', 'file', 'system'))
);

CREATE INDEX IF NOT EXISTS chat_messages_party ON public.chat_messages (party_id, created_at DESC);
CREATE INDEX IF NOT EXISTS chat_messages_sender ON public.chat_messages (sender_id, created_at DESC);
CREATE UNIQUE INDEX IF NOT EXISTS chat_messages_client_key
    ON public.chat_messages (sender_id, client_id) WHERE client_id IS NOT NULL;

ALTER TABLE public.chat_messages ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.chat_messages FROM anon, authenticated;

COMMENT ON TABLE public.chat_messages IS 'A study party''s chat: text, shared files and the server''s own lines.';
COMMENT ON COLUMN public.chat_messages.deleted_at IS 'Soft delete: the text and file are gone for everyone, the row stays for replies.';
