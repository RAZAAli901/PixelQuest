-- Migration: 20261019000110_default_deny.sql
-- Description: S009 of the security plan: default deny. Supabase's defaults give the app's roles
-- (anon, authenticated) every privilege on each new table, sequence and function that postgres
-- creates in public, which leaves row-level security as the only lock. From here on a new object
-- starts with no access, and its migration grants exactly what it needs: a table its SELECT or
-- INSERT to authenticated, an RPC its EXECUTE (docs/security/MIGRATIONS.md). Functions also lose
-- Postgres's own EXECUTE-to-PUBLIC default. The service role keeps its access.
-- Existing objects keep their grants; S017 trims those.
-- Re-runnable: ALTER DEFAULT PRIVILEGES is idempotent.
-- Rollback: ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON TABLES TO anon, authenticated; (and the same for SEQUENCES and FUNCTIONS).

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public REVOKE ALL ON TABLES FROM anon, authenticated;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public REVOKE ALL ON SEQUENCES FROM anon, authenticated;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public REVOKE ALL ON FUNCTIONS FROM anon, authenticated;
-- Postgres's own EXECUTE-to-PUBLIC default for functions can only be removed globally (not per
-- schema), so this covers every schema postgres creates functions in. A function that a policy
-- calls (a membership helper) is granted to authenticated by its own migration, as before.
ALTER DEFAULT PRIVILEGES FOR ROLE postgres REVOKE EXECUTE ON FUNCTIONS FROM PUBLIC;
