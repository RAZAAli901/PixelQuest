-- Migration: 20261010000002_profiles_policies_per_query.sql
-- Description: The leaderboard's row-level security, rewritten the way Supabase's Performance
-- Advisor recommends, with the same rules:
-- - auth.uid() is wrapped in (select auth.uid()), so Postgres works it out once per query instead
--   of once per row (auth_rls_initplan); the leaderboard reads many rows;
-- - the two read policies (opted-in rows, and your own row) become one, so each row is checked
--   once (multiple_permissive_policies).
-- Who can read and write what is unchanged; supabase/tests/local/profiles_rls.test.ts checks it.

DROP POLICY IF EXISTS "Public read opted in profiles" ON public.profiles;
DROP POLICY IF EXISTS "Users can read own profile" ON public.profiles;
DROP POLICY IF EXISTS "Users can insert own profile" ON public.profiles;
DROP POLICY IF EXISTS "Users can update own profile" ON public.profiles;
DROP POLICY IF EXISTS "Users can delete own profile" ON public.profiles;

-- Signed-in players read the board (opted-in rows) and always their own row.
CREATE POLICY "Read opted-in profiles and your own"
    ON public.profiles
    FOR SELECT
    TO authenticated
    USING (leaderboard_opt_in = TRUE OR (SELECT auth.uid()) = id);

CREATE POLICY "Insert your own profile"
    ON public.profiles
    FOR INSERT
    TO authenticated
    WITH CHECK ((SELECT auth.uid()) = id);

CREATE POLICY "Update your own profile"
    ON public.profiles
    FOR UPDATE
    TO authenticated
    USING ((SELECT auth.uid()) = id)
    WITH CHECK ((SELECT auth.uid()) = id);

CREATE POLICY "Delete your own profile"
    ON public.profiles
    FOR DELETE
    TO authenticated
    USING ((SELECT auth.uid()) = id);

-- reports: the same per-query form for its one policy.
DROP POLICY IF EXISTS allow_insert_authenticated_reports ON public.reports;
CREATE POLICY allow_insert_authenticated_reports ON public.reports
    FOR INSERT
    TO authenticated
    WITH CHECK ((SELECT auth.uid()) = reporter_id);
