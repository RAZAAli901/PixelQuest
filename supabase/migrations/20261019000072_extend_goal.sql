-- Migration: 20261019000072_extend_goal.sql
-- Description: Goals that go on (docs/darood-weekly.md, Extending). set_milestone, still the
-- leader's alone, now:
--   * with no goal yet: adds goal 1;
--   * while the current goal isn't reached: changes its target (as before);
--   * once it's reached (the party finished it): adds the next goal, and the party takes entries
--     again. Its finish time stays in the goal's reached_at; parties.completed_at clears.
--   * Every target must be above the party's total now (milestone_too_low), and at most
--     1,000,000,000,000 (milestone_too_high).
-- The rules live in the parties trigger, so no other way round them changes a goal.
-- Re-runnable: CREATE OR REPLACE.
-- Rollback: re-run 20261019000020_set_milestone.sql.

CREATE OR REPLACE FUNCTION public.party_milestone_rules()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    total BIGINT;
    current public.party_milestones%ROWTYPE;
    setter UUID := coalesce((SELECT auth.uid()), NEW.leader_id);
BEGIN
    IF NEW.milestone IS NOT DISTINCT FROM OLD.milestone THEN
        RETURN NEW;
    END IF;
    SELECT coalesce(sum(e.amount), 0) INTO total FROM public.darood_entries e WHERE e.party_id = NEW.id;
    IF NEW.milestone IS NULL OR NEW.milestone <= total THEN
        RAISE EXCEPTION 'milestone_too_low' USING ERRCODE = 'check_violation',
            DETAIL = format('The goal must be above the party''s total, %s.', total);
    END IF;
    IF NEW.milestone > 1000000000000 THEN
        RAISE EXCEPTION 'milestone_too_high' USING ERRCODE = 'check_violation',
            DETAIL = 'A goal can be at most 1,000,000,000,000.';
    END IF;

    SELECT * INTO current FROM public.party_milestones g WHERE g.party_id = NEW.id ORDER BY g.sequence DESC LIMIT 1;
    IF NOT FOUND THEN
        INSERT INTO public.party_milestones (party_id, sequence, target, set_by) VALUES (NEW.id, 1, NEW.milestone, setter);
    ELSIF current.reached_at IS NULL AND OLD.completed_at IS NULL THEN
        UPDATE public.party_milestones g SET target = NEW.milestone, set_by = setter, set_at = now() WHERE g.id = current.id;
    ELSE
        -- Reached: kept as it was, with its finish time, and the next goal starts.
        UPDATE public.party_milestones g SET reached_at = coalesce(g.reached_at, OLD.completed_at, now()) WHERE g.id = current.id;
        INSERT INTO public.party_milestones (party_id, sequence, target, set_by)
        VALUES (NEW.id, current.sequence + 1, NEW.milestone, setter);
        NEW.completed_at := NULL;
    END IF;
    NEW.milestone_set_at := now();
    RETURN NEW;
END;
$$;

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
    goal INTEGER;
BEGIN
    IF caller IS NULL THEN
        RAISE EXCEPTION 'Not authenticated' USING ERRCODE = 'insufficient_privilege';
    END IF;
    SELECT * INTO party FROM public.parties p WHERE p.id = party_id FOR UPDATE;
    IF NOT FOUND OR party.deleted_at IS NOT NULL OR party.leader_id IS DISTINCT FROM caller THEN
        RAISE EXCEPTION 'not_leader' USING ERRCODE = 'insufficient_privilege',
            DETAIL = 'Only the party''s leader can set its goal.';
    END IF;
    UPDATE public.parties p SET milestone = target WHERE p.id = party_id
    RETURNING * INTO party;
    SELECT max(g.sequence) INTO goal FROM public.party_milestones g WHERE g.party_id = party.id;
    RETURN jsonb_build_object('milestone', party.milestone, 'milestone_set_at', party.milestone_set_at, 'goal', goal);
END;
$$;

REVOKE ALL ON FUNCTION public.party_milestone_rules() FROM public, anon, authenticated;
REVOKE ALL ON FUNCTION public.set_milestone(UUID, BIGINT) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.set_milestone(UUID, BIGINT) TO authenticated;
