-- Migration: 20261011000000_profile_progress_plausibility.sql
-- Description: The leaderboard no longer takes whatever numbers the app sends. Before a profile row
-- is written, its streaks and level are kept within what the app's own rules allow, given the
-- player's XP and how long ago their streak and level last changed.
--
-- Impossible values are lowered rather than refused. A refusal would leave the app's sync retrying
-- forever. An honest player caught by a limit (a nightly check that missed several nights, then
-- caught up at once) shows the right numbers after the next few syncs.
--
-- The rules, from the app (PointsCalculator, StreakEvaluationWorker, DifficultyMode):
--   * Every completed quest earns at least 50 XP, and a perfect day needs at least one, so the
--     longest streak is at most total_xp / 50.
--   * A level takes at least 3 perfect days, so level - 1 is at most total_xp / 150.
--   * A streak grows by at most one a calendar day. A level takes 3 days, though one level may come
--     sooner with progress carried over from a harder difficulty. Both allow two days of slack, for
--     time zones and a nightly check that ran a night late.
--   * The current streak is never longer than the longest.
--   * The caps the app's backup import applies: XP 10,000,000, level 999, streaks 36,500.
-- XP itself can't be checked further: the number of quests a player may have is unlimited.
--
-- When each player's streak and level last changed is kept in its own table. Every row of
-- profiles is public to the app, which decodes it strictly, so a new column there would break
-- installed apps. The client can't read or set these times.
-- The service role (the project's own server code and test bench) is trusted and not limited.

CREATE TABLE IF NOT EXISTS public.profile_progress_marks (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    streak_changed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    level_changed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- No policies: only the trigger function below, running as its owner, reads and writes it.
ALTER TABLE public.profile_progress_marks ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.profile_progress_marks FROM anon, authenticated;

CREATE OR REPLACE FUNCTION public.keep_profile_progress_plausible()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    marks public.profile_progress_marks%ROWTYPE;
    today DATE := (now() AT TIME ZONE 'UTC')::date;
    streak_days INTEGER;
    level_days INTEGER;
BEGIN
    -- The server's clock. Before this, a first insert stored the updated_at the client sent.
    NEW.updated_at := now();

    IF (SELECT auth.role()) = 'service_role' THEN
        RETURN NEW;
    END IF;

    -- The insert half of an upsert on an existing row (PostgREST's on_conflict): columns the client
    -- didn't send hold their defaults here (0 XP), so the update half checks the row instead.
    IF TG_OP = 'INSERT' AND EXISTS (SELECT 1 FROM public.profiles WHERE id = NEW.id) THEN
        RETURN NEW;
    END IF;

    NEW.total_xp := LEAST(NEW.total_xp, 10000000);
    NEW.level := LEAST(NEW.level, 999);
    NEW.current_streak := LEAST(NEW.current_streak, 36500);
    NEW.longest_streak := LEAST(NEW.longest_streak, 36500);

    IF TG_OP = 'UPDATE' THEN
        SELECT * INTO marks FROM public.profile_progress_marks WHERE id = NEW.id;
        IF FOUND THEN
            streak_days := today - (marks.streak_changed_at AT TIME ZONE 'UTC')::date;
            IF NEW.current_streak > OLD.current_streak THEN
                NEW.current_streak := LEAST(NEW.current_streak, OLD.current_streak + streak_days + 2);
            END IF;
            -- The longest streak only grows with the current one, so no faster either.
            IF NEW.longest_streak > OLD.longest_streak THEN
                NEW.longest_streak := LEAST(NEW.longest_streak, OLD.longest_streak + streak_days + 2);
            END IF;

            level_days := today - (marks.level_changed_at AT TIME ZONE 'UTC')::date;
            IF NEW.level > OLD.level THEN
                NEW.level := LEAST(NEW.level, OLD.level + 1 + (level_days + 2) / 3);
            END IF;
        END IF;
    END IF;

    NEW.longest_streak := LEAST(NEW.longest_streak, NEW.total_xp / 50);
    NEW.current_streak := LEAST(NEW.current_streak, NEW.longest_streak);
    NEW.level := LEAST(NEW.level, 1 + NEW.total_xp / 150);

    -- Remember when the streak and level changed. A new player starts now.
    IF TG_OP = 'INSERT' THEN
        INSERT INTO public.profile_progress_marks (id) VALUES (NEW.id)
        ON CONFLICT (id) DO UPDATE SET streak_changed_at = now(), level_changed_at = now();
    ELSE
        INSERT INTO public.profile_progress_marks (id) VALUES (NEW.id) ON CONFLICT (id) DO NOTHING;
        IF NEW.current_streak IS DISTINCT FROM OLD.current_streak THEN
            UPDATE public.profile_progress_marks SET streak_changed_at = now() WHERE id = NEW.id;
        END IF;
        IF NEW.level IS DISTINCT FROM OLD.level THEN
            UPDATE public.profile_progress_marks SET level_changed_at = now() WHERE id = NEW.id;
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

-- A trigger function can't be called on its own, but nobody needs to try.
REVOKE EXECUTE ON FUNCTION public.keep_profile_progress_plausible() FROM PUBLIC, anon, authenticated;

-- Named to run before set_profiles_updated_at and the display-name check (triggers fire by name).
DROP TRIGGER IF EXISTS keep_profile_progress_plausible ON public.profiles;
CREATE TRIGGER keep_profile_progress_plausible
    BEFORE INSERT OR UPDATE ON public.profiles
    FOR EACH ROW
    EXECUTE FUNCTION public.keep_profile_progress_plausible();
