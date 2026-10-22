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

### S012: reports are insert-only
- **Done:** migration `20261019000112_reports_grants`. `reports`:
  - signed-in players may only INSERT, in their own name (the policy pins `reporter_id` to `auth.uid()`);
  - signed-out users get nothing;
  - staff read reports with the service role, from the dashboard.
- **Already closed:**
  - `chat_reports` and `chess_reports` have no grants to app roles; they're written only by `report_chat_message` and `report_chess_challenge`.
  - Profile reports are limited to one per player a day.
- **Test:** `reports_and_deletion.test.ts`:
  - a forged reporter is refused (403);
  - reading back is refused (403);
  - signed out is refused (401);
  - the service role sees the report.
- **Test:** `abuse.test.ts` and `study_chat.test.ts`: `chess_reports` and `chat_reports` aren't readable (403).

### S013: parties and members against chat and chess
- **Reviewed:**
  - Chat, files and chess read through `private.is_active_study_member`: the caller is an *active* member of a *study* party that isn't deleted. Invitees, members who left and removed members get nothing.
  - `parties`: members and invitees read it.
  - A leader may update only `name`, `milestone` and `is_public_race` (column grants). The policy pins `leader_id` to the leader, so leadership moves only through `transfer_leadership`.
  - `party_members`: no direct writes. Invitations, joining, leaving and removal go through functions.
- **Test:** `security.test.ts`, "only active members reach chat and chess; a leader can't retype or finish a party":
  - an invitee and a member who left see no chat, chess or member list;
  - the leader's PATCH of `party_type`, `completed_at`, `leader_id` or `max_members` is refused.
- **Test:** "a removed member loses chat, files and chess at once" (J170).

### S014: Darood entries are insert-only
- **State:** `darood_entries` grants app users SELECT, plus INSERT through the policy "Active members post their own entries" (`user_id` = the caller, an active member). No UPDATE or DELETE grant exists, for anyone.
  - The daily cap and the amount range are constraints and triggers on the server.
  - A deleted account's entries stay in the total without a name (SET NULL).
- **Test:** `darood_parties.test.ts`:
  - posting as someone else, as an invitee, as an outsider and signed out are refused (403, 403, 403, 401);
  - a member's PATCH and DELETE and the leader's DELETE are refused (403), and the entry is unchanged.
- **Result 2026-10-22:** 31 of 31 pass.

### S015: study items, completions, sessions and reminders
- **Reviewed.** Every policy requires an active member of a study party, writing as themselves (`created_by` or `user_id` = the caller):
  - **items:** members add; the creator or the leader edits and deletes. Only `title`, `subject`, `notes`, `due_at`, `pinned` and `kind` are updatable; never `party_id` or `created_by`.
  - **completions:** members tick for themselves, and undo their own within 24 hours. No edits.
  - **sessions:** members log their own; no edits or deletes.
  - **reminders:** a "just me" reminder is read only by its creator; the creator, or the leader for party reminders, edits and deletes.
- **Found:** app roles held INSERT and UPDATE column grants on the views `study_chat_config`, `study_chat_types` and `study_chat_storage`.
  - Tested as a signed-in user: writing through them fails ("cannot update view": they're computed), so the chat's limits couldn't be changed.
  - The grants are removed in S017 anyway.
- **Test:** `darood_parties.test.ts`, the study tests. A member can't edit or delete another's item, can't PATCH a completion, and can't undo another's tick.

### S016: storage is private by default
- **State:**
  - One bucket, `study-files`: private, 20 MB, with a list of allowed types.
  - `storage.objects` has RLS with no permissive policy, so app users can reach no object in any bucket, now or in a bucket added later. A restrictive policy also blocks `study-files` by name.
  - `storage.buckets` has RLS with no policy, so players can't create a bucket.
  - Files move only through `request-upload` / `finalize-upload` (the service role, with checks), and come back through `get-download-url` (a signed link valid 60 seconds, for an active member).
- **Path rule:** objects are stored under `<party_id>/<file_id>`, and the functions check the caller's membership of that party before signing.
- **Test:** `security.test.ts`, "storage is closed to players". A player can't create a bucket, upload, list or download directly. `study-files` is private, and no bucket is public.
- **Test:** `study_files.test.ts`. A removed member's link stops working; a link expires.

### S017: minimum grants
- **Done:** migration `20261019000113_minimum_grants`:
  - `anon` holds nothing on any public table, view, sequence or function.
  - `authenticated` has no TRUNCATE, REFERENCES or TRIGGER anywhere, and no write on any view.
  - Trigger functions aren't callable as RPCs.
  - No public function is executable by PUBLIC.
  - Every RPC kept its grant: the list of functions `authenticated` may run lost exactly the four triggers.
- **Check:** each result is 0:
  ```sql
  SELECT count(*) FROM information_schema.role_table_grants WHERE table_schema = 'public' AND grantee = 'anon';
  SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
   WHERE n.nspname = 'public' AND has_function_privilege('anon', p.oid, 'EXECUTE');
  SELECT count(*) FROM information_schema.role_table_grants WHERE table_schema = 'public'
   AND grantee = 'authenticated' AND privilege_type IN ('TRUNCATE', 'REFERENCES', 'TRIGGER');
  ```
- **Result 2026-10-22:**
  - all three are 0;
  - the server suite passes (164 tests, 0 failures);
  - the app's own repositories pass against the stack (`*LocalSupabase*`: 20 tests).
