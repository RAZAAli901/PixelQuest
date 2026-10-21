-- Migration: 20261019000061_chat_upload_cleanup.sql
-- Description: Files that shouldn't be kept are let go (docs/study-chat.md, How a file is shared).
-- For the Edge Functions only (the service role), which delete the bytes from study-files.
--   * chat_expire_uploads(p_limit): a pending upload never finished within an hour is rejected
--     (upload_expired). Returns up to p_limit storage paths whose bytes are to be deleted: files
--     rejected or deleted, not yet purged; and objects in study-files that no file row names,
--     older than an hour (an upload whose row went with its party).
--   * chat_mark_purged(p_paths): those paths' bytes are gone (chat_files.purged_at).
-- Re-runnable: IF NOT EXISTS, CREATE OR REPLACE.
-- Rollback: DROP FUNCTION public.chat_expire_uploads(INTEGER), public.chat_mark_purged(TEXT[]);
--   ALTER TABLE public.chat_files DROP COLUMN purged_at.

ALTER TABLE public.chat_files ADD COLUMN IF NOT EXISTS purged_at TIMESTAMP WITH TIME ZONE;
CREATE INDEX IF NOT EXISTS chat_files_to_purge ON public.chat_files (created_at)
    WHERE purged_at IS NULL AND (status = 'rejected' OR deleted_at IS NOT NULL);

CREATE OR REPLACE FUNCTION public.chat_expire_uploads(p_limit INTEGER DEFAULT 100)
RETURNS TEXT[]
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    paths TEXT[];
BEGIN
    UPDATE public.chat_files
    SET status = 'rejected', reject_reason = 'upload_expired'
    WHERE status = 'pending' AND created_at < now() - INTERVAL '1 hour';

    SELECT array_agg(path) INTO paths FROM (
        SELECT f.storage_path AS path
        FROM public.chat_files f
        WHERE f.purged_at IS NULL AND (f.status = 'rejected' OR f.deleted_at IS NOT NULL)
        UNION
        SELECT o.name
        FROM storage.objects o
        WHERE o.bucket_id = 'study-files' AND o.created_at < now() - INTERVAL '1 hour'
          AND NOT EXISTS (SELECT 1 FROM public.chat_files f WHERE f.storage_path = o.name)
        LIMIT greatest(1, least(p_limit, 1000))
    ) AS due;
    RETURN coalesce(paths, ARRAY[]::TEXT[]);
END;
$$;

CREATE OR REPLACE FUNCTION public.chat_mark_purged(p_paths TEXT[])
RETURNS VOID
LANGUAGE sql
SECURITY DEFINER
SET search_path = ''
AS $$
    UPDATE public.chat_files SET purged_at = now() WHERE storage_path = ANY (p_paths) AND purged_at IS NULL;
$$;

REVOKE ALL ON FUNCTION public.chat_expire_uploads(INTEGER) FROM public, anon, authenticated;
REVOKE ALL ON FUNCTION public.chat_mark_purged(TEXT[]) FROM public, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.chat_expire_uploads(INTEGER) TO service_role;
GRANT EXECUTE ON FUNCTION public.chat_mark_purged(TEXT[]) TO service_role;
