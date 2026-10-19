-- Migration: 20261019000020_set_milestone.sql
-- Description: The milestone (docs/darood-design.md, The milestone and the race).
--   * set_milestone(party_id, target): the leader sets or raises the party's target total
--     ('not_leader', 42501). Returns {"milestone", "milestone_set_at"}.
--   * Whichever way it changes (this function, or the leader's plain update the policy allows), a
--     trigger keeps the rules:
--       - above the party's current total ('milestone_too_low', 23514), so it's never reached the
--         moment it's set, and never taken away once set;
--       - not once the party has finished ('party_closed', 23514): a finish is final;
--       - milestone_set_at is the server's time of the change.
--   * The update waits for the party's row, so an entry being posted is counted first.
-- Re-runnable: CREATE OR REPLACE, and the trigger is dropped first.
-- Rollback: DROP FUNCTION IF EXISTS public.set_milestone(uuid, bigint);
--   DROP TRIGGER IF EXISTS party_milestone_rules ON public.parties; DROP FUNCTION IF EXISTS public.party_milestone_rules();

CREATE OR REPLACE FUNCTION public.party_milestone_rules()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    total BIGINT;
BEGIN
    IF NEW.milestone IS NOT DISTINCT FROM OLD.milestone THEN
        RETURN NEW;
    END IF;
    IF OLD.completed_at IS NOT NULL THEN
        RAISE EXCEPTION 'party_closed' USING ERRCODE = 'check_violation',
            DETAIL = 'This party is finished, so its milestone stays.';
    END IF;
    SELECT coalesce(sum(e.amount), 0) INTO total FROM public.darood_entries e WHERE e.party_id = NEW.id;
    IF NEW.milestone IS NULL OR NEW.milestone <= total THEN
        RAISE EXCEPTION 'milestone_too_low' USING ERRCODE = 'check_violation',
            DETAIL = format('The milestone must be above the party''s total, %s.', total);
    END IF;
    NEW.milestone_set_at := now();
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS party_milestone_rules ON public.parties;
CREATE TRIGGER party_milestone_rules
    BEFORE UPDATE OF milestone ON public.parties
    FOR EACH ROW
    EXECUTE FUNCTION public.party_milestone_rules();

CREATE OR REPLACE FUNCTION public.set_milestone(party_id UUID, target BIGINT)
RETURNS JSONB
LANGUAGE plpgsql
VOLATILE
SECURITY DEFINER
SET search_path = ''
AS $$
#variable_conflict use_variable
DECLARE
    caller UUID := (SELECT auth.uid());
    party public.parties%ROWTYPE;
BEGIN
    IF caller IS NULL THEN
        RAISE EXCEPTION 'Not authenticated' USING ERRCODE = 'insufficient_privilege';
    END IF;
    SELECT * INTO party FROM public.parties p WHERE p.id = party_id FOR UPDATE;
    IF NOT FOUND OR party.deleted_at IS NOT NULL OR party.leader_id IS DISTINCT FROM caller THEN
        RAISE EXCEPTION 'not_leader' USING ERRCODE = 'insufficient_privilege',
            DETAIL = 'Only the party''s leader can set its milestone.';
    END IF;
    UPDATE public.parties p SET milestone = target WHERE p.id = party_id
    RETURNING * INTO party;
    RETURN jsonb_build_object('milestone', party.milestone, 'milestone_set_at', party.milestone_set_at);
END;
$$;

REVOKE ALL ON FUNCTION public.party_milestone_rules() FROM public, anon, authenticated;
REVOKE ALL ON FUNCTION public.set_milestone(UUID, BIGINT) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.set_milestone(UUID, BIGINT) TO authenticated;
