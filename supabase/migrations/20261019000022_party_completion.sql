-- Migration: 20261019000022_party_completion.sql
-- Description: Finishing a party (docs/darood-design.md, The milestone and the race).
--   * After each entry, if the party's total has reached its milestone and it hasn't finished yet,
--     completed_at is set to now. The entry that reaches it counts; the guard takes no more after.
--   * The party's row is still locked from the entry's guard, so two entries can't both miss the
--     moment, or both claim it.
--   * completed_at is permanent: once set it never changes or empties, whoever writes
--     ('completion_is_final', 23514). The race board ranks by it.
-- Re-runnable: CREATE OR REPLACE, and the triggers are dropped first.
-- Rollback: DROP TRIGGER IF EXISTS darood_entry_completes ON public.darood_entries;
--   DROP TRIGGER IF EXISTS party_completion_is_final ON public.parties;
--   DROP FUNCTION IF EXISTS public.darood_entry_completes(), public.party_completion_is_final();

CREATE OR REPLACE FUNCTION public.darood_entry_completes()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    party public.parties%ROWTYPE;
    total BIGINT;
BEGIN
    SELECT * INTO party FROM public.parties p WHERE p.id = NEW.party_id;
    IF party.milestone IS NULL OR party.completed_at IS NOT NULL THEN
        RETURN NULL;
    END IF;
    SELECT coalesce(sum(e.amount), 0) INTO total FROM public.darood_entries e WHERE e.party_id = NEW.party_id;
    IF total >= party.milestone THEN
        UPDATE public.parties p SET completed_at = now() WHERE p.id = NEW.party_id;
    END IF;
    RETURN NULL;
END;
$$;

DROP TRIGGER IF EXISTS darood_entry_completes ON public.darood_entries;
CREATE TRIGGER darood_entry_completes
    AFTER INSERT ON public.darood_entries
    FOR EACH ROW
    EXECUTE FUNCTION public.darood_entry_completes();

CREATE OR REPLACE FUNCTION public.party_completion_is_final()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = ''
AS $$
BEGIN
    IF OLD.completed_at IS NOT NULL AND NEW.completed_at IS DISTINCT FROM OLD.completed_at THEN
        RAISE EXCEPTION 'completion_is_final' USING ERRCODE = 'check_violation',
            DETAIL = 'A party''s finish time never changes.';
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS party_completion_is_final ON public.parties;
CREATE TRIGGER party_completion_is_final
    BEFORE UPDATE OF completed_at ON public.parties
    FOR EACH ROW
    EXECUTE FUNCTION public.party_completion_is_final();

REVOKE ALL ON FUNCTION public.darood_entry_completes(), public.party_completion_is_final() FROM public, anon, authenticated;
