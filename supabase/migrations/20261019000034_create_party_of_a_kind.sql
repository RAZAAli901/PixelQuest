-- Migration: 20261019000034_create_party_of_a_kind.sql
-- Description: Starting a party of either kind (docs/party-core.md).
--   * private.start_party: the one way a party starts. The party and its leader's row together, led
--     by the caller, who must have a username (username_required); its kind must be darood or study
--     (unknown_party_type); a milestone only for a Darood Party (the table's darood_only_columns).
--   * create_party(name, max_members, party_type): what the app calls now, for either kind, with no
--     milestone. party_type has no default, so a call without it reaches the old form below.
--   * create_party(name, max_members, milestone): the old form, kept for the 1.4 apps already
--     installed. It starts a Darood Party exactly as before.
--   * PostgREST tells the two apart by their argument names (party_type or milestone).
-- Re-runnable: CREATE OR REPLACE.
-- Rollback: DROP FUNCTION IF EXISTS public.create_party(TEXT, INTEGER, TEXT);
--   DROP FUNCTION IF EXISTS private.start_party(TEXT, INTEGER, TEXT, BIGINT); then re-run
--   20261019000017 for the old form's own body.

CREATE OR REPLACE FUNCTION private.start_party(name TEXT, max_members INTEGER, party_type TEXT, milestone BIGINT)
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
    IF party_type IS NULL OR party_type NOT IN ('darood', 'study') THEN
        RAISE EXCEPTION 'unknown_party_type' USING ERRCODE = 'check_violation',
            DETAIL = 'A party is a darood or a study party.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM public.usernames u WHERE u.user_id = caller) THEN
        RAISE EXCEPTION 'username_required' USING ERRCODE = 'check_violation',
            DETAIL = 'Choose a username before starting a party.';
    END IF;

    INSERT INTO public.parties (name, leader_id, max_members, party_type, milestone, milestone_set_at)
    VALUES (btrim(coalesce(name, '')), caller, coalesce(max_members, 10), party_type, milestone,
            CASE WHEN milestone IS NULL THEN NULL ELSE now() END)
    RETURNING id INTO new_party;

    INSERT INTO public.party_members (party_id, user_id, role, status, invited_by, joined_at)
    VALUES (new_party, caller, 'leader', 'active', caller, now());

    RETURN new_party;
END;
$$;

REVOKE ALL ON FUNCTION private.start_party(TEXT, INTEGER, TEXT, BIGINT) FROM public, anon, authenticated;

CREATE OR REPLACE FUNCTION public.create_party(name TEXT, max_members INTEGER, party_type TEXT)
RETURNS UUID
LANGUAGE sql
VOLATILE
SECURITY DEFINER
SET search_path = ''
AS $$
    SELECT private.start_party(name, max_members, party_type, NULL::BIGINT);
$$;

CREATE OR REPLACE FUNCTION public.create_party(name TEXT, max_members INTEGER DEFAULT 10, milestone BIGINT DEFAULT NULL)
RETURNS UUID
LANGUAGE sql
VOLATILE
SECURITY DEFINER
SET search_path = ''
AS $$
    SELECT private.start_party(name, max_members, 'darood'::TEXT, milestone);
$$;

REVOKE ALL ON FUNCTION public.create_party(TEXT, INTEGER, TEXT), public.create_party(TEXT, INTEGER, BIGINT) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.create_party(TEXT, INTEGER, TEXT), public.create_party(TEXT, INTEGER, BIGINT) TO authenticated;

COMMENT ON FUNCTION public.create_party(TEXT, INTEGER, TEXT) IS
    'Starts a party of a kind (darood or study) led by the caller, who must have a username. Returns its id.';
COMMENT ON FUNCTION public.create_party(TEXT, INTEGER, BIGINT) IS
    'The 1.4 form: starts a Darood Party, with an optional milestone, led by the caller. Returns its id.';
