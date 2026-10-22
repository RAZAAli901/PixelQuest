-- Migration: 20261019000114_definer_search_paths.sql
-- Description: S018 of the security plan: every SECURITY DEFINER function sets an empty search_path
-- and names every object with its schema, so nothing a caller puts on their search_path can stand
-- in for a table the function writes. All of them did except delete_user_account (from 2026-09,
-- search_path "public, auth"); it's rewritten here with qualified names, same behaviour: the caller
-- deletes their own profile and auth user, and signed out it refuses. Every app-callable definer
-- function checks its caller inside (auth.uid() directly, or through private.start_party,
-- is_active_study_member and is_party_leader); scripts/security/check_db.sql keeps both rules.
-- Re-runnable: CREATE OR REPLACE.
-- Rollback: re-run 20260909000002_user_deletion_rpc.sql.

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
    -- The profile first (defence in depth), then the auth user, which cascades to the rest.
    DELETE FROM public.profiles WHERE id = current_user_id;
    DELETE FROM auth.users WHERE id = current_user_id;
END;
$$;

REVOKE ALL ON FUNCTION public.delete_user_account() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.delete_user_account() TO authenticated;
