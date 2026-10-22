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

### S018: SECURITY DEFINER functions
- **Done:** migration `20261019000114_definer_search_paths` rewrites `delete_user_account`. It was the only definer function whose search path wasn't empty (`public, auth`); it now names every table with its schema. Behaviour is unchanged.
- **Reviewed:** every definer function the app can call checks its caller inside:
  - most read `auth.uid()` directly;
  - `create_party` hands off to `private.start_party`;
  - `pin_chat_file` uses `is_active_study_member` and `is_party_leader`.
  - None takes the acting user's id from the client: service-role functions take it from the Edge Function, which takes it from the verified token.
  - App roles can't create objects in `public`, so nothing can be planted for a function to find.
- **Check** (`scripts/security/check_db.sql`, S029): both queries return no rows:
  ```sql
  -- definer functions without an empty search_path
  SELECT n.nspname || '.' || p.proname FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
   WHERE p.prosecdef AND n.nspname IN ('public', 'private')
     AND NOT ('search_path=""' = ANY (coalesce(p.proconfig, '{}')));
  -- app-callable definer functions that never mention auth.uid() (each must be explained here)
  SELECT p.proname FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
   WHERE p.prosecdef AND n.nspname = 'public' AND has_function_privilege('authenticated', p.oid, 'EXECUTE')
     AND position('auth.uid()' IN p.prosrc) = 0 AND p.proname NOT IN ('create_party', 'pin_chat_file');
  ```
- **Test:** `reports_and_deletion.test.ts` and `privacy.test.ts` (account deletion): 14 of 14 pass.

### S019: limits on what phones and requests send
- **Audit:** every text, number and array column in `public` with no check constraint. The ones a phone or a request can fill now have limits (migration `20261019000115_input_constraints`):
  - **time zones** (`darood_entries.tz`, `study_completions.tz`, `study_sessions.tz`, `parties.week_tz`): an IANA name or offset, at most 64 characters;
  - **the AI counter's key** (`ai_proxy_usage.device_id`): at most 128;
  - **a file's checksum** (`chat_files.sha256`): 64 hex digits;
  - **chess positions:** at most 100 characters.
- **Already limited:**
  - usernames (3 to 20 characters, `[a-z0-9_]`, reserved words);
  - display names; chat bodies (2,000); file names (120);
  - item titles, subjects and notes; reminder titles and offsets;
  - Darood amounts and the daily cap; study minutes; reasons in reports.
- **Not needed:** server-computed columns (weekly places, points, the chess config).
- **Safe on old data:** each limit is enforced for new rows at once. Existing rows are validated, and if one doesn't fit, the limit stays enforced for new rows with a notice. Locally all eight validated.
- **Test:** `security.test.ts`, "what a phone sends is checked". A script tag, an SQL-looking string and a 65-character zone are refused for a Darood entry; a 200-character week zone for a party. The file, chess and AI proxy suites still pass (30 of 30).

### S020: rate limits
- **Done:** migration `20261019000116_rate_limits`:
  - `private.rate_events` and `private.rate_limit(bucket, max, per)`: they count the caller's events and raise `rate_limited` past the limit;
  - message reports and challenge reports: 30 an hour each;
  - the cloud export: 10 an hour;
  - push-token registration: 30 an hour.
- **Limits that already existed** (each tested in its own suite):

  | Action | Limit | Where |
  |---|---|---|
  | Invitations | 20 an hour per inviter | `invite_member`, `private.invite_log` |
  | Chat messages | 30 a minute | trigger on `chat_messages` |
  | Uploads | 20 a day; 20 MB a file; 500 MB a party | trigger on `chat_files`, `request-upload` |
  | Challenges | 10 an hour | `chess_create_challenge` |
  | Moves | 5 a second per game | trigger on `chess_moves` |
  | Watching | 20 joins an hour | `chess_watch` |
  | Profile reports | 1 a day | `reports` trigger |
  | AI | per account per day, and a global daily cap | `gemini-proxy` (S050) |
  | Sign-in emails, code checks | GoTrue's limits | `supabase/config.toml` `[auth.rate_limit]` (S061) |
- **Test:** `abuse.test.ts`, "reports, the export and push tokens are rate-limited":
  - the 11th export in an hour, the 31st push token and the 31st report of each kind are refused;
  - an hour later, the export works again.

### S021: no username harvesting
- **State and fix:**
  - Invitations give the same answer for a real username and a made-up one.
  - Usernames are never listed or searchable; only party mates see them.
  - The availability check, which must say whether a name is taken, needs a signed-in account and now stops at 60 an hour: migration `20261019000117_username_enumeration`.
- **Test:** `abuse.test.ts`, "usernames can't be harvested":
  - an invitation to a real name and to a made-up one get identical answers;
  - signed out, the check is refused (401);
  - the 61st check in an hour is refused;
  - `usernames.test.ts` still passes.

