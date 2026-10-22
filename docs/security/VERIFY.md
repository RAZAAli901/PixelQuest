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

### S029: a check that fails the build on an open table or bucket
- **Done:**
  - `scripts/security/check_db.sql`: seven queries that must return nothing. They catch tables without RLS, public buckets, anything granted to `anon`, TRUNCATE, TRIGGER or REFERENCES grants, definer functions without an empty search path, owner-run views beyond the reviewed four, and writes through views.
  - `supabase/tests/database/020_rules.test.sql` runs the same rules in CI (S028), so a breach fails the build.
- **Check:** the script returns no rows locally, and pgTAP passes.
- **Proved it catches problems:** with a table created without RLS and a public bucket (in a rolled-back transaction), it reported `no RLS | zz_open` and `public bucket | zz-open`.
- **Hosted:** paste `scripts/security/check_db.sql` into the project's SQL editor after each push; it should return no rows.

### S030: RLS matrix
Who may do what, directly through the API, as of 2026-10-22. It's generated from the grants and policies; to refresh it, run the query in this section's history (commit "docs(security): RLS matrix").
- **`anon` (signed out):** nothing, on every table (S023).
- **`service_role`** (the Edge Functions and the dashboard): everything, and it bypasses RLS.
- **`authenticated`** (a signed-in player) is shown below. Each cell names the policy that decides which rows; "—" means no grant, so the action is refused outright.

Writes that have rules beyond "your own row" don't appear here. They go through SECURITY DEFINER functions that check the caller:
- **Party life:** `create_party`, `invite_member`, `respond_invite`, `leave_party`, `remove_member`, `transfer_leadership`, `delete_party`, `set_milestone`.
- **Darood and usernames:** `add_darood_entry`, `set_username`.
- **Chat and blocks:** `delete_chat_message`, `pin_chat_file`, `link_item_file`, `mark_chat_read`, `block_member`, `report_chat_message`.
- **Chess and push:** `chess_watch`, `report_chess_challenge`, `register_device_token`.
- **Export:** `export_my_data`.
- **Through the Edge Functions:** the chess moves and challenges, and the file uploads.

| Table | Read | Insert | Update | Delete |
|---|---|---|---|---|
| `ai_proxy_usage` | — | — | — | — |
| `chat_files` | Members read ready files, uploaders their own | — | — | — |
| `chat_messages` | Members read their study party's messages | Members send text as themselves | — | — |
| `chat_reads` | Members read their own read state | — | — | — |
| `chat_reports` | — | — | — | — |
| `chess_challenges` | Members read their party's challenges | — | — | — |
| `chess_config` | Anyone signed in reads the chess rules | — | — | — |
| `chess_games` | Members read their party's games | — | — | — |
| `chess_head_to_head` | Party mates read head-to-head | — | — | — |
| `chess_moves` | Members read their party's moves | — | — | — |
| `chess_ratings` | Party mates read ratings | — | — | — |
| `chess_reports` | — | — | — | — |
| `chess_time_controls` | Anyone signed in reads the time controls | — | — | — |
| `chess_watch_log` | — | — | — | — |
| `darood_entries` | Members read their party's entries | Active members post their own entries | — | — |
| `darood_week_results` | Members read their party's weeks | — | — | — |
| `device_tokens` | Owners read their tokens | — | — | Owners delete their tokens |
| `member_blocks` | Members read their own blocks | — | — | — |
| `parties` | Members read their party | — | The leader updates the party | — |
| `party_members` | Members read their party's members | — | — | — |
| `party_milestones` | Members read their party's goals | — | — | — |
| `profile_progress_marks` | — | — | — | — |
| `profiles` | Read opted-in profiles and your own | Insert your own profile | Update your own profile | Delete your own profile |
| `reports` | — | allow_insert_authenticated_reports | — | — |
| `study_completions` | Members read their study party's completions | Members complete items for themselves | — | Members undo their own completion within a day |
| `study_item_files` | Members read their items' materials | — | — | — |
| `study_items` | Members read their study party's items | Members add items to their study party | The creator or the leader edits an item | The creator or the leader deletes an item |
| `study_reminders` | Members read their study party's reminders | Members add reminders | Creators and leaders change reminders | Creators and leaders delete reminders |
| `study_sessions` | Members read their study party's sessions | Members log their own study time | — | — |
| `study_week_winners` | Members read their study party's winners | — | — | — |
| `usernames` | Read the usernames of people in a party with you; Read your own username | — | — | — |

- **Tests:** `rls_matrix.test.ts` (S023 to S025) proves this for strangers and owners across every table. `profiles_rls.test.ts`, `darood_parties.test.ts`, `study_chat.test.ts`, `security.test.ts`, `chess_rls.test.ts` and `abuse.test.ts` prove each policy's own rule.

## Phone

