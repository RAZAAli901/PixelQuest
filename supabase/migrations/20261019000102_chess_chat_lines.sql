-- Migration: 20261019000102_chess_chat_lines.sql
-- Description: Chess in the study chat (docs/chess-ui-integration.md): the server's own lines can
-- point at what they're about (ref_id), and an open challenge posts one ("chess_open_challenge",
-- the challenge's id), which any member can take from the chat.
--   * ref_id is only for system lines, and only the server sets it: a member's own message has it
--     cleared (chat_message_guard), so nobody can post a line that looks like the server's.
-- Re-runnable: ADD COLUMN IF NOT EXISTS, DROP CONSTRAINT IF EXISTS, CREATE OR REPLACE.
-- Rollback: re-run chat_message_guard (20261019000055) and chess_create_challenge (20261019000096);
--   ALTER TABLE public.chat_messages DROP COLUMN ref_id.

ALTER TABLE public.chat_messages ADD COLUMN IF NOT EXISTS ref_id UUID;
ALTER TABLE public.chat_messages DROP CONSTRAINT IF EXISTS chat_message_ref;
ALTER TABLE public.chat_messages ADD CONSTRAINT chat_message_ref CHECK (ref_id IS NULL OR kind = 'system');

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
        NEW.ref_id := NULL;
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

-- A chess line in a party's chat, from the server.
CREATE OR REPLACE FUNCTION private.chess_chat_line(party UUID, token TEXT, ref UUID)
RETURNS VOID
LANGUAGE sql
VOLATILE
SECURITY DEFINER
SET search_path = ''
AS $$
    INSERT INTO public.chat_messages (party_id, sender_id, kind, body, ref_id) VALUES (party, NULL, 'system', token, ref);
$$;
REVOKE ALL ON FUNCTION private.chess_chat_line(UUID, TEXT, UUID) FROM public, anon, authenticated;

CREATE OR REPLACE FUNCTION private.chess_open_challenge_line()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    IF NEW.to_user IS NULL THEN
        PERFORM private.chess_chat_line(NEW.party_id, 'chess_open_challenge', NEW.id);
    END IF;
    RETURN NULL;
END;
$$;
REVOKE ALL ON FUNCTION private.chess_open_challenge_line() FROM public, anon, authenticated;

DROP TRIGGER IF EXISTS chess_open_challenge_line ON public.chess_challenges;
CREATE TRIGGER chess_open_challenge_line
    AFTER INSERT ON public.chess_challenges
    FOR EACH ROW
    EXECUTE FUNCTION private.chess_open_challenge_line();
