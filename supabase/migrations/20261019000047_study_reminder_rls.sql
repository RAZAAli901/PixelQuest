-- Migration: 20261019000047_study_reminder_rls.sql
-- Description: Who reads a study party's reminders (docs/study-reminders.md, Who sees what).
--   * A party reminder: every active member of its study party (private.is_active_study_member,
--     20261019000039), and nobody else.
--   * A `me` reminder: only its creator, while they're an active member. Not the leader, not the
--     other members.
--   * Members who left or were removed, the invited, outsiders and the signed out read nothing.
--   * Writing comes in 20261019000048; until then there's no grant to write.
-- Re-runnable: DROP POLICY IF EXISTS.
-- Rollback: DROP POLICY IF EXISTS "Members read their study party's reminders" ON public.study_reminders;
--   REVOKE SELECT ON public.study_reminders FROM authenticated;

REVOKE ALL ON public.study_reminders FROM anon;
REVOKE INSERT, UPDATE, DELETE, TRUNCATE, REFERENCES, TRIGGER ON public.study_reminders FROM authenticated;
GRANT SELECT ON public.study_reminders TO authenticated;

DROP POLICY IF EXISTS "Members read their study party's reminders" ON public.study_reminders;
CREATE POLICY "Members read their study party's reminders"
    ON public.study_reminders FOR SELECT TO authenticated
    USING (
        private.is_active_study_member(party_id)
        AND (audience = 'party' OR created_by = (SELECT auth.uid()))
    );