### S032: no database or tokens in the cloud backup
- **Done:**
  - `backup_rules.xml` and the `cloud-backup` section of `data_extraction_rules.xml` copy the app's own automatic backups (`files/auto-backups/`) and three settings files. They no longer copy the database, which caches other members' messages and names.
  - Phone-to-phone transfer (the player holding both phones) still moves the whole game.
  - The sign-in session is in neither, as before.
- **Restoring:** a phone restored from the cloud opens with an empty game and offers the newest automatic backup ("Restore your game?", `LostProgress`).
- **Test:** `AndroidBackupRulesTest`. Cloud gets exactly the automatic backups and the settings. Transfer gets those plus the database. No `exclude` rules are used (include-only), and the folder is the one `AutoBackups` writes. `LostProgressTest` covers the restore offer.

### S033: include-only backups, decided file by file
- **State:** both rule files list only `include` elements. A file not named is never copied, so a new store can't slip into a backup.
- **Guard:** `AndroidBackupRulesTest`:
  - "every preferences file is decided": each preferences file the code opens must be in the backed-up list or the device-only list, so a new one fails until someone decides.
  - "the session is never backed up": no rule names supabase-kt's preferences file, and no rule includes a whole domain.
- **Result 2026-10-22:** 7 of 7 pass.

### S034: the session sealed with the Android Keystore
- **Before:** supabase-kt kept the access and refresh tokens as plain JSON in the default preferences. They were private to the app and never backed up, but readable on a rooted phone or in a device dump.
- **Done:**
  - `auth/KeystoreSessionManager` stores the session sealed with AES-256-GCM in `pixelquest_session`. The key lives in the Android Keystore and can't be exported (`KeystoreCipher`).
  - On first load, a plain session from 1.7.2 or earlier is moved in and the plain copy deleted, so nobody is signed out by the update.
  - A session that can't be opened (for example the key is gone after a restore onto another phone) is dropped: the player signs in again.
  - `NetworkModule` gives it to the Supabase client before the client is built.
- **Tests:**
  - `KeystoreSessionManagerTest` (Robolectric, with a stand-in cipher): the stored value doesn't contain the tokens; it loads back; a plain session is moved in and the old copy deleted; an unopenable one is dropped; sign-out deletes both.
  - `KeystoreCipherInstrumentedTest` (Android 14 emulator, the real Keystore): it seals and opens with a fresh IV each time, and the key's material can't be read out.
  - `AndroidBackupRulesTest`: `pixelquest_session` is device-only.
- **Result 2026-10-22:** 4 of 4, 2 of 2 on the emulator, and 7 of 7.

### S035: database encryption, decided
- **Audit:** the Room database holds:
  - the player's own progress;
  - their parties' content: entries, items, chat messages and file names, games, members' usernames.
  - It holds **no secrets**: tokens are sealed outside it (S034). File links are fetched, used once in memory and never stored (`ChatFileOpener`). Chat drafts aren't saved.
- **Decision: no SQLCipher.**
  - The database is private to the app and encrypted at rest by Android's file-based encryption, unlocked by the screen lock.
  - Since S032 it never goes to a cloud backup, and since S036 sign-out clears the party caches and opened files.
  - SQLCipher would add a native library per CPU type, several MB, past the 8,000,000-byte APK budget, and slow every query, for no protection beyond what a rooted phone's owner already has.
  - Revisit if the app ever stores something secret, health data or payment data.
- **Guard:** `NoSecretsAtRestTest` fails if any Room entity gains a field named like a token, password, secret, key or signed link.

### S036: sign-out and deletion wipe the account's traces
- **On sign-out, and on account deletion (which signs out):**
  - **the session:** deleted in both its sealed and old stores (S034);
  - **party caches:** every party with its members, entries, items, ticks, study time, chat messages, file rows and chess games, through `DaroodAccountCleanup` (chat and chess cascade from parties). The study state kept outside the database goes too;
  - **the push token** ends on the phone (`PushRegistration`);
  - **AI lines:** cached AI-written lines are dropped (`AiSignOutCleanup`);
  - **opened files (new):** the chat files the player opened, other people's documents, are deleted from `cache/study-files` (`OpenedChatFiles`).
- **The same account signing in again keeps its things;** a different account clears them first.
- **Test:** `DaroodAccountCleanupTest`: 8 of 8, including "signing out deletes opened chat files; the same account keeps them".

## Network and components

### S037: https only, system CAs only
- **Done:**
  - `src/main/res/xml/network_security_config.xml`: cleartext is off, and the only trust anchors are the system's (no user-installed CAs).
  - The manifest uses it and sets `usesCleartextTraffic="false"`.
  - Debug builds replace it with `src/debug/res/xml/network_security_config.xml`: plain http only to `10.0.2.2`, `127.0.0.1` and `localhost`, for the local stack.
