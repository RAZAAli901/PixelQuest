-- Migration: 20261019000048_study_reminder_policies.sql
-- Description: Who writes a study party's reminders (docs/study-reminders.md, Who sees what).
--   * Adding: any active member, for the party or for themselves. The server fills in who and when
--     (created_by, created_at); tz must be a real time zone (bad_time_zone). A member keeps at most
--     100 reminders in a party (too_many_reminders), so nobody can flood it.
--   * Changing and deleting: only its creator, or the party's leader for a party reminder (the
--     leader can't read, so can't touch, a member's own). Its party, creator and audience never
--     change: there's no column grant for them on update.
-- Re-runnable: CREATE OR REPLACE, DROP POLICY IF EXISTS, DROP TRIGGER IF EXISTS.
-- Rollback: DROP POLICY for each policy below; DROP TRIGGER study_reminder_guard;
--   DROP FUNCTION public.study_reminder_guard(); REVOKE the grants.

GRANT INSERT (party_id, title, kind, scope, due_at, week_start, tz, repeat, audience, alert_offsets)
    ON public.study_reminders TO authenticated;
GRANT UPDATE (title, kind, scope, due_at, week_start, tz, repeat, alert_offsets)
    ON public.study_reminders TO authenticated;
GRANT DELETE ON public.study_reminders TO authenticated;

DROP POLICY IF EXISTS "Members add reminders" ON public.study_reminders;
CREATE POLICY "Members add reminders"
    ON public.study_reminders FOR INSERT TO authenticated
    WITH CHECK (created_by = (SELECT auth.uid()) AND private.is_active_study_member(party_id));

DROP POLICY IF EXISTS "Creators and leaders change reminders" ON public.study_reminders;
CREATE POLICY "Creators and leaders change reminders"
    ON public.study_reminders FOR UPDATE TO authenticated
    USING (
        private.is_active_study_member(party_id)
        AND (created_by = (SELECT auth.uid()) OR (audience = 'party' AND private.is_party_leader(party_id)))
    )
    WITH CHECK (
        private.is_active_study_member(party_id)
        AND (created_by = (SELECT auth.uid()) OR (audience = 'party' AND private.is_party_leader(party_id)))
    );

DROP POLICY IF EXISTS "Creators and leaders delete reminders" ON public.study_reminders;
CREATE POLICY "Creators and leaders delete reminders"
    ON public.study_reminders FOR DELETE TO authenticated
    USING (
        private.is_active_study_member(party_id)
        AND (created_by = (SELECT auth.uid()) OR (audience = 'party' AND private.is_party_leader(party_id)))
    );

CREATE OR REPLACE FUNCTION public.study_reminder_guard()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    probe TIMESTAMP;
BEGIN
    IF TG_OP = 'INSERT' THEN
        NEW.created_by := (SELECT auth.uid());
        NEW.created_at := now();
        IF (SELECT count(*) FROM public.study_reminders r
            WHERE r.party_id = NEW.party_id AND r.created_by = NEW.created_by) >= 100 THEN
            RAISE EXCEPTION 'too_many_reminders' USING ERRCODE = 'check_violation',
                DETAIL = 'A member keeps at most 100 reminders in a party.';
        END IF;
    END IF;
    BEGIN
        probe := now() AT TIME ZONE NEW.tz;
    EXCEPTION WHEN invalid_parameter_value THEN
        RAISE EXCEPTION 'bad_time_zone' USING ERRCODE = 'invalid_parameter_value',
            DETAIL = 'tz must be a time zone name such as Asia/Karachi.';
    END;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS study_reminder_guard ON public.study_reminders;
CREATE TRIGGER study_reminder_guard
    BEFORE INSERT OR UPDATE ON public.study_reminders
    FOR EACH ROW
    EXECUTE FUNCTION public.study_reminder_guard();

REVOKE ALL ON FUNCTION public.study_reminder_guard() FROM public, anon, authenticated;
