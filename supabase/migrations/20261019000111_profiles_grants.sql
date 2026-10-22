-- Migration: 20261019000111_profiles_grants.sql
-- Description: S010 of the security plan: profiles. Its policies already let a player write only
-- their own row and read only opted-in rows and their own. The table grants were wider than the
-- policies: anon held every privilege (RLS gave it no rows), and both app roles held TRUNCATE,
-- REFERENCES and TRIGGER, which PostgREST never uses (TRUNCATE ignores RLS). Now anon has nothing
-- and authenticated only what the app does. Every column is a leaderboard field (name, level,
-- streaks, XP, the opt-in, when it changed); nothing private lives in profiles (emails stay in
-- auth.users), so no separate public view is needed.
-- Re-runnable: REVOKE and GRANT are idempotent.
-- Rollback: GRANT ALL ON public.profiles TO anon, authenticated;

REVOKE ALL ON public.profiles FROM anon;
REVOKE ALL ON public.profiles FROM authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.profiles TO authenticated;
