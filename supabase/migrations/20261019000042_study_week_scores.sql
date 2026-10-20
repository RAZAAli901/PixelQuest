-- Migration: 20261019000042_study_week_scores.sql
-- Description: A study party's weekly points (docs/study-party.md, Points and Weeks).
--   * study_points_config: every points value, in one place, mirrored by the app's
--     StudyPointsConfig (a test holds them equal): 10 for an assignment, 15 for a quiz, +5 for
--     finishing a day or more before it's due, 1 point per 10 minutes of study, 12 a day at most.
--   * study_week_scores: per party, member and week, their points, with the breakdown: items'
--     points, early bonuses, study points, items finished and minutes studied.
--       - A week runs Monday to Sunday in the member's own time zone: it's found from the day each
--         completion and session was recorded on (local_date), whatever the server's clock says.
--       - Study points are counted per day, capped there, then added up for the week.
--       - An item with no due moment gets no early bonus.
--   * It runs with the reader's rights (security_invoker), so a party's scores are its active
--     members' to read, as its completions and sessions are; no points ever reach XP.
-- Re-runnable: CREATE OR REPLACE.
-- Rollback: DROP VIEW IF EXISTS public.study_week_scores; DROP VIEW IF EXISTS public.study_points_config;

CREATE OR REPLACE VIEW public.study_points_config AS
SELECT
    10 AS assignment,
    15 AS quiz,
    5 AS early_bonus,
    10 AS minutes_per_point,
    12 AS daily_study_max;

CREATE OR REPLACE VIEW public.study_week_scores
WITH (security_invoker = true)
AS
WITH config AS (SELECT * FROM public.study_points_config),
finished AS (
    SELECT
        sc.party_id,
        sc.user_id,
        date_trunc('week', sc.local_date)::DATE AS week_start,
        sum(CASE i.kind WHEN 'quiz' THEN config.quiz ELSE config.assignment END)::INTEGER AS base_points,
        sum(CASE WHEN i.due_at IS NOT NULL AND sc.completed_at <= i.due_at - INTERVAL '1 day' THEN config.early_bonus ELSE 0 END)::INTEGER AS bonus_points,
        count(*)::INTEGER AS items_completed
    FROM public.study_completions sc
    JOIN public.study_items i ON i.id = sc.item_id
    CROSS JOIN config
    GROUP BY sc.party_id, sc.user_id, date_trunc('week', sc.local_date)
),
studied AS (
    SELECT
        d.party_id,
        d.user_id,
        date_trunc('week', d.local_date)::DATE AS week_start,
        sum(least(d.minutes / config.minutes_per_point, config.daily_study_max))::INTEGER AS study_points,
        sum(d.minutes)::INTEGER AS study_minutes
    FROM (
        SELECT s.party_id, s.user_id, s.local_date, sum(s.minutes)::INTEGER AS minutes
        FROM public.study_sessions s
        GROUP BY s.party_id, s.user_id, s.local_date
    ) d
    CROSS JOIN config
    GROUP BY d.party_id, d.user_id, date_trunc('week', d.local_date)
)
SELECT
    coalesce(f.party_id, s.party_id) AS party_id,
    coalesce(f.user_id, s.user_id) AS user_id,
    coalesce(f.week_start, s.week_start) AS week_start,
    coalesce(f.base_points, 0) + coalesce(f.bonus_points, 0) + coalesce(s.study_points, 0) AS points,
    coalesce(f.base_points, 0) AS item_points,
    coalesce(f.bonus_points, 0) AS bonus_points,
    coalesce(s.study_points, 0) AS study_points,
    coalesce(f.items_completed, 0) AS items_completed,
    coalesce(s.study_minutes, 0) AS study_minutes
FROM finished f
FULL JOIN studied s ON s.party_id = f.party_id AND s.user_id = f.user_id AND s.week_start = f.week_start;

REVOKE ALL ON public.study_points_config, public.study_week_scores FROM public, anon;
REVOKE INSERT, UPDATE, DELETE, TRUNCATE, REFERENCES, TRIGGER ON public.study_points_config, public.study_week_scores FROM authenticated;
GRANT SELECT ON public.study_points_config, public.study_week_scores TO authenticated;

COMMENT ON VIEW public.study_points_config IS 'Every study points value, mirrored by the app''s StudyPointsConfig.';
COMMENT ON VIEW public.study_week_scores IS 'A study party''s points per member per week (Monday to Sunday, the member''s own), for its members.';
