-- Migration: 20261019000018_invite_member.sql
-- Description: invite_member(party_id, username): the leader invites someone by username.
--   * Leader only ('not_leader', 42501), into a party that's neither finished nor deleted
--     ('party_closed', 23514).
--   * The party must have room for one more, counting invitations ('party_full', 23514). A full
--     party answers that for every name not already in it, whether or not the name exists.
--   * The answer is neutral: {"status": "sent"} whether or not the username exists. For an unknown
--     name nothing is stored and nobody is told. Only "already_member" differs, for someone already
--     in the party, whom the leader can see anyway.
--   * Someone who declined, left or was removed can be invited again: their row goes back to
--     'invited'. Inviting someone already invited changes nothing.
-- Re-runnable: CREATE OR REPLACE.
-- Rollback: DROP FUNCTION IF EXISTS public.invite_member(uuid, text);

CREATE OR REPLACE FUNCTION public.invite_member(party_id UUID, username TEXT)
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
    taken INTEGER;
    invitee UUID;
    existing public.party_members%ROWTYPE;
BEGIN
    IF caller IS NULL THEN
        RAISE EXCEPTION 'Not authenticated' USING ERRCODE = 'insufficient_privilege';
    END IF;

    SELECT * INTO party FROM public.parties p WHERE p.id = party_id FOR UPDATE;
    IF NOT FOUND OR party.deleted_at IS NOT NULL OR party.leader_id IS DISTINCT FROM caller THEN
        RAISE EXCEPTION 'not_leader' USING ERRCODE = 'insufficient_privilege',
            DETAIL = 'Only the party''s leader can invite.';
    END IF;
    IF party.completed_at IS NOT NULL THEN
        RAISE EXCEPTION 'party_closed' USING ERRCODE = 'check_violation',
            DETAIL = 'This party is finished.';
    END IF;

    SELECT u.user_id INTO invitee FROM public.usernames u
    WHERE lower(u.username) = lower(btrim(coalesce(username, '')));

    -- Someone already in the party (the leader sees them in its member list anyway).
    IF invitee IS NOT NULL THEN
        SELECT * INTO existing FROM public.party_members m WHERE m.party_id = party_id AND m.user_id = invitee;
        IF FOUND AND existing.status = 'active' THEN
            RETURN jsonb_build_object('status', 'already_member');
        END IF;
        IF FOUND AND existing.status = 'invited' THEN
            RETURN jsonb_build_object('status', 'sent');
        END IF;
    END IF;

    -- Everyone else needs a free place, whether or not the name exists, so a full party answers
    -- the same for both.
    SELECT count(*) INTO taken FROM public.party_members m
    WHERE m.party_id = party_id AND m.status IN ('invited', 'active');
    IF taken >= LEAST(party.max_members, 30) THEN
        RAISE EXCEPTION 'party_full' USING ERRCODE = 'check_violation',
            DETAIL = format('This party holds %s people, invitations included.', LEAST(party.max_members, 30));
    END IF;

    IF invitee IS NULL THEN
        RETURN jsonb_build_object('status', 'sent');
    END IF;
    IF existing.id IS NOT NULL THEN
        -- Declined, left or removed: invited again.
        UPDATE public.party_members m
        SET status = 'invited', invited_by = caller, invited_at = now(), joined_at = NULL, updated_at = now()
        WHERE m.id = existing.id;
    ELSE
        INSERT INTO public.party_members (party_id, user_id, role, status, invited_by)
        VALUES (party_id, invitee, 'member', 'invited', caller);
    END IF;
    RETURN jsonb_build_object('status', 'sent');
END;
$$;

REVOKE ALL ON FUNCTION public.invite_member(UUID, TEXT) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.invite_member(UUID, TEXT) TO authenticated;

COMMENT ON FUNCTION public.invite_member(UUID, TEXT) IS 'The leader invites a username into a Darood Party; the answer never says whether the name exists.';
