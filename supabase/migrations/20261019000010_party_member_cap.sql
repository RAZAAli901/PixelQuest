-- Migration: 20261019000010_party_member_cap.sql
-- Description: A party never holds more people than its limit (docs/darood-design.md, Parties).
--   * Invited and active members count together: an invitation holds a place until it's answered.
--   * The limit is the party's max_members, and never above 30 even if a row somehow had more.
--   * The party's row is locked while counting, so two invitations sent at once can't both take
--     the last place.
--   * Over the limit: 'party_full' (23514, HTTP 400). The 31st person can't join.
-- Re-runnable: CREATE OR REPLACE, and the trigger is dropped first.
-- Rollback: DROP TRIGGER IF EXISTS party_member_cap ON public.party_members;
--   DROP FUNCTION IF EXISTS public.party_member_cap();

CREATE OR REPLACE FUNCTION public.party_member_cap()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    limit_now INTEGER;
    taken INTEGER;
BEGIN
    -- Only a row taking a place: a new invitation or membership, or a declined, left or removed
    -- one coming back.
    IF NEW.status NOT IN ('invited', 'active') THEN
        RETURN NEW;
    END IF;
    IF TG_OP = 'UPDATE' AND OLD.status IN ('invited', 'active') THEN
        RETURN NEW;
    END IF;

    SELECT LEAST(p.max_members, 30) INTO limit_now
    FROM public.parties p
    WHERE p.id = NEW.party_id
    FOR UPDATE;

    SELECT count(*) INTO taken
    FROM public.party_members m
    WHERE m.party_id = NEW.party_id
      AND m.status IN ('invited', 'active')
      AND m.id IS DISTINCT FROM NEW.id;

    IF taken + 1 > limit_now THEN
        RAISE EXCEPTION 'party_full' USING ERRCODE = 'check_violation',
            DETAIL = format('This party holds %s people, invitations included.', limit_now);
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS party_member_cap ON public.party_members;
CREATE TRIGGER party_member_cap
    BEFORE INSERT OR UPDATE OF status ON public.party_members
    FOR EACH ROW
    EXECUTE FUNCTION public.party_member_cap();

REVOKE ALL ON FUNCTION public.party_member_cap() FROM public, anon, authenticated;
