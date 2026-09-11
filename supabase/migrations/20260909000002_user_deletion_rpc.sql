-- Migration: 20260909000002_user_deletion_rpc.sql
-- Description: Provides an authenticated RPC function allowing users to completely self-delete their Supabase account

CREATE OR REPLACE FUNCTION public.delete_user_account()
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth
AS $$
DECLARE
    current_user_id UUID;
BEGIN
    current_user_id := auth.uid();
    IF current_user_id IS NULL THEN
        RAISE EXCEPTION 'Not authenticated' USING ERRCODE = 'insufficient_privilege';
    END IF;

    -- 1. Explicitly delete profile row (defense-in-depth before auth user deletion)
    DELETE FROM public.profiles WHERE id = current_user_id;

    -- 2. Delete the user record from auth.users (cascades sessions, refresh tokens, identities)
    DELETE FROM auth.users WHERE id = current_user_id;
END;
$$;

-- Revoke default public execution, permit authenticated users only
REVOKE ALL ON FUNCTION public.delete_user_account() FROM public;
GRANT EXECUTE ON FUNCTION public.delete_user_account() TO authenticated;

COMMENT ON FUNCTION public.delete_user_account() IS 'Self-service account and cloud data deletion RPC executed with security definer privileges.';
