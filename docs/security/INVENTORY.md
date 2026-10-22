# Inventory

Everything PixelQuest's security depends on, as of 2026-10-22 (S002). Generated from the local stack, which has the same migrations as the hosted project (through `20261019000108`). Rerun the queries in [VERIFY.md](VERIFY.md) to refresh it.

## Database tables (schema `public`)

RLS is on for every table. A table with RLS on and no policy is deny-all to app users: only server functions (SECURITY DEFINER) and the service role touch it.

| Table | RLS | Policies | Holds |
|---|---|---|---|
| `ai_proxy_usage` | on | 0 | AI calls counted per account and day (written by `gemini-proxy` only) |
| `chat_files` | on | 1 | Shared files' metadata |
| `chat_messages` | on | 2 | Study chat messages |
| `chat_reads` | on | 1 | Each member's last-read time |
| `chat_reports` | on | 0 | Message reports (service role only) |
| `chess_challenges` | on | 1 | Challenges |
| `chess_config` | on | 1 | Chess limits (read-only) |
| `chess_games` | on | 1 | Games |
| `chess_head_to_head` | on | 1 | Pair records |
| `chess_moves` | on | 1 | Moves |
| `chess_ratings` | on | 1 | Int points |
| `chess_reports` | on | 0 | Challenge reports (service role only) |
| `chess_time_controls` | on | 1 | The allowed time controls (read-only) |
| `chess_watch_log` | on | 0 | Who joined which game to watch; one day |
| `darood_entries` | on | 2 | Darood counts |
| `darood_week_results` | on | 1 | Weekly places |
| `device_tokens` | on | 2 | Push tokens |
| `member_blocks` | on | 1 | Block lists |
| `parties` | on | 2 | Parties |
| `party_members` | on | 1 | Memberships and invitations |
| `party_milestones` | on | 1 | Goal history |
| `profile_progress_marks` | on | 0 | When streak and level last changed (plausibility checks) |
| `profiles` | on | 4 | Leaderboard rows (opt-in) |
| `reports` | on | 1 | Display-name reports |
| `study_completions` | on | 3 | Ticks |
| `study_item_files` | on | 1 | Files pinned to study items |
| `study_items` | on | 4 | Assignments, quizzes |
| `study_reminders` | on | 4 | Reminders |
| `study_sessions` | on | 2 | Study time |
| `study_week_winners` | on | 1 | Weekly winners |
| `usernames` | on | 2 | Usernames |

`private` schema (not reachable through the API): `invite_log`, `study_weeks_closed`, and helper functions.

## Views (schema `public`)

| View | Runs as | Note |
|---|---|---|
| `party_stats`, `party_member_stats`, `party_day_stats`, `study_week_scores`, `study_chat_storage`, `chess_head_to_head_totals`, `chess_party_leaderboard`, `chess_profile_stats` | the reader (`security_invoker`) | The underlying tables' RLS applies |
| `race_board` | the owner, on purpose | Only parties whose leader opted in; name, member count and progress only |
| `chess_config`-style constants: `study_chat_config`, `study_chat_types`, `study_points_config` | the owner | Fixed limits; no user data |

## Storage

| Bucket | Public | Size limit | Access |
|---|---|---|---|
| `study-files` | no | 20 MB | Uploads through `request-upload`; downloads through 60-second signed links from `get-download-url` |

## Functions

- **Postgres:** 72 in `public` (RPCs and triggers), 29 in `private`. Every SECURITY DEFINER function sets `search_path = ''` (checked by the query in VERIFY.md).
- **Edge Functions (all deployed with `--no-verify-jwt`; each verifies the caller's token itself):**
  - AI: `gemini-proxy`;
  - files: `request-upload`, `finalize-upload`, `get-download-url`, `cleanup-uploads`;
  - chess: `chess-create-challenge`, `chess-respond-challenge`, `chess-move`, `chess-resign`, `chess-draw`, `chess-claim-timeout`, `chess-sweep`, `chess-live-games`;
  - push: `send-push`.

## Keys and secrets

| Key | Where it is | Who can see it |
|---|---|---|
| Supabase publishable (anon) key | The APK (`BuildConfig.SUPABASE_ANON_KEY`); `local.properties`; GitHub secret `SUPABASE_ANON_KEY` | Public by design: RLS is the protection |
| Supabase service role key | Supabase function environment only | The functions; the owner's dashboard |
| Gemini API key | Supabase function secret `GEMINI_API_KEY`; the owner's `local.properties` for debug builds | The proxy. **Also a GitHub secret `GEMINI_API_KEY` that no workflow uses** (see [ROTATE.md](ROTATE.md)) |
| FCM service account (private key) | Supabase function secret `FCM_SERVICE_ACCOUNT` | `send-push` and the chess functions |
| Firebase app settings (`FCM_*`) | `local.properties`; GitHub secrets | Public by design (they're in every APK) |
| Google web client ID | `local.properties`; GitHub secret | Public by design |
| Release keystore and passwords | The owner's machine; GitHub secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` | The release workflow |
| Supabase access token (CLI) | The owner's machine (Windows credential store) | Pushing migrations, deploying functions |

## Third-party services

| Service | Used for | Data it sees |
|---|---|---|
| Supabase (hosted) | Auth, database, storage, functions, Realtime | Everything in the tables above |
| Google AI (Gemini) | The AI Coach, through `gemini-proxy` | An anonymous habit summary (no task names, no account ids) |
| Firebase Cloud Messaging | Chess challenge notifications | Push tokens and ids only; no names or text |
| Google Sign-In | Signing in | The Google account's ID token, checked by Supabase |
| GitHub (Actions, Releases) | CI, signed releases | Source, build secrets |
| Tanzil Project | The Quran text, bundled at build time | Nothing at run time |
