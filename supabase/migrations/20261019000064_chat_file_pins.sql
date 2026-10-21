-- Migration: 20261019000064_chat_file_pins.sql
-- Description: The study party's leader pins shared files to the top of Files and This week
-- (docs/study-chat.md). pin_chat_file(file_id, pinned): the party's leader only, an active
-- member, a ready file not deleted; not_allowed otherwise. No client writes chat_files directly.
-- Re-runnable: CREATE OR REPLACE.
-- Rollback: DROP FUNCTION public.pin_chat_file(UUID, BOOLEAN).

CREATE OR REPLACE FUNCTION public.pin_chat_file(file_id UUID, pinned BOOLEAN)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
#variable_conflict use_column
DECLARE
    target UUID := pin_chat_file.file_id;
    pin BOOLEAN := pin_chat_file.pinned;
    party UUID;
BEGIN
    SELECT f.party_id INTO party FROM public.chat_files f WHERE f.id = target AND f.status = 'ready' AND f.deleted_at IS NULL;
    IF party IS NULL OR NOT private.is_active_study_member(party) OR NOT private.is_party_leader(party) THEN
        RAISE EXCEPTION 'not_allowed' USING ERRCODE = 'insufficient_privilege';
    END IF;
    UPDATE public.chat_files SET pinned = pin WHERE id = target;
END;
$$;

REVOKE ALL ON FUNCTION public.pin_chat_file(UUID, BOOLEAN) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.pin_chat_file(UUID, BOOLEAN) TO authenticated;
