-- Migration: 20261019000019_membership_functions.sql
-- Description: Joining, leaving and removing (docs/darood-design.md, Parties). party_members has no
-- client write grant, so these are the only ways a membership changes after an invitation.
--   * respond_invite(party_id, accept): the invited person accepts (active) or declines. Only
--     someone with an unanswered invitation ('no_invitation', 23514), and accepting a finished
--     party is refused ('party_closed', 23514).
--   * leave_party(party_id): an active member leaves. The leader can't, until they've handed the
--     leadership over ('leader_must_hand_over', 23514); they can also delete the party.
--   * remove_member(party_id, user_id): the leader removes an active member or withdraws an
--     invitation ('not_leader', 42501; 'not_a_member', 23514). The leader can't remove themselves.
--   * Who left or was removed keeps their row, and their entries stay in the total.
--   * When a leader's account is deleted, leadership passes to the active member who joined first,
--     so the party isn't left without one. With nobody left, it stays leaderless.
-- Re-runnable: CREATE OR REPLACE, and the trigger is dropped first.
-- Rollback: DROP FUNCTION IF EXISTS public.respond_invite(uuid, boolean), public.leave_party(uuid),
--   public.remove_member(uuid, uuid); DROP TRIGGER IF EXISTS party_leader_gone ON public.party_members;
--   DROP FUNCTION IF EXISTS public.party_leader_gone();

CREATE OR REPLACE FUNCTION public.respond_invite(party_id UUID, accept BOOLEAN)
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
    invitation public.party_members%ROWTYPE;
BEGIN
    IF caller IS NULL THEN
        RAISE EXCEPTION 'Not authenticated' USING ERRCODE = 'insufficient_privilege';
    END IF;
    SELECT * INTO party FROM public.parties p WHERE p.id = party_id FOR UPDATE;
    SELECT * INTO invitation FROM public.party_members m
    WHERE m.party_id = party_id AND m.user_id = caller AND m.status = 'invited';
    IF party.id IS NULL OR party.deleted_at IS NOT NULL OR invitation.id IS NULL THEN
        RAISE EXCEPTION 'no_invitation' USING ERRCODE = 'check_violation',
            DETAIL = 'There''s no invitation to answer.';
    END IF;

    IF accept THEN
        IF party.completed_at IS NOT NULL THEN
            RAISE EXCEPTION 'party_closed' USING ERRCODE = 'check_violation',
                DETAIL = 'This party is finished.';
        END IF;
        UPDATE public.party_members m SET status = 'active', joined_at = now(), updated_at = now()
        WHERE m.id = invitation.id;
        RETURN jsonb_build_object('status', 'active');
    END IF;
    UPDATE public.party_members m SET status = 'declined', updated_at = now()
    WHERE m.id = invitation.id;
    RETURN jsonb_build_object('status', 'declined');
END;
$$;

CREATE OR REPLACE FUNCTION public.leave_party(party_id UUID)
RETURNS JSONB
LANGUAGE plpgsql
VOLATILE
SECURITY DEFINER
SET search_path = ''
AS $$
#variable_conflict use_variable
DECLARE
    caller UUID := (SELECT auth.uid());
    membership public.party_members%ROWTYPE;
BEGIN
    IF caller IS NULL THEN
        RAISE EXCEPTION 'Not authenticated' USING ERRCODE = 'insufficient_privilege';
    END IF;
    SELECT * INTO membership FROM public.party_members m
    WHERE m.party_id = party_id AND m.user_id = caller AND m.status = 'active'
    FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'not_a_member' USING ERRCODE = 'check_violation',
            DETAIL = 'You''re not in this party.';
    END IF;
    IF membership.role = 'leader' THEN
        RAISE EXCEPTION 'leader_must_hand_over' USING ERRCODE = 'check_violation',
            DETAIL = 'Hand the leadership to another member first, or delete the party.';
    END IF;
    UPDATE public.party_members m SET status = 'left', updated_at = now() WHERE m.id = membership.id;
    RETURN jsonb_build_object('status', 'left');
END;
$$;

CREATE OR REPLACE FUNCTION public.remove_member(party_id UUID, user_id UUID)
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
    target public.party_members%ROWTYPE;
BEGIN
    IF caller IS NULL THEN
        RAISE EXCEPTION 'Not authenticated' USING ERRCODE = 'insufficient_privilege';
    END IF;
    SELECT * INTO party FROM public.parties p WHERE p.id = party_id FOR UPDATE;
    IF NOT FOUND OR party.deleted_at IS NOT NULL OR party.leader_id IS DISTINCT FROM caller THEN
        RAISE EXCEPTION 'not_leader' USING ERRCODE = 'insufficient_privilege',
            DETAIL = 'Only the party''s leader can remove members.';
    END IF;
    SELECT * INTO target FROM public.party_members m
    WHERE m.party_id = party_id AND m.user_id = user_id AND m.status IN ('active', 'invited') AND m.role = 'member';
    IF NOT FOUND THEN
        RAISE EXCEPTION 'not_a_member' USING ERRCODE = 'check_violation',
            DETAIL = 'That person isn''t in this party.';
    END IF;
    UPDATE public.party_members m SET status = 'removed', updated_at = now() WHERE m.id = target.id;
    RETURN jsonb_build_object('status', 'removed');
END;
$$;

-- A leader's membership went with their account: the longest-standing active member leads now.
CREATE OR REPLACE FUNCTION public.party_leader_gone()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    successor public.party_members%ROWTYPE;
BEGIN
    IF OLD.role <> 'leader' THEN
        RETURN OLD;
    END IF;
    SELECT * INTO successor FROM public.party_members m
    WHERE m.party_id = OLD.party_id AND m.status = 'active' AND m.id <> OLD.id
    ORDER BY m.joined_at NULLS LAST, m.id
    LIMIT 1;
    IF FOUND THEN
        UPDATE public.party_members m SET role = 'leader', updated_at = now() WHERE m.id = successor.id;
        UPDATE public.parties p SET leader_id = successor.user_id WHERE p.id = OLD.party_id;
    END IF;
    RETURN OLD;
END;
$$;

DROP TRIGGER IF EXISTS party_leader_gone ON public.party_members;
CREATE TRIGGER party_leader_gone
    AFTER DELETE ON public.party_members
    FOR EACH ROW
    EXECUTE FUNCTION public.party_leader_gone();

REVOKE ALL ON FUNCTION public.respond_invite(UUID, BOOLEAN), public.leave_party(UUID), public.remove_member(UUID, UUID) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.respond_invite(UUID, BOOLEAN), public.leave_party(UUID), public.remove_member(UUID, UUID) TO authenticated;
REVOKE ALL ON FUNCTION public.party_leader_gone() FROM public, anon, authenticated;
