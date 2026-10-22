-- Migration: 20261019000113_minimum_grants.sql
-- Description: S017 of the security plan: the app's roles keep only the access the app uses.
-- Supabase's defaults had given anon and authenticated every privilege on each object; RLS kept
-- the rows safe, but the grants were wider than any feature needs:
--   * anon (signed out) loses everything on every public table, view and function: no feature
--     works signed out except sign-in itself, which is Supabase Auth, not these.
--   * authenticated loses TRUNCATE (it ignores RLS), REFERENCES and TRIGGER everywhere, and any
--     INSERT, UPDATE or DELETE on views (study_chat_config, study_chat_types, study_chat_storage
--     had column grants; writing through them failed, but they shouldn't exist).
--   * Trigger functions (handle_updated_at, stamp_report_time, usernames_lowercase,
--     check_display_name_moderation) can't be called as RPCs by anyone; triggers still fire.
--   * Postgres's EXECUTE-to-PUBLIC is removed from every public function. Each RPC keeps its
--     explicit grant to authenticated (and service-role-only functions keep theirs).
-- Re-runnable: REVOKE is idempotent.
-- Rollback: GRANT ALL ON ALL TABLES IN SCHEMA public TO anon, authenticated; GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO anon, authenticated;

DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT c.relname, c.relkind FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE n.nspname = 'public' AND c.relkind IN ('r', 'p', 'v', 'm')
    LOOP
        EXECUTE format('REVOKE ALL ON public.%I FROM anon', r.relname);
        EXECUTE format('REVOKE TRUNCATE, REFERENCES, TRIGGER ON public.%I FROM authenticated', r.relname);
        IF r.relkind IN ('v', 'm') THEN
            EXECUTE format('REVOKE INSERT, UPDATE, DELETE ON public.%I FROM authenticated', r.relname);
        END IF;
    END LOOP;
END $$;

REVOKE ALL ON ALL SEQUENCES IN SCHEMA public FROM anon;
REVOKE EXECUTE ON ALL FUNCTIONS IN SCHEMA public FROM PUBLIC, anon;

REVOKE EXECUTE ON FUNCTION public.handle_updated_at() FROM authenticated;
REVOKE EXECUTE ON FUNCTION public.stamp_report_time() FROM authenticated;
REVOKE EXECUTE ON FUNCTION public.usernames_lowercase() FROM authenticated;
REVOKE EXECUTE ON FUNCTION public.check_display_name_moderation() FROM authenticated;
