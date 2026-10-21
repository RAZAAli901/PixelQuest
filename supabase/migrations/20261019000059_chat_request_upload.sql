-- Migration: 20261019000059_chat_request_upload.sql
-- Description: The database's half of request-upload (docs/study-chat.md, How a file is shared).
--   * chat_request_upload(p_user, p_party, p_name, p_mime, p_size): for the Edge Function only
--     (the service role), which has checked the caller's sign-in and passes their id. It checks
--     the member is active in that study party (not_a_member), then makes the pending file row at
--     a path of the server's own choosing, <party_id>/<uuid>. The table's rules and triggers hold
--     the rest: the type and size (chat_file_mime, chat_file_size), the name (chat_file_name), the
--     party's 500 MB (party_storage_full) and the member's 20 a day (too_many_uploads).
--   * Returns {file_id, storage_path}. No client may call it.
-- Re-runnable: CREATE OR REPLACE.
-- Rollback: DROP FUNCTION public.chat_request_upload(UUID, UUID, TEXT, TEXT, BIGINT).

CREATE OR REPLACE FUNCTION public.chat_request_upload(p_user UUID, p_party UUID, p_name TEXT, p_mime TEXT, p_size BIGINT)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    file UUID := gen_random_uuid();
    path TEXT := p_party::TEXT || '/' || gen_random_uuid()::TEXT;
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM public.party_members m
        JOIN public.parties p ON p.id = m.party_id
        WHERE m.party_id = p_party AND m.user_id = p_user AND m.status = 'active'
          AND p.party_type = 'study' AND p.deleted_at IS NULL
    ) THEN
        RAISE EXCEPTION 'not_a_member' USING ERRCODE = 'insufficient_privilege';
    END IF;
    INSERT INTO public.chat_files (id, party_id, uploader_id, storage_path, display_name, mime, size_bytes, status)
    VALUES (file, p_party, p_user, path, p_name, p_mime, p_size, 'pending');
    RETURN jsonb_build_object('file_id', file, 'storage_path', path);
END;
$$;

REVOKE ALL ON FUNCTION public.chat_request_upload(UUID, UUID, TEXT, TEXT, BIGINT) FROM public, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.chat_request_upload(UUID, UUID, TEXT, TEXT, BIGINT) TO service_role;
