-- Migration: 20261019000058_chat_rate_limits.sql
-- Description: How fast a member may write in study chats (docs/study-chat.md), counted on the
-- server whatever the phone says.
--   * Uploads: 20 per member in any 24 hours, across their parties, rejected ones included (a
--     rejected file still cost a check): too_many_uploads.
--   * Messages: 30 per member in any minute, across their parties: too_many_messages. Only a
--     member's own; the server's file and system lines aren't counted.
-- Re-runnable: CREATE OR REPLACE, DROP TRIGGER IF EXISTS.
-- Rollback: DROP TRIGGER chat_file_rate ON public.chat_files; DROP TRIGGER chat_message_rate ON
--   public.chat_messages; DROP FUNCTION public.chat_file_rate(), public.chat_message_rate().

CREATE INDEX IF NOT EXISTS chat_files_uploader_recent ON public.chat_files (uploader_id, created_at);

CREATE OR REPLACE FUNCTION public.chat_file_rate()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    PERFORM pg_advisory_xact_lock(hashtext('chat_file_rate:' || coalesce(NEW.uploader_id::TEXT, '')));
    IF (SELECT count(*) FROM public.chat_files
        WHERE uploader_id = NEW.uploader_id AND created_at > now() - INTERVAL '24 hours')
        >= (SELECT uploads_per_day FROM public.study_chat_config) THEN
        RAISE EXCEPTION 'too_many_uploads' USING ERRCODE = 'check_violation',
            DETAIL = 'A member shares at most 20 files a day.';
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS chat_file_rate ON public.chat_files;
CREATE TRIGGER chat_file_rate
    BEFORE INSERT ON public.chat_files
    FOR EACH ROW
    EXECUTE FUNCTION public.chat_file_rate();

CREATE OR REPLACE FUNCTION public.chat_message_rate()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    IF NEW.kind <> 'text' THEN
        RETURN NEW;
    END IF;
    PERFORM pg_advisory_xact_lock(hashtext('chat_message_rate:' || coalesce(NEW.sender_id::TEXT, '')));
    IF (SELECT count(*) FROM public.chat_messages
        WHERE sender_id = NEW.sender_id AND kind = 'text' AND created_at > now() - INTERVAL '1 minute')
        >= (SELECT messages_per_minute FROM public.study_chat_config) THEN
        RAISE EXCEPTION 'too_many_messages' USING ERRCODE = 'check_violation',
            DETAIL = 'A member sends at most 30 messages a minute.';
    END IF;
    RETURN NEW;
END;
$$;

-- After the guard (chat_message_guard), which sets who sent it: triggers run in name order.
DROP TRIGGER IF EXISTS chat_message_rate ON public.chat_messages;
CREATE TRIGGER chat_message_rate
    BEFORE INSERT ON public.chat_messages
    FOR EACH ROW
    EXECUTE FUNCTION public.chat_message_rate();

REVOKE ALL ON FUNCTION public.chat_file_rate(), public.chat_message_rate() FROM public, anon, authenticated;
