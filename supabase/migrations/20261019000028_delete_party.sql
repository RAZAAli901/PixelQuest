-- Migration: 20261019000028_delete_party.sql
-- Description: The leader deletes the party (docs/darood-design.md, Parties).
--   * delete_party(party_id): the leader only ('not_leader', 42501, HTTP 403). It sets
--     parties.deleted_at, and every read policy, the membership helpers and the party functions
--     already treat a deleted party as gone: nobody reads it, its members or entries any more, nobody
--     posts to it, and its invitations can't be answered.
--   * Its rows stay (entries are never deleted), hidden; deleting again answers the same.
--   * Typing the party's name to confirm is the app's safeguard against a slip; the server needs
--     only the leader.
-- Re-runnable: CREATE OR REPLACE.
-- Rollback: DROP FUNCTION IF EXISTS public.delete_party(UUID);

CREATE OR REPLACE FUNCTION public.delete_party(party_id UUID)
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
    IF NOT FOUND OR party.leader_id IS DISTINCT FROM caller THEN
        RAISE EXCEPTION 'not_leader' USING ERRCODE = 'insufficient_privilege',
            DETAIL = 'Only the party''s leader can delete it.';
    END IF;
    -- Already deleted (an answer lost on the way back, say): the same answer again.
    IF party.deleted_at IS NULL THEN
        UPDATE public.parties p SET deleted_at = now() WHERE p.id = party_id;
    END IF;
    RETURN jsonb_build_object('status', 'deleted');
END;
$$;

REVOKE ALL ON FUNCTION public.delete_party(UUID) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.delete_party(UUID) TO authenticated;
