-- Migration: 20261019000017_create_party.sql
-- Description: create_party(name, max_members, milestone): a new party and its leader, together.
--   * One transaction: the party and the caller's membership (leader, active) both exist, or
--     neither does.
--   * The caller needs a username first ('username_required', 23514): members are shown, and
--     invited, by username.
--   * The name is trimmed; the table's rules then hold (2-40 characters, 2-30 members, a milestone
--     above 0 if one is given, 23514). max_members is 10 unless given; the milestone may come later.
--   * Returns the new party's id. Signed-in players only.
-- Re-runnable: CREATE OR REPLACE.
-- Rollback: DROP FUNCTION IF EXISTS public.create_party(text, integer, bigint);

CREATE OR REPLACE FUNCTION public.create_party(name TEXT, max_members INTEGER DEFAULT 10, milestone BIGINT DEFAULT NULL)
RETURNS UUID
LANGUAGE plpgsql
VOLATILE
SECURITY DEFINER
SET search_path = ''
AS $$
#variable_conflict use_variable
DECLARE
    caller UUID := (SELECT auth.uid());
    new_party UUID;
BEGIN
    IF caller IS NULL THEN
        RAISE EXCEPTION 'Not authenticated' USING ERRCODE = 'insufficient_privilege';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM public.usernames u WHERE u.user_id = caller) THEN
        RAISE EXCEPTION 'username_required' USING ERRCODE = 'check_violation',
            DETAIL = 'Choose a username before starting a party.';
    END IF;

    INSERT INTO public.parties (name, leader_id, max_members, milestone, milestone_set_at)
    VALUES (btrim(coalesce(name, '')), caller, coalesce(max_members, 10), milestone,
            CASE WHEN milestone IS NULL THEN NULL ELSE now() END)
    RETURNING id INTO new_party;

    INSERT INTO public.party_members (party_id, user_id, role, status, invited_by, joined_at)
    VALUES (new_party, caller, 'leader', 'active', caller, now());

    RETURN new_party;
END;
$$;

REVOKE ALL ON FUNCTION public.create_party(TEXT, INTEGER, BIGINT) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.create_party(TEXT, INTEGER, BIGINT) TO authenticated;

COMMENT ON FUNCTION public.create_party(TEXT, INTEGER, BIGINT) IS 'Creates a Darood Party led by the caller, who must have a username. Returns its id.';
