-- Migration: 20261019000118_account_deletion_complete.sql
-- Description: S057 of the security plan: deleting an account leaves nothing that names it.
--   Deleting the auth user cascades to, or clears, every table with a key to it (checked in
--   supabase/tests/local/account_deletion.test.ts). Two places named a player without a key, and
--   they are now cleared too:
--     * public.ai_proxy_usage: the account's daily AI call counters ("user:<id>");
--     * private.rate_events: the rate limits' recent events (S020).
--   Kept on purpose, as before: the opponent's head-to-head record (chess_head_to_head, theirs to
--   read; the deleted id no longer leads to any account, name or username), finished games with
--   the deleted side set to null, and Darood counts in their party's total with no name on them.
--   The project-wide AI counter ("*") names nobody. The player's
--   chat files lose their rows, and cleanup-uploads deletes their bytes on its next round (it
--   removes any object no file row names).
-- Re-runnable: CREATE OR REPLACE.
-- Rollback: re-run 20261019000114_definer_search_paths.sql.

CREATE OR REPLACE FUNCTION public.delete_user_account()
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    current_user_id UUID := (SELECT auth.uid());
BEGIN
    IF current_user_id IS NULL THEN
        RAISE EXCEPTION 'Not authenticated' USING ERRCODE = 'insufficient_privilege';
    END IF;
    -- What names the player without a key to them.
    DELETE FROM public.ai_proxy_usage WHERE device_id = 'user:' || lower(current_user_id::text);
    DELETE FROM private.rate_events WHERE subject = current_user_id;
    -- The profile first (defence in depth), then the auth user, which cascades to the rest.
    DELETE FROM public.profiles WHERE id = current_user_id;
    DELETE FROM auth.users WHERE id = current_user_id;
END;
$$;

REVOKE ALL ON FUNCTION public.delete_user_account() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.delete_user_account() TO authenticated;
