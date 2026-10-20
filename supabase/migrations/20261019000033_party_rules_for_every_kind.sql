-- Migration: 20261019000033_party_rules_for_every_kind.sql
-- Description: The membership rules are every party's; Darood's own rules are Darood Parties' only
-- (docs/party-core.md).
--   * The membership rules already hold for any kind of party: the 2-30 limit (party_member_cap),
--     invite_member, respond_invite, leave_party, remove_member, transfer_leadership, delete_party
--     and the leader's hand-over (party_leader_gone) never look at what the party does. Their
--     descriptions now say so.
--   * Darood's own rules apply to Darood Parties alone:
--       - darood_only_columns: only a Darood Party has a milestone, finishes (completed_at) or goes
--         on the race board. A study party's are always empty and off, whoever tries to set them.
--       - darood_entry_guard: entries go only into a Darood Party (not_a_darood_party).
--       - race_board: Darood Parties only.
--   * A finished party (completed_at, Darood's milestone reached) still takes no new members; a study
--     party never finishes that way, so that rule never reaches one.
-- Re-runnable: DROP CONSTRAINT IF EXISTS, CREATE OR REPLACE.
-- Rollback: ALTER TABLE public.parties DROP CONSTRAINT IF EXISTS darood_only_columns; then re-run
--   20261019000011 and 20261019000030 for the guard and the view.

ALTER TABLE public.parties DROP CONSTRAINT IF EXISTS darood_only_columns;
ALTER TABLE public.parties ADD CONSTRAINT darood_only_columns
    CHECK (party_type = 'darood' OR (milestone IS NULL AND completed_at IS NULL AND NOT is_public_race));

CREATE OR REPLACE FUNCTION public.darood_entry_guard()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    party public.parties%ROWTYPE;
BEGIN
    SELECT * INTO party FROM public.parties p WHERE p.id = NEW.party_id FOR UPDATE;
    IF NOT FOUND OR party.deleted_at IS NOT NULL OR party.completed_at IS NOT NULL THEN
        RAISE EXCEPTION 'party_closed' USING ERRCODE = 'check_violation',
            DETAIL = 'This party is finished or deleted, so it takes no more entries.';
    END IF;
    IF party.party_type <> 'darood' THEN
        RAISE EXCEPTION 'not_a_darood_party' USING ERRCODE = 'check_violation',
            DETAIL = 'Only a Darood Party takes Darood entries.';
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM public.party_members m
        WHERE m.party_id = NEW.party_id AND m.user_id = NEW.user_id AND m.status = 'active'
    ) THEN
        RAISE EXCEPTION 'not_a_member' USING ERRCODE = 'insufficient_privilege',
            DETAIL = 'Only the party''s active members can post.';
    END IF;
    NEW.created_at := now();
    RETURN NEW;
END;
$$;

REVOKE ALL ON FUNCTION public.darood_entry_guard() FROM public, anon, authenticated;

CREATE OR REPLACE VIEW public.race_board
WITH (security_invoker = false)
AS
SELECT
    p.name,
    coalesce(m.member_count, 0) AS member_count,
    coalesce(e.total, 0) AS total,
    p.milestone,
    least(coalesce(e.total, 0)::NUMERIC / p.milestone, 1)::REAL AS progress,
    p.completed_at,
    EXISTS (
        SELECT 1 FROM public.party_members me
        WHERE me.party_id = p.id AND me.user_id = (SELECT auth.uid()) AND me.status = 'active'
    ) AS mine
FROM public.parties p
LEFT JOIN (
    SELECT party_id, sum(amount)::BIGINT AS total
    FROM public.darood_entries
    GROUP BY party_id
) e ON e.party_id = p.id
LEFT JOIN (
    SELECT party_id, count(*)::INTEGER AS member_count
    FROM public.party_members
    WHERE status = 'active'
    GROUP BY party_id
) m ON m.party_id = p.id
WHERE p.is_public_race
  AND p.party_type = 'darood'
  AND p.deleted_at IS NULL
  AND p.milestone IS NOT NULL
  AND (SELECT auth.uid()) IS NOT NULL;

REVOKE ALL ON public.race_board FROM public, anon;
REVOKE INSERT, UPDATE, DELETE, TRUNCATE, REFERENCES, TRIGGER ON public.race_board FROM authenticated;
GRANT SELECT ON public.race_board TO authenticated;

COMMENT ON TABLE public.parties IS
    'Parties of every kind (party_type): a name, a leader and a member limit. A Darood Party also has a milestone the members race to.';
COMMENT ON TABLE public.party_members IS
    'Memberships of a party of any kind: invited, active, declined, left or removed. Written only by the party functions.';
COMMENT ON FUNCTION public.invite_member(UUID, TEXT) IS
    'The leader invites a username into a party of any kind; the answer never says whether the name exists.';
COMMENT ON FUNCTION public.respond_invite(UUID, BOOLEAN) IS
    'The invited person accepts or declines, in a party of any kind.';
COMMENT ON FUNCTION public.leave_party(UUID) IS
    'A member leaves a party of any kind; its leader hands the leadership over first.';
COMMENT ON FUNCTION public.remove_member(UUID, UUID) IS
    'The leader removes a member, or withdraws an invitation, in a party of any kind.';
COMMENT ON FUNCTION public.transfer_leadership(UUID, UUID) IS
    'The leader hands the leadership of a party of any kind to an active member.';
COMMENT ON FUNCTION public.delete_party(UUID) IS
    'The leader deletes a party of any kind: it is hidden from everyone.';
