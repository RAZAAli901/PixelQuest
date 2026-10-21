-- Migration: 20261019000051_chat_files.sql
-- Description: Files shared in a study party's chat (docs/study-chat.md, Files).
--   * One row per upload, in a party (deleted with it). uploader_id is the member who shared it.
--   * storage_path: the server's own path in the private bucket study-files, <party_id>/<uuid>;
--     never the uploader's file name.
--   * display_name: the name shown, cleaned of path characters (finalize-upload).
--   * mime and size_bytes: declared when asked (request-upload), then replaced by what the stored
--     object really is once checked. sha256: worked out by the server from the stored bytes.
--   * status: pending (asked for, maybe uploading), ready (checked, shown to the party) or rejected
--     (failed a check; its bytes deleted). reject_reason says why, for the uploader.
--   * pinned: the leader pins a file to the top of the Files tab and This week.
--   * A file message (chat_messages.kind = 'file') points at its file.
--   * Row-level security on, no policy yet (20261019000054).
-- Re-runnable: IF NOT EXISTS.
-- Rollback: ALTER TABLE public.chat_messages DROP CONSTRAINT IF EXISTS chat_messages_file;
--   DROP TABLE IF EXISTS public.chat_files;

CREATE TABLE IF NOT EXISTS public.chat_files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    party_id UUID NOT NULL REFERENCES public.parties(id) ON DELETE CASCADE,
    uploader_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    storage_path TEXT NOT NULL UNIQUE,
    display_name TEXT NOT NULL,
    mime TEXT NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha256 TEXT,
    status TEXT NOT NULL DEFAULT 'pending',
    reject_reason TEXT,
    pinned BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    ready_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chat_file_status CHECK (status IN ('pending', 'ready', 'rejected'))
);

CREATE INDEX IF NOT EXISTS chat_files_party ON public.chat_files (party_id, created_at DESC);
CREATE INDEX IF NOT EXISTS chat_files_uploader ON public.chat_files (uploader_id, created_at DESC);
CREATE INDEX IF NOT EXISTS chat_files_pending ON public.chat_files (created_at) WHERE status = 'pending';

ALTER TABLE public.chat_messages DROP CONSTRAINT IF EXISTS chat_messages_file;
ALTER TABLE public.chat_messages ADD CONSTRAINT chat_messages_file
    FOREIGN KEY (file_id) REFERENCES public.chat_files(id) ON DELETE SET NULL;

ALTER TABLE public.chat_files ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.chat_files FROM anon, authenticated;

COMMENT ON TABLE public.chat_files IS 'Files shared in a study party''s chat; untrusted until finalize-upload checks them.';
COMMENT ON COLUMN public.chat_files.storage_path IS 'The server''s own path in study-files: <party_id>/<uuid>.';
