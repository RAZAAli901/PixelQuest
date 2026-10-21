-- Migration: 20261019000054_chat_rls.sql
-- Description: Who reads and sends in a study party's chat (docs/study-chat.md).
--   * Reading messages: the party's active members only (private.is_active_study_member).
--     Outsiders, the invited, members who left or were removed, Darood Parties' members and the
--     signed out read nothing, from the moment they stop being members.
--   * Reading files: the same members, and only files checked and ready; a pending or rejected one
--     is its uploader's alone (to see its progress, or why it failed). storage_path isn't granted:
--     files are fetched through get-download-url only.
--   * Sending: an active member sends text as themselves. The guard fills in who and when, keeps
--     a reply in its own party, and lets no client write a file or system message (only the
--     server's functions do, with the service role). Nobody updates a message: there's no grant.
--   * Read state: a member reads their own chat_reads rows; mark_chat_read(party) moves their
--     own last_read_at on (never back), for an active member only.
-- Re-runnable: CREATE OR REPLACE, DROP POLICY IF EXISTS, DROP TRIGGER IF EXISTS.
-- Rollback: DROP POLICY for each policy below; DROP TRIGGER chat_message_guard;
--   DROP FUNCTION public.chat_message_guard(), public.mark_chat_read(UUID); REVOKE the grants.

GRANT SELECT ON public.chat_messages TO authenticated;
GRANT INSERT (party_id, kind, body, reply_to, client_id) ON public.chat_messages TO authenticated;
GRANT SELECT (id, party_id, uploader_id, display_name, mime, size_bytes, sha256, status, reject_reason, pinned, created_at, ready_at)
    ON public.chat_files TO authenticated;
GRANT SELECT ON public.chat_reads TO authenticated;

DROP POLICY IF EXISTS "Members read their study party's messages" ON public.chat_messages;
CREATE POLICY "Members read their study party's messages"
    ON public.chat_messages FOR SELECT TO authenticated
    USING (private.is_active_study_member(party_id));

DROP POLICY IF EXISTS "Members send text as themselves" ON public.chat_messages;
CREATE POLICY "Members send text as themselves"
    ON public.chat_messages FOR INSERT TO authenticated
    WITH CHECK (sender_id = (SELECT auth.uid()) AND kind = 'text' AND private.is_active_study_member(party_id));

DROP POLICY IF EXISTS "Members read ready files, uploaders their own" ON public.chat_files;
CREATE POLICY "Members read ready files, uploaders their own"
    ON public.chat_files FOR SELECT TO authenticated
    USING (private.is_active_study_member(party_id) AND (status = 'ready' OR uploader_id = (SELECT auth.uid())));

DROP POLICY IF EXISTS "Members read their own read state" ON public.chat_reads;
CREATE POLICY "Members read their own read state"
    ON public.chat_reads FOR SELECT TO authenticated
    USING (user_id = (SELECT auth.uid()));

CREATE OR REPLACE FUNCTION public.chat_message_guard()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    -- A member's own message: who and when are the server's. The service role (auth.uid() null)
    -- writes file and system lines with their own values.
    IF (SELECT auth.uid()) IS NOT NULL THEN
        NEW.sender_id := (SELECT auth.uid());
        NEW.created_at := now();
        NEW.deleted_at := NULL;
        NEW.file_id := NULL;
    END IF;
    IF NEW.reply_to IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM public.chat_messages r WHERE r.id = NEW.reply_to AND r.party_id = NEW.party_id
    ) THEN
        RAISE EXCEPTION 'bad_reply' USING ERRCODE = 'check_violation',
            DETAIL = 'A reply answers a message in the same party.';
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS chat_message_guard ON public.chat_messages;
CREATE TRIGGER chat_message_guard
    BEFORE INSERT ON public.chat_messages
    FOR EACH ROW
    EXECUTE FUNCTION public.chat_message_guard();

REVOKE ALL ON FUNCTION public.chat_message_guard() FROM public, anon, authenticated;

CREATE OR REPLACE FUNCTION public.mark_chat_read(party_id UUID)
RETURNS TIMESTAMP WITH TIME ZONE
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
#variable_conflict use_column
DECLARE
    me UUID := (SELECT auth.uid());
    party UUID := mark_chat_read.party_id;
    at TIMESTAMP WITH TIME ZONE;
BEGIN
    IF me IS NULL OR NOT private.is_active_study_member(party) THEN
        RAISE EXCEPTION 'not_a_member' USING ERRCODE = 'insufficient_privilege';
    END IF;
    INSERT INTO public.chat_reads AS r (party_id, user_id, last_read_at)
    VALUES (party, me, now())
    ON CONFLICT (party_id, user_id) DO UPDATE SET last_read_at = greatest(r.last_read_at, excluded.last_read_at)
    RETURNING r.last_read_at INTO at;
    RETURN at;
END;
$$;

REVOKE ALL ON FUNCTION public.mark_chat_read(UUID) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.mark_chat_read(UUID) TO authenticated;