- **Test:** `NetworkSecurityConfigTest`: release is https-only with system CAs, has no cleartext domain and no debug overrides; the manifest uses the config; debug's cleartext covers exactly those three hosts.

### S038: certificate pinning for Supabase
- **Done:** `*.supabase.co` (database, Auth, Storage, Realtime and the Edge Functions, including the AI proxy) is pinned in both configs:
  - to the Google Trust Services roots and today's intermediate (the live chain is `supabase.co` ← WE1 ← GTS Root R4);
  - with backup pins for the other CAs Cloudflare issues from: Let's Encrypt (ISRG X1, X2), SSL.com and Sectigo.
  - The pins expire on **2027-06-30**, after which Android falls back to normal checks.
  - [PINNING.md](PINNING.md) explains how to check the chain and rotate.
- **Not pinned:** Firebase and Google sign-in, whose certificates Google rotates; they carry no PixelQuest data beyond ids.
- **Tests:**
  - `NetworkSecurityConfigTest`: both configs pin `supabase.co` with subdomains, include GTS Root R4, have at least 5 SHA-256 pins, and expire within two years.
  - `PinnedConnectionInstrumentedTest` (Android 14 emulator, the real hosted project): the TLS handshake succeeds with the pins in force.
- **Result 2026-10-22:** 4 of 4, and 1 of 1 on the emulator (not skipped).

### S040: BootReceiver isn't exported
- **Done:** `scheduling.BootReceiver` is `exported="false"`. It still checks that the action is one of its re-arm actions (boot, package replaced, exact-alarm permission, time or time zone changed).
- **Checked on the Android 14 emulator:**
  - **another app can't send to it:** `am broadcast -n com.pixelquest.app/.scheduling.BootReceiver` from the shell gives "Permission Denial: not allowed to send broadcast";
  - **the system still can:** after a reboot, Android queued and then delivered the boot broadcast to `com.pixelquest.app.scheduling.BootReceiver`, with none left pending. (An app that has never been opened gets no boot broadcasts at all; that's Android's "stopped" state.)
- **Test:** `ManifestComponentsTest`: of the app's own components, only `MainActivity` is exported.

### S041: no quest name on a locked phone
- **Before:** the "did you do it?" prompt opens over the lock screen (`showWhenLocked`, `turnScreenOn`) with the quest's name, readable by anyone holding the phone.
- **Done:** while the phone is locked (`KeyguardManager.isKeyguardLocked`, checked on every resume), the prompt shows only "A quest reminder" with an Unlock button.
  - On Android 8 and later, the button asks Android to unlock (`requestDismissKeyguard`), and the quest appears once it's unlocked.
  - On Android 7 the player unlocks the phone, and the prompt shows the quest when it comes back.
  - The answer buttons are behind the unlock too.
  - The notification that opens it is private on the lock screen (S047).
- **Test:** `LockedPromptTest`:
  - locked: the quest's name appears nowhere, and Unlock asks to unlock;
  - unlocked: the quest shows as before. `PromptQueueAnswerTest` and `PromptQueueTest` still pass.
- **To check on a phone:** set a screen lock, add a PROMPT-style quest due in a minute, lock the phone, and wait. The prompt says "A quest reminder", and the name shows after unlocking.

### S042: no new exported component without a reason
- **Done:** `scripts/security/check_manifest.py` reads the merged release manifest (the app's and its libraries'). Every exported component must have a permission, or be in its allowed list with a reason; today the list is only `MainActivity`. A component with intent filters and no `android:exported` fails too. CI runs it after the release build (`build.yml`, "Exported components").
- **Checked:** the real manifest passes (0 problems); with an exported receiver added, it reports "exported with no permission and no reason" and fails.
- **Test:** `ManifestComponentsTest` checks the same for the app's own manifest in the unit tests.

### S043: FLAG_SECURE on sensitive screens
- **Done:** `ui/components/SecureScreen` sets `FLAG_SECURE` while it's on screen, which blocks screenshots, screen recording and the recent-apps preview. Nested screens count, so the flag stays until the last one leaves. It's used on:
  - the account screen (email, the cloud data controls);
  - a study party's chat (private messages and files);
  - a rated chess game the player is playing (no screen sharing to a helper). Watching, friendly games and finished games stay open to screenshots.
- **Test:** `SecureScreenTest`: the flag is set while shown, held by an outer screen when an inner one leaves, cleared when both have gone, and not set when disabled. The chess, chat and account suites still pass.

### S044: no WebView
- **Checked:** no WebView, JavaScript setting or JavaScript interface anywhere in the app's code or layouts. Library manifests add none either (MANIFEST_AUDIT.md).
  - External links open in the phone's browser: only the Quran's sources page, with fixed https addresses.
  - Shared files open in a viewer app, through the FileProvider, read-only.
- **Test:** `NoWebViewTest` fails if a WebView or a JavaScript bridge appears.
