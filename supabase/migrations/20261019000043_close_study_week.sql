-- Migration: 20261019000043_close_study_week.sql
-- Description: Closing a study party's week, and its winners on the board (docs/study-party.md, Weeks).
--   * study_week_winners: per party and week, who won and with how many points; ties all win.
--     Its members read it; only close_study_week writes it.
--   * close_study_week(party_id, week_start): any active member of the study party may ask, for a
--     week (a Monday) that has ended everywhere: from its second Tuesday on the server's clock, since
--     the members' own weeks end up to about a day apart (not_a_member, bad_week, week_not_over).
--     It records the week's highest points and everyone who has them, once: asked again, it answers
--     with the winners it recorded (already_closed). A week where nobody scored has no winner.
--   * What comes in after a week closes (a completion made offline, up to 7 days back) still counts
--     in its points, but its winners stay as recorded: the simplest board that never changes under
--     the players.
-- Re-runnable: IF NOT EXISTS, CREATE OR REPLACE, DROP POLICY IF EXISTS.
-- Rollback: DROP FUNCTION IF EXISTS public.close_study_week(UUID, DATE); DROP TABLE IF EXISTS public.study_week_winners;

CREATE TABLE IF NOT EXISTS public.study_week_winners (
    party_id UUID NOT NULL REFERENCES public.parties(id) ON DELETE CASCADE,
    week_start DATE NOT NULL,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    points INTEGER NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    PRIMARY KEY (party_id, week_start, user_id)
);

ALTER TABLE public.study_week_winners ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.study_week_winners FROM anon;
REVOKE INSERT, UPDATE, DELETE, TRUNCATE, REFERENCES, TRIGGER ON public.study_week_winners FROM authenticated;
GRANT SELECT ON public.study_week_winners TO authenticated;

DROP POLICY IF EXISTS "Members read their study party's winners" ON public.study_week_winners;
CREATE POLICY "Members read their study party's winners"
    ON public.study_week_winners FOR SELECT TO authenticated
    USING (private.is_active_study_member(party_id));

-- Marks a week closed with no winner, so asking again is answered as closed.
CREATE TABLE IF NOT EXISTS private.study_weeks_closed (
    party_id UUID NOT NULL,
    week_start DATE NOT NULL,
    closed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    PRIMARY KEY (party_id, week_start)
);
REVOKE ALL ON private.study_weeks_closed FROM public, anon, authenticated;

CREATE OR REPLACE FUNCTION public.close_study_week(party_id UUID, week_start DATE)
RETURNS JSONB
LANGUAGE plpgsql
VOLATILE
SECURITY DEFINER
SET search_path = ''
AS $$
#variable_conflict use_variable
DECLARE
    top INTEGER;
    status TEXT := 'closed';
BEGIN
    IF (SELECT auth.uid()) IS NULL THEN
        RAISE EXCEPTION 'Not authenticated' USING ERRCODE = 'insufficient_privilege';
    END IF;
    IF NOT private.is_active_study_member(party_id) THEN
        RAISE EXCEPTION 'not_a_member' USING ERRCODE = 'insufficient_privilege',
            DETAIL = 'Only the study party''s members close its weeks.';
    END IF;
    IF week_start IS NULL OR extract(isodow FROM week_start) <> 1 THEN
        RAISE EXCEPTION 'bad_week' USING ERRCODE = 'check_violation',
            DETAIL = 'A week starts on a Monday.';
    END IF;
    IF (now() AT TIME ZONE 'UTC')::DATE < week_start + 8 THEN
        RAISE EXCEPTION 'week_not_over' USING ERRCODE = 'check_violation',
            DETAIL = 'A week closes once it has ended everywhere.';
    END IF;

    -- One at a time per party and week, so two members asking at once record it once.
    PERFORM pg_advisory_xact_lock(hashtext(party_id::TEXT || week_start::TEXT));
    IF EXISTS (SELECT 1 FROM private.study_weeks_closed c WHERE c.party_id = party_id AND c.week_start = week_start) THEN
        status := 'already_closed';
    ELSE
        SELECT max(s.points) INTO top FROM public.study_week_scores s
        WHERE s.party_id = party_id AND s.week_start = week_start;
        IF coalesce(top, 0) > 0 THEN
            INSERT INTO public.study_week_winners (party_id, week_start, user_id, points)
            SELECT s.party_id, s.week_start, s.user_id, s.points FROM public.study_week_scores s
            WHERE s.party_id = party_id AND s.week_start = week_start AND s.points = top;
        END IF;
        INSERT INTO private.study_weeks_closed (party_id, week_start) VALUES (party_id, week_start);
    END IF;

    RETURN jsonb_build_object(
        'status', status,
        'winners', coalesce((
            SELECT jsonb_agg(jsonb_build_object('user_id', w.user_id, 'points', w.points) ORDER BY w.user_id)
            FROM public.study_week_winners w WHERE w.party_id = party_id AND w.week_start = week_start
        ), '[]'::JSONB)
    );
END;
$$;

REVOKE ALL ON FUNCTION public.close_study_week(UUID, DATE) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.close_study_week(UUID, DATE) TO authenticated;

COMMENT ON TABLE public.study_week_winners IS 'A study party''s weekly winners (ties all win), recorded when the week closes.';
COMMENT ON FUNCTION public.close_study_week(UUID, DATE) IS 'Closes a study party''s ended week once, recording its top scorers.';
