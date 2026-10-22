# Security verification

Every security fix in the plan (S001 to S070) has a test or a written check here. Each entry says what was done, and how to see that it holds. Run checks against the local stack ([LOCAL_SUPABASE.md](../LOCAL_SUPABASE.md)) unless they say otherwise. SQL runs as `postgres`, with `docker exec supabase_db_pixelquest psql -U postgres -c "..."`.

## Database

### S008: RLS on every public table
- **Done:** migration `20261019000109_rls_everywhere` turns RLS on for any public table without it, and fails if one is left. Every table already had it.
- **Check:** `scripts/security/check_db.sql` (S029) lists tables without RLS. It returns no rows, and CI fails if it ever does.
  ```sql
  SELECT c.relname FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
  WHERE n.nspname = 'public' AND c.relkind IN ('r','p') AND NOT c.relrowsecurity;
  ```

### S009: default deny for new objects
- **Done:** migration `20261019000110_default_deny`. New tables, sequences and functions that `postgres` creates in `public` give `anon` and `authenticated` nothing. New functions in any schema lose Postgres's EXECUTE-to-PUBLIC default. The service role keeps its access. Each migration grants what it needs ([MIGRATIONS.md](MIGRATIONS.md)).
- **Check** (a throwaway table and functions, rolled back). The result is `false false false false false true`:
  ```sql
  BEGIN;
  CREATE TABLE public.zz_probe(id int);
  CREATE FUNCTION public.zz_probe_fn() RETURNS int LANGUAGE sql AS 'select 1';
  CREATE FUNCTION private.zz_probe_fn2() RETURNS int LANGUAGE sql AS 'select 1';
  SELECT has_table_privilege('anon','public.zz_probe','select'), has_table_privilege('authenticated','public.zz_probe','insert'),
         has_function_privilege('anon','public.zz_probe_fn()','execute'), has_function_privilege('authenticated','public.zz_probe_fn()','execute'),
         has_function_privilege('authenticated','private.zz_probe_fn2()','execute'), has_function_privilege('service_role','public.zz_probe_fn()','execute');
  ROLLBACK;
  ```
