# Supabase Schema (PixelQuest)

## Table: `public.profiles`

The `profiles` table stores cloud-synchronized leaderboard profiles for PixelQuest.

### Schema Columns

| Column Name | Data Type | Constraints | Default | Description |
|---|---|---|---|---|
| `id` | `UUID` | `PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE` | N/A | Unique identifier linked 1:1 to Supabase Auth user. |
| `display_name` | `TEXT` | `NOT NULL, CHECK (char_length(display_name) <= 20)`; the moderation trigger also requires `^[a-zA-Z0-9_]{3,20}$` and refuses offensive words (see "Display names" below) | `''` | Publicly visible leaderboard name. Separate from the local hero name and the sign-in email. |
| `current_streak` | `INTEGER` | `NOT NULL, CHECK (current_streak >= 0)` | `0` | Active daily streak count. |
| `longest_streak` | `INTEGER` | `NOT NULL, CHECK (longest_streak >= 0)` | `0` | All-time highest streak count. |
| `level` | `INTEGER` | `NOT NULL, CHECK (level >= 1)` | `1` | Current hero level. |
| `total_xp` | `INTEGER` | `NOT NULL, CHECK (total_xp >= 0)` | `0` | Total cumulative quest experience points. |
| `leaderboard_opt_in` | `BOOLEAN` | `NOT NULL` | `FALSE` | Explicit player opt-in to public leaderboard visibility. Defaults to OFF. |
| `updated_at` | `TIMESTAMPTZ` | `NOT NULL` | `timezone('utc'::text, now())` | Timestamp of latest sync. |

### Privacy Design Requirements
1. **Default Opt-In**: `leaderboard_opt_in` defaults to `FALSE`. An un-opted user's profile is never returned in public queries.
2. **PII Isolation**: Never store the sign-in email (Google or email code), real names, or tokens in this table.
3. **Display Name**: The `display_name` is an independent pseudonym chosen by the user specifically for the leaderboard.

## Row-level security (`20261010000002_profiles_policies_per_query.sql`)

All for signed-in players (`authenticated`); signed-out callers read nothing.

- **Read**: opted-in rows, and always your own (one policy).
- **Insert / update / delete**: your own row only (`id` = your account).
- `auth.uid()` is written `(SELECT auth.uid())`, so it's worked out once per query, not per row.
- `supabase/tests/local/profiles_rls.test.ts` checks these rules on a local Postgres (docs/LOCAL_SUPABASE.md).

## Display names (`20261010000000_display_name_word_matching.sql`)

The trigger `check_display_name_moderation` refuses a name that isn't 3-20 letters, digits or `_`, or that contains an offensive term. Unambiguous terms are refused anywhere in the name; words that are only offensive on their own (`tit`, `crap`, `dick`, ...) only as whole words, split at `_` and at lower-to-upper case changes, so `Titan_Slayer` passes and `DickHead` doesn't. Digits used as letters (`sh1t`) are read as letters. The app's `DisplayNameModerator` follows the same rules; both are tested on `supabase/tests/display_names.json`.

## Leaderboard ordering

`LeaderboardRepositoryImpl` reads opted-in rows only (`leaderboard_opt_in = TRUE`, enforced by row-level security for signed-in users).

- **Streaks**: `current_streak DESC, longest_streak DESC, id ASC`.
- **Levels**: `level DESC, total_xp DESC, id ASC`.
- `id` breaks exact ties, so every hero has a distinct position and the same position in every page.
- **Your rank** is 1 + the number of opted-in rows ordered ahead of you: higher first column, or equal first and higher second, or equal on both with a smaller `id`. The counts fetch only `id`.
- **Around you**: the app fetches the same ordered list with `range(rank - 1 - 3, rank - 1 + 3)`, which gives up to 3 heroes above you, you, and up to 3 below.
- **Indexes** (`20261003000000_leaderboard_sort_indexes.sql`): one per order, matching its columns and directions, partial on `leaderboard_opt_in = TRUE`. Apply it in the Supabase SQL editor or with `supabase db push`; the app works without it, only slower on a large table.

## Table: `public.reports` (`20260909000001_create_reports_table.sql`)

Players report offensive display names. A signed-in player can insert a report in their own name only; nobody but the project (service role, dashboard) can read them. Both foreign keys are indexed (`20261010000003_reports_foreign_key_indexes.sql`), and deleting an account deletes its reports.

## Table: `public.ai_proxy_usage` (`20261006000000_ai_proxy_usage.sql`)

Daily Gemini call counts for the `gemini-proxy` Edge Function, which holds the Gemini API key so it never ships in the app. Since Day 31 only signed-in accounts can use the AI Coach, and calls are counted per account.

| Column Name | Data Type | Constraints | Description |
|---|---|---|---|
| `device_id` | `TEXT` | part of `PRIMARY KEY` | The counter's key: `user:<account id>` for a signed-in account, or `'*'` for the project-wide total. (The column name dates from when calls were counted per install.) |
| `day` | `DATE` | part of `PRIMARY KEY` | UTC day. |
| `calls` | `INTEGER` | `NOT NULL, CHECK (calls >= 0)` | Calls claimed that day. |

- `claim_ai_proxy_call(device, device_limit, global_limit)` claims one call. It returns `'ok'`, `'device_limit'` (the account's limit) or `'global_limit'`. Both counters move in one transaction, and a refused global claim gives the device's call back. The first call of a day deletes rows older than 30 days.
- Only the service role can use either one. RLS is on with no policies, and the function is not executable by `anon` or `authenticated`.
