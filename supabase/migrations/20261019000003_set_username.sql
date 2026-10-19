-- Migration: 20261019000003_set_username.sql
-- Description: set_username(name), the only way a username is written.
--   * Signed-in players only, for their own account.
--   * The same rules as the table (form, reserved words, unique ignoring case), answered as a
--     status instead of an error, so the app can say what happened.
--   * Once set, a username can change again 30 days later. Setting the same name again is fine.
--   * Returns {"status": "ok" | "invalid" | "taken" | "too_soon", "username", "changeable_at"}:
--     the name now held and when it may change next ("taken" and "invalid" carry neither).
-- Re-runnable: CREATE OR REPLACE.
-- Rollback: DROP FUNCTION IF EXISTS public.set_username(text);

CREATE OR REPLACE FUNCTION public.set_username(name TEXT)
RETURNS JSONB
LANGUAGE plpgsql
VOLATILE
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    caller UUID := (SELECT auth.uid());
    wanted TEXT := lower(btrim(coalesce(name, '')));
    wait CONSTANT INTERVAL := INTERVAL '30 days';
    held public.usernames%ROWTYPE;
BEGIN
    IF caller IS NULL THEN
        RAISE EXCEPTION 'Not authenticated' USING ERRCODE = 'insufficient_privilege';
    END IF;
    IF wanted !~ '^[a-z0-9_]{3,20}$' OR public.username_is_reserved(wanted) THEN
        RETURN jsonb_build_object('status', 'invalid');
    END IF;

    -- Locked, so two changes sent at once can't both slip under the 30 days.
    SELECT * INTO held FROM public.usernames WHERE user_id = caller FOR UPDATE;
    IF FOUND THEN
        IF held.username = wanted THEN
            RETURN jsonb_build_object('status', 'ok', 'username', held.username, 'changeable_at', held.changed_at + wait);
        END IF;
        IF held.changed_at + wait > now() THEN
            RETURN jsonb_build_object('status', 'too_soon', 'username', held.username, 'changeable_at', held.changed_at + wait);
        END IF;
    END IF;

    BEGIN
        INSERT INTO public.usernames (user_id, username, changed_at)
        VALUES (caller, wanted, now())
        ON CONFLICT (user_id) DO UPDATE SET username = EXCLUDED.username, changed_at = EXCLUDED.changed_at;
    EXCEPTION WHEN unique_violation THEN
        RETURN jsonb_build_object('status', 'taken');
    END;
    RETURN jsonb_build_object('status', 'ok', 'username', wanted, 'changeable_at', now() + wait);
END;
$$;

REVOKE ALL ON FUNCTION public.set_username(TEXT) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.set_username(TEXT) TO authenticated;

COMMENT ON FUNCTION public.set_username(TEXT) IS 'Sets the caller''s Darood Party username: valid, not reserved, not taken, and at most once every 30 days.';
