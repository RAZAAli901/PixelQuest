-- Migration: 20261019000109_rls_everywhere.sql
-- Description: S008 of the security plan (docs/security/VERIFY.md): row-level security is on for
-- every table in the public schema. It already was; this turns it on for any table that somehow
-- isn't, then refuses to finish if one is still without it, so a later environment built from these
-- migrations can't end up with an open table. scripts/security/check_db.sql checks it in CI.
-- Re-runnable: ENABLE is idempotent.
-- Rollback: none needed (turning RLS off is never the fix).

DO $$
DECLARE
    t RECORD;
BEGIN
    FOR t IN
        SELECT c.relname FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE n.nspname = 'public' AND c.relkind IN ('r', 'p') AND NOT c.relrowsecurity
    LOOP
        EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY', t.relname);
        RAISE NOTICE 'RLS turned on for public.%', t.relname;
    END LOOP;
    IF EXISTS (
        SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE n.nspname = 'public' AND c.relkind IN ('r', 'p') AND NOT c.relrowsecurity
    ) THEN
        RAISE EXCEPTION 'a public table is still without row-level security';
    END IF;
END $$;
