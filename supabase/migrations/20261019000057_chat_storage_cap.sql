-- Migration: 20261019000057_chat_storage_cap.sql
-- Description: Each study party's share of study-files, capped at 500 MB (docs/study-chat.md, Files).
--   * study_chat_storage: per party, the bytes and files it holds: ready ones and those still
--     being uploaded or checked (pending), not deleted or rejected. With the reader's rights, so a
--     member sees their own party's (its ready files, and their own pending ones).
--   * chat_file_cap: before a file row is made (request-upload) or changes size (finalize-upload
--     records the real size), the party's total with it must stay within 500 MB:
--     party_storage_full otherwise. Counted with the definer's rights, over every file.
-- Re-runnable: CREATE OR REPLACE, DROP TRIGGER IF EXISTS.
-- Rollback: DROP TRIGGER chat_file_cap ON public.chat_files; DROP FUNCTION public.chat_file_cap();
--   DROP VIEW public.study_chat_storage.

CREATE OR REPLACE VIEW public.study_chat_storage
WITH (security_invoker = true)
AS
SELECT party_id, sum(size_bytes)::BIGINT AS used_bytes, count(*)::INTEGER AS files
FROM public.chat_files
WHERE deleted_at IS NULL AND status IN ('pending', 'ready')
GROUP BY party_id;

GRANT SELECT ON public.study_chat_storage TO authenticated;
REVOKE ALL ON public.study_chat_storage FROM anon;

CREATE OR REPLACE FUNCTION public.chat_file_cap()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    used BIGINT;
BEGIN
    IF NEW.deleted_at IS NOT NULL OR NEW.status = 'rejected' THEN
        RETURN NEW;
    END IF;
    -- One party's uploads at a time, so two can't both slip under the cap.
    PERFORM pg_advisory_xact_lock(hashtext('chat_file_cap:' || NEW.party_id::TEXT));
    SELECT coalesce(sum(size_bytes), 0) INTO used
    FROM public.chat_files
    WHERE party_id = NEW.party_id AND id <> NEW.id AND deleted_at IS NULL AND status IN ('pending', 'ready');
    IF used + NEW.size_bytes > (SELECT party_max_bytes FROM public.study_chat_config) THEN
        RAISE EXCEPTION 'party_storage_full' USING ERRCODE = 'check_violation',
            DETAIL = 'A study party holds at most 500 MB of files.';
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS chat_file_cap ON public.chat_files;
CREATE TRIGGER chat_file_cap
    BEFORE INSERT OR UPDATE OF size_bytes, status, deleted_at ON public.chat_files
    FOR EACH ROW
    EXECUTE FUNCTION public.chat_file_cap();

REVOKE ALL ON FUNCTION public.chat_file_cap() FROM public, anon, authenticated;
