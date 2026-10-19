-- Migration: 20261019000027_transfer_leadership.sql
-- Description: The leader hands the party's leadership to an active member (docs/darood-design.md, Parties).
--   * transfer_leadership(party_id, user_id): the leader only ('not_leader', 42501, HTTP 403), to an
--     active member of the party who isn't the leader already ('not_a_member', 23514, HTTP 400).
--   * The old leader stays in the party as a member, and can then leave it.
--   * The old leader steps down before the new one steps up, so the party never has two leaders
--     (party_members_one_leader), and parties.leader_id follows in the same transaction.
-- Re-runnable: CREATE OR REPLACE.
-- Rollback: DROP FUNCTION IF EXISTS public.transfer_leadership(UUID, UUID);

CREATE OR REPLACE FUNCTION public.transfer_leadership(party_id UUID, user_id UUID)
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
            DETAIL = 'Only the party''s leader can hand the leadership over.';
    END IF;
    SELECT * INTO target FROM public.party_members m
    WHERE m.party_id = party_id AND m.user_id = user_id AND m.status = 'active' AND m.role = 'member'
    FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'not_a_member' USING ERRCODE = 'check_violation',
            DETAIL = 'The leadership goes to an active member of the party.';
    END IF;

    UPDATE public.party_members m SET role = 'member', updated_at = now()
    WHERE m.party_id = party_id AND m.user_id = caller;
    UPDATE public.party_members m SET role = 'leader', updated_at = now() WHERE m.id = target.id;
    UPDATE public.parties p SET leader_id = target.user_id WHERE p.id = party_id;
    RETURN jsonb_build_object('leader_id', target.user_id);
END;
$$;

REVOKE ALL ON FUNCTION public.transfer_leadership(UUID, UUID) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.transfer_leadership(UUID, UUID) TO authenticated;
