-- Migration: 20261019000073_goal_reached.sql
-- Description: Reaching a goal (docs/darood-weekly.md, Reaching a goal). The entry that takes the
-- party's total to its current goal marks that goal reached (party_milestones.reached_at), the
-- first time only, and finishes the party as before (completed_at) until the leader extends it.
--   * A goal's reached_at, once set, never changes; nor do its target or number after that.
-- Re-runnable: CREATE OR REPLACE, DROP TRIGGER IF EXISTS.
-- Rollback: re-run 20261019000022_party_completion.sql; DROP FUNCTION private.goal_is_final().

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
        UPDATE public.party_milestones g SET reached_at = now()
        WHERE g.party_id = NEW.party_id AND g.reached_at IS NULL AND g.target <= total;
        UPDATE public.parties p SET completed_at = now() WHERE p.id = NEW.party_id;
    END IF;
    RETURN NULL;
END;
$$;

CREATE OR REPLACE FUNCTION private.goal_is_final()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = ''
AS $$
BEGIN
    IF OLD.reached_at IS NOT NULL AND (
        NEW.reached_at IS DISTINCT FROM OLD.reached_at OR NEW.target <> OLD.target OR NEW.sequence <> OLD.sequence
    ) THEN
        RAISE EXCEPTION 'goal_is_final' USING ERRCODE = 'check_violation',
            DETAIL = 'A reached goal never changes.';
    END IF;
    RETURN NEW;
END;
$$;
REVOKE ALL ON FUNCTION private.goal_is_final() FROM public, anon, authenticated;
REVOKE ALL ON FUNCTION public.darood_entry_completes() FROM public, anon, authenticated;

DROP TRIGGER IF EXISTS goal_is_final ON public.party_milestones;
CREATE TRIGGER goal_is_final
    BEFORE UPDATE ON public.party_milestones
    FOR EACH ROW
    EXECUTE FUNCTION private.goal_is_final();
