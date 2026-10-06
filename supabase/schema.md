# Supabase Profiles Schema Design

## Table: `public.profiles`

The `profiles` table stores cloud-synchronized leaderboard profiles for PixelQuest.

### Schema Columns

| Column Name | Data Type | Constraints | Default | Description |
|---|---|---|---|---|
| `id` | `UUID` | `PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE` | N/A | Unique identifier linked 1:1 to Supabase Auth user. |
| `display_name` | `TEXT` | `NOT NULL, CHECK (char_length(display_name) <= 20)`; the moderation trigger (`20260909000000_display_name_moderation.sql`) also requires `^[a-zA-Z0-9_]{3,20}$` | `''` | Publicly visible leaderboard name. Separate from local username and Google account name. |
| `current_streak` | `INTEGER` | `NOT NULL, CHECK (current_streak >= 0)` | `0` | Active daily streak count. |
| `longest_streak` | `INTEGER` | `NOT NULL, CHECK (longest_streak >= 0)` | `0` | All-time highest streak count. |
| `level` | `INTEGER` | `NOT NULL, CHECK (level >= 1)` | `1` | Current hero level. |
| `total_xp` | `INTEGER` | `NOT NULL, CHECK (total_xp >= 0)` | `0` | Total cumulative quest experience points. |
| `leaderboard_opt_in` | `BOOLEAN` | `NOT NULL` | `FALSE` | Explicit player opt-in to public leaderboard visibility. Defaults to OFF. |
| `updated_at` | `TIMESTAMPTZ` | `NOT NULL` | `timezone('utc'::text, now())` | Timestamp of latest sync. |

### Privacy Design Requirements
1. **Default Opt-In**: `leaderboard_opt_in` defaults to `FALSE`. An un-opted user's profile is never returned in public queries.
2. **PII Isolation**: Never store Google email, real names, or OAuth tokens in this table.
3. **Display Name**: The `display_name` is an independent pseudonym chosen by the user specifically for the leaderboard.

## Leaderboard ordering

`LeaderboardRepositoryImpl` reads opted-in rows only (`leaderboard_opt_in = TRUE`, enforced by row-level security for signed-in users).

- **Streaks**: `current_streak DESC, longest_streak DESC, id ASC`.
- **Levels**: `level DESC, total_xp DESC, id ASC`.
- `id` breaks exact ties, so every hero has a distinct position and the same position in every page.
- **Your rank** is 1 + the number of opted-in rows ordered ahead of you: higher first column, or equal first and higher second, or equal on both with a smaller `id`. The counts fetch only `id`.
- **Around you**: the app fetches the same ordered list with `range(rank - 1 - 3, rank - 1 + 3)`, which gives up to 3 heroes above you, you, and up to 3 below.
- **Indexes** (`20261003000000_leaderboard_sort_indexes.sql`): one per order, matching its columns and directions, partial on `leaderboard_opt_in = TRUE`. Apply it in the Supabase SQL editor or with `supabase db push`; the app works without it, only slower on a large table.

## Table: `public.ai_proxy_usage` (`20261006000000_ai_proxy_usage.sql`)

Daily Gemini call counts for the `gemini-proxy` Edge Function, which holds the Gemini API key so it never ships in the app.

| Column Name | Data Type | Constraints | Description |
|---|---|---|---|
| `device_id` | `TEXT` | part of `PRIMARY KEY` | A random id each install generates for AI calls (not linked to an account), or `'*'` for the project-wide total. |
| `day` | `DATE` | part of `PRIMARY KEY` | UTC day. |
| `calls` | `INTEGER` | `NOT NULL, CHECK (calls >= 0)` | Calls claimed that day. |

- `claim_ai_proxy_call(device, device_limit, global_limit)` claims one call. It returns `'ok'`, `'device_limit'` or `'global_limit'`. Both counters move in one transaction, and a refused global claim gives the device's call back. The first call of a day deletes rows older than 30 days.
- Only the service role can use either one. RLS is on with no policies, and the function is not executable by `anon` or `authenticated`.
