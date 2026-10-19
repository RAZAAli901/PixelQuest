-- Migration: 20261019000015_entry_insert_policy.sql
-- Description: Posting entries (docs/darood-design.md, Entries).
--   * Signed-in players may insert only party_id, amount, local_date and tz. user_id isn't theirs
--     to send: it's always the caller (its default, auth.uid()), so nobody posts as someone else.
--   * The policy lets in only the caller's own row, in a party where they're an active member.
--     The triggers then check the party is open, the amount, the day and the daily cap.
--   * Entries are final: there's no UPDATE or DELETE grant or policy, so clients can't change or
--     remove one (42501, HTTP 403/401). An entry goes only with its party.
-- Re-runnable: the grant is repeatable and the policy is dropped first.
-- Rollback: DROP POLICY IF EXISTS "Active members post their own entries" ON public.darood_entries;
--   REVOKE INSERT ON public.darood_entries FROM authenticated;

GRANT INSERT (party_id, amount, local_date, tz) ON public.darood_entries TO authenticated;

DROP POLICY IF EXISTS "Active members post their own entries" ON public.darood_entries;
CREATE POLICY "Active members post their own entries"
    ON public.darood_entries
    FOR INSERT
    TO authenticated
    WITH CHECK (user_id = (SELECT auth.uid()) AND private.is_active_member(party_id));
