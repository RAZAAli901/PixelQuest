-- Migration: 20261019000016_party_leader_policy.sql
-- Description: Only the leader changes a party (docs/darood-design.md, Parties).
--   * Signed-in players may update only a party's name and milestone (a column-level grant): never
--     its leader, limit, completion or deletion, which only the party functions change.
--   * The policy lets only the party's leader do even that, on a party that isn't deleted. Anyone
--     else's update matches no row and changes nothing.
--   * The name rules still hold (party_limits); the milestone's own rules (above the total, not
--     after finishing) come with set_milestone.
--   * Removing a member is the leader's too: remove_member (with the membership functions) checks
--     it. party_members keeps no client write grant, so that function is the only way in.
-- Re-runnable: the grant is repeatable and the policy is dropped first.
-- Rollback: DROP POLICY IF EXISTS "The leader updates the party" ON public.parties;
--   REVOKE UPDATE ON public.parties FROM authenticated;

GRANT UPDATE (name, milestone) ON public.parties TO authenticated;

DROP POLICY IF EXISTS "The leader updates the party" ON public.parties;
CREATE POLICY "The leader updates the party"
    ON public.parties
    FOR UPDATE
    TO authenticated
    USING (leader_id = (SELECT auth.uid()) AND deleted_at IS NULL)
    WITH CHECK (leader_id = (SELECT auth.uid()) AND deleted_at IS NULL);
