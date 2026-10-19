-- Migration: 20261019000011_entry_guard.sql
-- Description: Who may post an entry, and where (docs/darood-design.md, Entries).
--   * Only an active member of the party: not someone invited, declined, left or removed
--     ('not_a_member', 42501, HTTP 403).
--   * Only into a party that's neither finished (completed_at) nor deleted ('party_closed',
--     23514, HTTP 400).
--   * created_at is always the server's time.
--   * The party's row is locked while this runs, so a party's entries are checked one at a time.
--     The daily cap and the milestone (later migrations) rely on that.
-- Re-runnable: CREATE OR REPLACE, and the trigger is dropped first.
-- Rollback: DROP TRIGGER IF EXISTS darood_entry_guard ON public.darood_entries;
--   DROP FUNCTION IF EXISTS public.darood_entry_guard();

CREATE OR REPLACE FUNCTION public.darood_entry_guard()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    party public.parties%ROWTYPE;
BEGIN
    SELECT * INTO party FROM public.parties p WHERE p.id = NEW.party_id FOR UPDATE;
    IF NOT FOUND OR party.deleted_at IS NOT NULL OR party.completed_at IS NOT NULL THEN
        RAISE EXCEPTION 'party_closed' USING ERRCODE = 'check_violation',
            DETAIL = 'This party is finished or deleted, so it takes no more entries.';
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM public.party_members m
        WHERE m.party_id = NEW.party_id AND m.user_id = NEW.user_id AND m.status = 'active'
    ) THEN
        RAISE EXCEPTION 'not_a_member' USING ERRCODE = 'insufficient_privilege',
            DETAIL = 'Only the party''s active members can post.';
    END IF;
    NEW.created_at := now();
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS darood_entry_guard ON public.darood_entries;
CREATE TRIGGER darood_entry_guard
    BEFORE INSERT ON public.darood_entries
    FOR EACH ROW
    EXECUTE FUNCTION public.darood_entry_guard();

REVOKE ALL ON FUNCTION public.darood_entry_guard() FROM public, anon, authenticated;
