-- Migration: 20261019000055_chat_deletes.sql
-- Description: Deleting in a study party's chat (docs/study-chat.md, Messages).
--   * delete_chat_message(message_id): its sender deletes their own message; the party's leader
--     deletes any in their party. Both must be active members. The delete is soft: deleted_at is
--     set, the text cleared, and a file message's file goes with it (deleted_at on the file, which
--     then nobody reads; its bytes are removed by the cleanup, 20261019000060).
--     Deleting again changes nothing. Anyone else: not_allowed.
--   * chat_files.deleted_at: a file gone with its message (or deleted by the cleanup).
-- Re-runnable: IF NOT EXISTS, CREATE OR REPLACE, DROP POLICY IF EXISTS.
-- Rollback: DROP FUNCTION public.delete_chat_message(UUID); restore 20261019000054's file policy;
--   ALTER TABLE public.chat_files DROP COLUMN deleted_at.

ALTER TABLE public.chat_files ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;
GRANT SELECT (deleted_at) ON public.chat_files TO authenticated;

DROP POLICY IF EXISTS "Members read ready files, uploaders their own" ON public.chat_files;
CREATE POLICY "Members read ready files, uploaders their own"
    ON public.chat_files FOR SELECT TO authenticated
    USING (
        deleted_at IS NULL
        AND private.is_active_study_member(party_id)
        AND (status = 'ready' OR uploader_id = (SELECT auth.uid()))
    );

CREATE OR REPLACE FUNCTION public.delete_chat_message(message_id UUID)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    me UUID := (SELECT auth.uid());
    msg public.chat_messages%ROWTYPE;
BEGIN
    SELECT * INTO msg FROM public.chat_messages WHERE id = message_id;
    IF NOT FOUND OR me IS NULL OR NOT private.is_active_study_member(msg.party_id)
        OR NOT (msg.sender_id = me OR private.is_party_leader(msg.party_id)) THEN
        RAISE EXCEPTION 'not_allowed' USING ERRCODE = 'insufficient_privilege';
    END IF;
    IF msg.deleted_at IS NOT NULL THEN
        RETURN;
    END IF;
    UPDATE public.chat_messages SET deleted_at = now(), body = NULL, file_id = NULL WHERE id = message_id;
    IF msg.file_id IS NOT NULL THEN
        UPDATE public.chat_files SET deleted_at = now(), pinned = false WHERE id = msg.file_id AND deleted_at IS NULL;
    END IF;
END;
$$;

REVOKE ALL ON FUNCTION public.delete_chat_message(UUID) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.delete_chat_message(UUID) TO authenticated;
