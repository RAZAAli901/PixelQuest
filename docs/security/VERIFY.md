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

### S010: profiles
- **Policies** (unchanged, already right):
  - a player inserts, updates and deletes only the row whose `id` is their own;
  - they read opted-in rows and their own.
  - Every column is a leaderboard field (name, level, streaks, XP, the opt-in, when it changed). Emails live only in `auth.users`.
- **Done:** migration `20261019000111_profiles_grants`:
  - `anon` loses every privilege (it had all of them, though RLS gave it no rows);
  - `authenticated` keeps only SELECT, INSERT, UPDATE and DELETE (no TRUNCATE, which ignores RLS).
  - The app no longer queries the board while signed out; it shows the same empty board as before.
- **Test:** `supabase/tests/local/profiles_rls.test.ts`:
  - a forged insert with another id is refused (403);
  - a patch of another's row changes nothing;
  - signed out, reading and writing are refused (401).

### S011: the leaderboard
- **Rules:** only opted-in rows are readable by other players (the SELECT policy on `profiles`); a row leaves the board the moment its owner opts out (S026). The fields are name, level, streaks, XP, the opt-in and when it changed, plus the row's id, which the app needs to mark "you" and to report a name.
- **Decision:** column grants would break the app's `select *`, and every column is a board field, so the guard is a test instead: `profiles` must have exactly those columns. Anything private goes in a table only its owner can read.
- **Test:** `profiles_rls.test.ts`, "the board exposes only leaderboard fields" and "leaving the board hides the row from others".
