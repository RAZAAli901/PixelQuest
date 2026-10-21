-- Migration: 20261019000060_chat_finalize_upload.sql
-- Description: The database's half of finalize-upload (docs/study-chat.md, How a file is shared).
-- For the Edge Function only (the service role), which has checked the caller's sign-in and the
-- stored bytes, and passes the caller's id.
--   * chat_upload_for_finalize(p_user, p_file): the uploader's own file, with when its object
--     arrived in study-files (storage.objects), or null if nothing's there yet. no_such_file, or
--     not_yours for anyone else's.
--   * chat_finalize_upload(p_user, p_file, p_mime, p_size, p_sha256): a pending file of theirs,
--     while they're still an active member, becomes ready with what it really is (its type, size
--     and SHA-256), and its file message is posted as theirs. Returns the message's id.
--     not_pending, not_a_member otherwise.
--   * chat_reject_upload(p_file, p_reason): a pending file becomes rejected, saying why, for its
--     uploader to see.
-- Re-runnable: CREATE OR REPLACE.
-- Rollback: DROP FUNCTION public.chat_upload_for_finalize(UUID, UUID),
--   public.chat_finalize_upload(UUID, UUID, TEXT, BIGINT, TEXT), public.chat_reject_upload(UUID, TEXT).

CREATE OR REPLACE FUNCTION public.chat_upload_for_finalize(p_user UUID, p_file UUID)
RETURNS JSONB
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    f public.chat_files%ROWTYPE;
    arrived TIMESTAMP WITH TIME ZONE;
BEGIN
    SELECT * INTO f FROM public.chat_files WHERE id = p_file AND deleted_at IS NULL;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'no_such_file' USING ERRCODE = 'no_data_found';
    END IF;
    IF f.uploader_id IS DISTINCT FROM p_user THEN
        RAISE EXCEPTION 'not_yours' USING ERRCODE = 'insufficient_privilege';
    END IF;
    SELECT o.created_at INTO arrived FROM storage.objects o WHERE o.bucket_id = 'study-files' AND o.name = f.storage_path;
    RETURN jsonb_build_object(
        'party_id', f.party_id, 'storage_path', f.storage_path, 'mime', f.mime, 'size_bytes', f.size_bytes,
        'status', f.status, 'created_at', f.created_at, 'uploaded_at', arrived
    );
END;
$$;

CREATE OR REPLACE FUNCTION public.chat_finalize_upload(p_user UUID, p_file UUID, p_mime TEXT, p_size BIGINT, p_sha256 TEXT)
RETURNS UUID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    f public.chat_files%ROWTYPE;
    message UUID := gen_random_uuid();
BEGIN
    SELECT * INTO f FROM public.chat_files WHERE id = p_file AND uploader_id = p_user AND deleted_at IS NULL FOR UPDATE;
    IF NOT FOUND OR f.status <> 'pending' THEN
        RAISE EXCEPTION 'not_pending' USING ERRCODE = 'check_violation';
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM public.party_members m JOIN public.parties p ON p.id = m.party_id
        WHERE m.party_id = f.party_id AND m.user_id = p_user AND m.status = 'active'
          AND p.party_type = 'study' AND p.deleted_at IS NULL
    ) THEN
        RAISE EXCEPTION 'not_a_member' USING ERRCODE = 'insufficient_privilege';
    END IF;
    IF p_sha256 !~ '^[0-9a-f]{64}$' THEN
        RAISE EXCEPTION 'bad_hash' USING ERRCODE = 'check_violation';
    END IF;
    UPDATE public.chat_files
    SET status = 'ready', mime = p_mime, size_bytes = p_size, sha256 = p_sha256, ready_at = now()
    WHERE id = p_file;
    INSERT INTO public.chat_messages (id, party_id, sender_id, kind, file_id)
    VALUES (message, f.party_id, p_user, 'file', p_file);
    RETURN message;
END;
$$;

CREATE OR REPLACE FUNCTION public.chat_reject_upload(p_file UUID, p_reason TEXT)
RETURNS VOID
LANGUAGE sql
SECURITY DEFINER
SET search_path = ''
AS $$
    UPDATE public.chat_files
    SET status = 'rejected', reject_reason = left(coalesce(nullif(p_reason, ''), 'refused'), 40)
    WHERE id = p_file AND status = 'pending';
$$;

REVOKE ALL ON FUNCTION public.chat_upload_for_finalize(UUID, UUID) FROM public, anon, authenticated;
REVOKE ALL ON FUNCTION public.chat_finalize_upload(UUID, UUID, TEXT, BIGINT, TEXT) FROM public, anon, authenticated;
REVOKE ALL ON FUNCTION public.chat_reject_upload(UUID, TEXT) FROM public, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.chat_upload_for_finalize(UUID, UUID) TO service_role;
GRANT EXECUTE ON FUNCTION public.chat_finalize_upload(UUID, UUID, TEXT, BIGINT, TEXT) TO service_role;
GRANT EXECUTE ON FUNCTION public.chat_reject_upload(UUID, TEXT) TO service_role;
