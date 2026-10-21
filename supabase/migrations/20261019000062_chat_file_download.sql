-- Migration: 20261019000062_chat_file_download.sql
-- Description: The database's half of get-download-url (docs/study-chat.md, How a file is shared).
--   * chat_file_for_download(p_user, p_file): for the Edge Function only (the service role), which
--     has checked the caller's sign-in. A ready file, not deleted, in a study party the caller is
--     an active member of, right now: its storage path, shown name, type and size. Anyone else, or
--     anything else: not_allowed, the same answer whether the file exists or not.
-- Re-runnable: CREATE OR REPLACE.
-- Rollback: DROP FUNCTION public.chat_file_for_download(UUID, UUID).

CREATE OR REPLACE FUNCTION public.chat_file_for_download(p_user UUID, p_file UUID)
RETURNS JSONB
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    f public.chat_files%ROWTYPE;
BEGIN
    SELECT cf.* INTO f
    FROM public.chat_files cf
    JOIN public.party_members m ON m.party_id = cf.party_id AND m.user_id = p_user AND m.status = 'active'
    JOIN public.parties p ON p.id = cf.party_id AND p.party_type = 'study' AND p.deleted_at IS NULL
    WHERE cf.id = p_file AND cf.status = 'ready' AND cf.deleted_at IS NULL;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'not_allowed' USING ERRCODE = 'insufficient_privilege';
    END IF;
    RETURN jsonb_build_object('storage_path', f.storage_path, 'display_name', f.display_name, 'mime', f.mime, 'size_bytes', f.size_bytes);
END;
$$;

REVOKE ALL ON FUNCTION public.chat_file_for_download(UUID, UUID) FROM public, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.chat_file_for_download(UUID, UUID) TO service_role;
