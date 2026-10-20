-- Migration: 20261019000039_study_rls.sql
-- Description: Only a study party's active members read its items, completions and sessions, and
-- log their own study time (docs/study-party.md, Privacy).
--   * private.is_active_study_member(party): the caller is an active member of that party, which is
--     a study party and not deleted. Everything below asks it.
--   * Reading: active members read their study party's items, completions and sessions. Outsiders,
--     the invited, members who left and Darood Parties' members read nothing.
--   * Study time: an active member logs a session for themselves (the server fills in who and
--     when), for today or up to 7 days back in a real time zone (bad_time_zone, bad_day), as a
--     Darood entry is. Never edited or deleted.
--   * Items and completions are written under their own rules (20261019000040, 20261019000041).
-- Re-runnable: CREATE OR REPLACE, DROP POLICY IF EXISTS, DROP TRIGGER IF EXISTS.
-- Rollback: DROP POLICY for each policy below; DROP TRIGGER study_session_guard;
--   DROP FUNCTION private.is_active_study_member(UUID), public.study_session_guard().

CREATE OR REPLACE FUNCTION private.is_active_study_member(party UUID)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM public.party_members m
        JOIN public.parties p ON p.id = m.party_id
        WHERE m.party_id = party
          AND m.user_id = (SELECT auth.uid())
          AND m.status = 'active'
          AND p.party_type = 'study'
          AND p.deleted_at IS NULL
    );
$$;

REVOKE ALL ON FUNCTION private.is_active_study_member(UUID) FROM public, anon;
GRANT EXECUTE ON FUNCTION private.is_active_study_member(UUID) TO authenticated;

REVOKE ALL ON public.study_items, public.study_completions, public.study_sessions FROM anon;
REVOKE INSERT, UPDATE, DELETE, TRUNCATE, REFERENCES, TRIGGER
    ON public.study_items, public.study_completions, public.study_sessions FROM authenticated;
GRANT SELECT ON public.study_items, public.study_completions, public.study_sessions TO authenticated;

DROP POLICY IF EXISTS "Members read their study party's items" ON public.study_items;
CREATE POLICY "Members read their study party's items"
    ON public.study_items FOR SELECT TO authenticated
    USING (private.is_active_study_member(party_id));

DROP POLICY IF EXISTS "Members read their study party's completions" ON public.study_completions;
CREATE POLICY "Members read their study party's completions"
    ON public.study_completions FOR SELECT TO authenticated
    USING (private.is_active_study_member(party_id));

DROP POLICY IF EXISTS "Members read their study party's sessions" ON public.study_sessions;
CREATE POLICY "Members read their study party's sessions"
    ON public.study_sessions FOR SELECT TO authenticated
    USING (private.is_active_study_member(party_id));

-- Logging study time: only what the member sends; who and when are the server's.
GRANT INSERT (party_id, minutes, local_date, tz, client_id) ON public.study_sessions TO authenticated;

DROP POLICY IF EXISTS "Members log their own study time" ON public.study_sessions;
CREATE POLICY "Members log their own study time"
    ON public.study_sessions FOR INSERT TO authenticated
    WITH CHECK (user_id = (SELECT auth.uid()) AND private.is_active_study_member(party_id));

CREATE OR REPLACE FUNCTION public.study_session_guard()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    today DATE;
BEGIN
    NEW.user_id := (SELECT auth.uid());
    NEW.created_at := now();
    BEGIN
        today := (now() AT TIME ZONE NEW.tz)::DATE;
    EXCEPTION WHEN invalid_parameter_value THEN
        RAISE EXCEPTION 'bad_time_zone' USING ERRCODE = 'invalid_parameter_value',
            DETAIL = 'tz must be a time zone name such as Asia/Karachi.';
    END;
    IF NEW.local_date > today OR NEW.local_date < today - 7 THEN
        RAISE EXCEPTION 'bad_day' USING ERRCODE = 'check_violation',
            DETAIL = 'Study time is for today, or up to 7 days back, in the member''s time zone.';
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS study_session_guard ON public.study_sessions;
CREATE TRIGGER study_session_guard
    BEFORE INSERT ON public.study_sessions
    FOR EACH ROW
    EXECUTE FUNCTION public.study_session_guard();

REVOKE ALL ON FUNCTION public.study_session_guard() FROM public, anon, authenticated;
