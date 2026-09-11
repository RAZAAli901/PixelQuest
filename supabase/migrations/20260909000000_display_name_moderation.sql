-- Migration: 20260909000000_display_name_moderation.sql
-- Description: Enforces server-side moderation and character validation on display_name in public.profiles

CREATE OR REPLACE FUNCTION public.check_display_name_moderation()
RETURNS TRIGGER AS $$
DECLARE
    cleaned_name TEXT;
    offensive_patterns TEXT[] := ARRAY[
        'asshole', 'bastard', 'bitch', 'blowjob', 'crap',
        'cunt', 'damn', 'dick', 'dildo', 'douche',
        'fag', 'faggot', 'fuck', 'homo', 'nazi',
        'nigga', 'nigger', 'penis', 'piss', 'prick',
        'pussy', 'retard', 'shit', 'slut', 'tit',
        'twat', 'vagina', 'whore'
    ];
    pattern TEXT;
BEGIN
    -- Only validate if display_name is not empty (empty default allowed for newly linked inactive profiles)
    IF NEW.display_name IS NOT NULL AND NEW.display_name <> '' THEN
        -- 1. Length & character structure constraint
        IF NOT (NEW.display_name ~* '^[a-zA-Z0-9_]{3,20}$') THEN
            RAISE EXCEPTION 'Display name must be 3-20 characters consisting only of letters, numbers, and underscores.'
                USING ERRCODE = 'check_violation';
        END IF;

        -- 2. Profanity and offensive term filter (lowercase matching)
        cleaned_name := lower(NEW.display_name);
        FOREACH pattern IN ARRAY offensive_patterns LOOP
            IF position(pattern IN cleaned_name) > 0 THEN
                RAISE EXCEPTION 'Display name contains disallowed or offensive terminology.'
                    USING ERRCODE = 'check_violation';
            END IF;
        END LOOP;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_check_display_name_moderation ON public.profiles;
CREATE TRIGGER trigger_check_display_name_moderation
    BEFORE INSERT OR UPDATE OF display_name ON public.profiles
    FOR EACH ROW
    EXECUTE FUNCTION public.check_display_name_moderation();

COMMENT ON FUNCTION public.check_display_name_moderation() IS 'Server-side defense-in-depth trigger rejecting profanity and malformed display names.';
