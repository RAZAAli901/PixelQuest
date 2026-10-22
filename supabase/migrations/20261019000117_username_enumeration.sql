-- Migration: 20261019000117_username_enumeration.sql
-- Description: S021 of the security plan: usernames can't be harvested.
--   * Invitations already answer the same for a username that exists and one that doesn't
--     (invite_member returns "sent" either way; the inviter learns nothing).
--   * Usernames are seen only by party mates, never listed or searched.
--   * The one place that must say whether a name is taken, the availability check while choosing a
--     username, needs a signed-in account (as before) and now allows 60 checks an hour, enough for
--     typing a name but not for walking a dictionary. It becomes VOLATILE to log the check.
-- Re-runnable: CREATE OR REPLACE.
-- Rollback: re-run 20261019000002_is_username_available.sql (STABLE, no limit).

CREATE OR REPLACE FUNCTION public.is_username_available(name text)
 RETURNS boolean
 LANGUAGE plpgsql
 VOLATILE SECURITY DEFINER
 SET search_path TO ''
AS $function$
DECLARE
    caller UUID := (SELECT auth.uid());
    wanted TEXT := lower(btrim(coalesce(name, '')));
BEGIN
    IF caller IS NULL THEN
        RAISE EXCEPTION 'Not authenticated' USING ERRCODE = 'insufficient_privilege';
    END IF;
    PERFORM private.rate_limit('username_check', 60, interval '1 hour');
    IF wanted !~ '^[a-z0-9_]{3,20}$' OR public.username_is_reserved(wanted) THEN
        RETURN FALSE;
    END IF;
    RETURN NOT EXISTS (
        SELECT 1 FROM public.usernames u
        WHERE lower(u.username) = wanted AND u.user_id <> caller
    );
END;
$function$;
