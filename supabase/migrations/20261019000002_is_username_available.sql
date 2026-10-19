-- Migration: 20261019000002_is_username_available.sql
-- Description: is_username_available(name), for the username screen's live check.
--   * True when the name is well-formed, not reserved, and no other account holds it (ignoring
--     case). The caller's own name counts as available: it's theirs.
--   * SECURITY DEFINER, so it can look at the whole table, which players can't read. It answers
--     yes or no only, never whose a name is.
--   * Signed-in players only.
-- Re-runnable: CREATE OR REPLACE.
-- Rollback: DROP FUNCTION IF EXISTS public.is_username_available(text);

CREATE OR REPLACE FUNCTION public.is_username_available(name TEXT)
RETURNS BOOLEAN
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    caller UUID := (SELECT auth.uid());
    wanted TEXT := lower(btrim(coalesce(name, '')));
BEGIN
    IF caller IS NULL THEN
        RAISE EXCEPTION 'Not authenticated' USING ERRCODE = 'insufficient_privilege';
    END IF;
    IF wanted !~ '^[a-z0-9_]{3,20}$' OR public.username_is_reserved(wanted) THEN
        RETURN FALSE;
    END IF;
    RETURN NOT EXISTS (
        SELECT 1 FROM public.usernames u
        WHERE lower(u.username) = wanted AND u.user_id <> caller
    );
END;
$$;

REVOKE ALL ON FUNCTION public.is_username_available(TEXT) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.is_username_available(TEXT) TO authenticated;

COMMENT ON FUNCTION public.is_username_available(TEXT) IS 'Whether a Darood Party username is free for the caller: yes or no, never whose it is.';
