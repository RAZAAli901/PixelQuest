-- Migration: 20261019000041_study_completion_policies.sql
-- Description: Ticking a study item off, and undoing it (docs/study-party.md, Completing).
--   * An active member completes an item for themselves alone. The server fills in the item's
--     party, who and when; the day must be today or up to 7 days back in a real time zone, so one
--     made offline still counts (bad_time_zone, bad_day). Once per member per item: sent again,
--     it's refused as a duplicate, and the phone counts that as done.
--   * A member undoes their own completion within 24 hours; after that it's final, so a week's
--     scores can't be rewritten. Nobody edits one (no update grant).
-- Re-runnable: CREATE OR REPLACE, DROP POLICY IF EXISTS, DROP TRIGGER IF EXISTS.
-- Rollback: DROP POLICY for each policy below; DROP TRIGGER study_completion_guard;
--   DROP FUNCTION public.study_completion_guard(); REVOKE the grants.

GRANT INSERT (item_id, local_date, tz, client_id) ON public.study_completions TO authenticated;
GRANT DELETE ON public.study_completions TO authenticated;

DROP POLICY IF EXISTS "Members complete items for themselves" ON public.study_completions;
CREATE POLICY "Members complete items for themselves"
    ON public.study_completions FOR INSERT TO authenticated
    WITH CHECK (user_id = (SELECT auth.uid()) AND private.is_active_study_member(party_id));

DROP POLICY IF EXISTS "Members undo their own completion within a day" ON public.study_completions;
CREATE POLICY "Members undo their own completion within a day"
    ON public.study_completions FOR DELETE TO authenticated
    USING (
        user_id = (SELECT auth.uid())
        AND private.is_active_study_member(party_id)
        AND completed_at > now() - INTERVAL '24 hours'
    );

CREATE OR REPLACE FUNCTION public.study_completion_guard()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    today DATE;
BEGIN
    SELECT i.party_id INTO NEW.party_id FROM public.study_items i WHERE i.id = NEW.item_id;
    IF NEW.party_id IS NULL THEN
        RAISE EXCEPTION 'no_such_item' USING ERRCODE = 'foreign_key_violation',
            DETAIL = 'That item isn''t there.';
    END IF;
    IF (SELECT auth.uid()) IS NOT NULL THEN
        NEW.user_id := (SELECT auth.uid());
    END IF;
    NEW.completed_at := now();
    BEGIN
        today := (now() AT TIME ZONE NEW.tz)::DATE;
    EXCEPTION WHEN invalid_parameter_value THEN
        RAISE EXCEPTION 'bad_time_zone' USING ERRCODE = 'invalid_parameter_value',
            DETAIL = 'tz must be a time zone name such as Asia/Karachi.';
    END;
    IF NEW.local_date > today OR NEW.local_date < today - 7 THEN
        RAISE EXCEPTION 'bad_day' USING ERRCODE = 'check_violation',
            DETAIL = 'A completion is for today, or up to 7 days back, in the member''s time zone.';
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS study_completion_guard ON public.study_completions;
CREATE TRIGGER study_completion_guard
    BEFORE INSERT ON public.study_completions
    FOR EACH ROW
    EXECUTE FUNCTION public.study_completion_guard();

REVOKE ALL ON FUNCTION public.study_completion_guard() FROM public, anon, authenticated;
