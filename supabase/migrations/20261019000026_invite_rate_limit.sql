-- Migration: 20261019000026_invite_rate_limit.sql
-- Description: At most 20 invitations an hour per account (docs/darood-design.md, Parties).
--   * private.invite_log records each invitation the leader sends, whatever the name: a name nobody
--     has counts too (its answer stays neutral), so the limit can't be dodged with made-up names, and
--     names can't be tried by the hundred.
--   * invite_member checks it after the leader check: the 21st within an hour is refused with
--     'too_many_invites' (23514, HTTP 400). Only a day of the log is kept.
--   * The log is in the private schema, which the API doesn't expose; only the function writes it.
-- Re-runnable: IF NOT EXISTS, CREATE OR REPLACE.
-- Rollback: re-run 20261019000018_invite_member.sql; DROP TABLE IF EXISTS private.invite_log;

CREATE TABLE IF NOT EXISTS private.invite_log (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    inviter UUID NOT NULL,
    at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS invite_log_inviter_at ON private.invite_log (inviter, at);
REVOKE ALL ON private.invite_log FROM public, anon, authenticated;

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

    -- At most 20 invitations an hour from one account, whatever the name: a name nobody has counts
    -- too, so the limit can't be dodged, and names can't be tried by the hundred.
    IF (SELECT count(*) FROM private.invite_log l WHERE l.inviter = caller AND l.at > now() - INTERVAL '1 hour') >= 20 THEN
        RAISE EXCEPTION 'too_many_invites' USING ERRCODE = 'check_violation',
            DETAIL = 'At most 20 invitations an hour. Try again later.';
    END IF;
    DELETE FROM private.invite_log l WHERE l.inviter = caller AND l.at < now() - INTERVAL '1 day';
    INSERT INTO private.invite_log (inviter) VALUES (caller);

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