### S022: database test harness
- **Done:**
  - pgTAP through `supabase test db` (`supabase/tests/database/*.test.sql`), for catalogue rules;
  - the existing Node harness (`supabase/tests/local/*.test.ts`), for behaviour through the API with real players.
  - [supabase/tests/README.md](../../supabase/tests/README.md) explains both.
- **Check:** `npx supabase@2.120.0 test db` gives "All tests successful".

### S023: signed out, nothing
- **pgTAP** (`supabase/tests/database/010_anon.test.sql`):
  - `anon` holds no table, column or sequence privilege in `public`;
  - it can run no `public` or `private` function;
  - no bucket is public.
- **API** (`supabase/tests/local/rls_matrix.test.ts`): every public table and view is found from the catalogue, and each refuses a signed-out read (401, no rows) and write. Every RPC refuses a signed-out call. A new table or function is covered automatically.
- **Result 2026-10-22:** pgTAP PASS; API 2 of 2.

### S024: no player reaches another's rows
- **Test:** `rls_matrix.test.ts`, "a stranger reads, changes and deletes none of another player's rows, in any table".
- **Setup:** player A gets a row in every table that can hold a player's data (seeded through the app's own calls where triggers stamp the owner):
  - a study party with C and a Darood party;
  - an entry, an item, a tick, study time, a reminder, a message, a file and its link to the item;
  - chat reads, a weekly place and win, a goal;
  - a finished game with a move, a rating and a record; a challenge;
  - reports by and about A, a push token, a block, a watch and a profile report.
- **What it tries:** for every column that references a user (found from the foreign keys) and every `party_id`, stranger B's SELECT, PATCH and DELETE filtered by A's id or A's parties.
- **Result:** B never gets a row, and the row counts never change.
- **Coverage:** every such table was seeded, except `profiles` and `profile_progress_marks`, because A isn't on the board. Their rules are in `profiles_rls.test.ts`.

### S025: ids, roles, the leader and counters stay the server's
- **Test:** `rls_matrix.test.ts`, "nobody rewrites ids, roles, the leader or counters, even on their own rows". Each of these is refused, and the rows are unchanged afterwards:
  - **memberships:** a member making themselves leader or editing their membership; the leader editing a member's membership directly (removal is `remove_member`);
  - **the party:** a member taking it over; the leader resizing it;
  - **chess:** a player raising their Int points, rewriting a game's result or editing a head-to-head record;
  - **weekly results:** moving up a weekly ranking; adding win points;
  - **files:** an uploader marking their own file ready (only `finalize-upload` does, after its checks);
  - **ids:** handing a push token to another account; moving a profile to another id.
- **How:** these tables have no UPDATE grant for app roles, or column grants that leave the field out, or policies that pin the owner. Every change goes through a SECURITY DEFINER function that checks the caller.

### S026: leaving the board is immediate
- **Rule:** the SELECT policy shows other players only rows with `leaderboard_opt_in = true`, so the moment the flag is false the row is gone. There's no cache and no delay; rank counts use the same policy.
- **Test:** `profiles_rls.test.ts`, "leaving the board hides the row from others but not from its owner":
  - before, Dave sees Carol on the board and she counts (`/1`);
  - right after her opt-out, the next read is empty and the count is `/0`;
  - her stats are untouched, and she still sees her own row.

### S027: no file crosses parties
- **Rule:** every file call checks that the caller is an active member of the file's party:
  - `get-download-url` (60-second links), `finalize-upload`, `link_item_file`, `pin_chat_file` (leader), `delete_chat_message` (sender or leader);
  - `request-upload` checks the party it's uploading into;
  - storage itself refuses players outright (S016).
- **Tests:** `security.test.ts`:
  - "another party's chat, files and chess are out of reach": an upload into another party is refused;
  - "another party's files can't be fetched, finalised, linked, pinned or deleted": a stranger, and a member of a different party, get no link; finalising, linking, pinning and deleting are refused, and nothing changes; the file's own member isn't refused.
- **Tests:** `study_files.test.ts`: a removed member's link stops working, and links expire after 60 seconds.

### S028: the database tests run in CI
- **Done:** `.github/workflows/cloud-tests.yml` runs on every push and pull request to `main`. It starts the local stack and applies every migration, then:
  - runs `supabase db lint`;
  - runs the pgTAP rules (`supabase test db`, new);
  - serves the Edge Functions;
  - runs every Node test in `supabase/tests`;
  - runs the app's own repositories against the stack (`*LocalSupabase*`).
- **To block merges:** make "Cloud Tests (local Supabase)" a required check on `main` (GitHub → Settings → Branches). That's the owner's switch.
