# Supabase Schema (PixelQuest)

## Table: `public.profiles`

The `profiles` table stores cloud-synchronized leaderboard profiles for PixelQuest.

### Schema Columns

| Column Name | Data Type | Constraints | Default | Description |
|---|---|---|---|---|
| `id` | `UUID` | `PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE` | N/A | Unique identifier linked 1:1 to Supabase Auth user. |
| `display_name` | `TEXT` | `NOT NULL, CHECK (char_length(display_name) <= 20)`; unique ignoring case when not empty; the moderation trigger also requires `^[a-zA-Z0-9_]{3,20}$` and refuses offensive words (see "Display names" below) | `''` | Publicly visible leaderboard name. Separate from the local hero name and the sign-in email. |
| `current_streak` | `INTEGER` | `NOT NULL, CHECK (current_streak >= 0)` | `0` | Active daily streak count. |
| `longest_streak` | `INTEGER` | `NOT NULL, CHECK (longest_streak >= 0)` | `0` | All-time highest streak count. |
| `level` | `INTEGER` | `NOT NULL, CHECK (level >= 1)` | `1` | Current hero level. |
| `total_xp` | `INTEGER` | `NOT NULL, CHECK (total_xp >= 0)` | `0` | Total cumulative quest experience points. |
| `leaderboard_opt_in` | `BOOLEAN` | `NOT NULL`; only with a name (`on_the_board_with_a_name`, `20261011000002_named_board_rows.sql`) | `FALSE` | Explicit player opt-in to public leaderboard visibility. Defaults to OFF. Nobody is on the board without a name. |
| `updated_at` | `TIMESTAMPTZ` | `NOT NULL` | `timezone('utc'::text, now())` | Time of the latest write, by the server's clock. What the client sends is replaced, on a first insert too. |

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

## Plausible progress (`20261011000000_profile_progress_plausibility.sql`)

The trigger `keep_profile_progress_plausible` runs before every insert and update of a profile. It keeps the numbers within what the app's own rules allow, so the leaderboard no longer takes whatever a client sends.

The rules come from the app:
- **XP.** Every completed quest earns at least 50 XP, and a perfect day needs at least one. So:
  - the longest streak is at most `total_xp / 50`;
  - a level takes at least 3 perfect days, so the level is at most `1 + total_xp / 150`.
- **Streaks.** The current streak is never longer than the longest.
- **Pace.** Both allow two days of slack, for time zones and a nightly check that ran a night late:
  - a streak (current or longest) grows by at most one a calendar day since the streak last changed;
  - a level grows by at most one per 3 days since the level last changed, plus one sooner when progress carries over from a harder difficulty.
- **Caps.** The caps from the app's backup import: XP 10,000,000, level 999, streaks 36,500.

How the rules are applied:
- **Lowered, not refused.** A refusal would leave the app's sync retrying forever.
  - An honest catch-up, after nights the nightly check missed, arrives over the next syncs.
  - SYNC NOW says what the board shows for now.
  - Dropping back (RESET ALL PROGRESS, an older backup) is always accepted.
- **XP itself isn't checked further**, because the number of quests a player may have is unlimited.
  - A first upload is checked only against XP and the caps, since a player may have played offline for years before signing in.
  - So a client that invents completions can still climb, but only at the pace a real player could.
- **The service role is not limited.** That is the project's own server code and the test bench.

When each player's streak and level last changed is kept in `public.profile_progress_marks` (`id`, `streak_changed_at`, `level_changed_at`).
- Only the trigger reads and writes it. Players can't, so they can't move the times back to widen their allowance.
- RLS is on with no policies, and `anon` and `authenticated` have no privileges.
- It is deleted with the account.

`supabase/tests/local/profile_progress.test.ts` tests these rules.

## Display names (`20261010000000_display_name_word_matching.sql`)

The trigger `check_display_name_moderation` refuses a name that isn't 3-20 letters, digits or `_`, or that contains an offensive term. Unambiguous terms are refused anywhere in the name; words that are only offensive on their own (`tit`, `crap`, `dick`, ...) only as whole words, split at `_` and at lower-to-upper case changes, so `Titan_Slayer` passes and `DickHead` doesn't. Digits used as letters (`sh1t`) are read as letters, and stretched letters as one (`fuuuck`, `Titt`; `20261010000004_display_name_repeated_letters.sql`). The app's `DisplayNameModerator` follows the same rules; both are tested on `supabase/tests/display_names.json`.

**Unique names** (`20261011000001_unique_display_names.sql`). The rule:
- A partial unique index on `lower(display_name)` where the name isn't empty, so no two players share a name in any case.
- A name stays its owner's while their row exists, also while they're off the board. A rename or deleting the account frees it.
- Empty names (a linked profile that hasn't joined yet) don't count.

How the app handles it:
- Taking someone's name fails with `23505` (HTTP 409), and Account says the name is taken.
- Rows that already shared a name when the index was added: the least recently updated one kept it, and the others lost the name and left the board.

`supabase/tests/local/unique_names.test.ts` tests the rule.

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

**Limits** (`20261011000003_report_limits.sql`):
- **Once a day.** A player reports another player at most once a UTC day, through a unique index on reporter, reported player and day. A repeat fails with `23505` (HTTP 409), and the app says it's already reported.
- **Not yourself.** A player can't report themselves (`reports_not_yourself`, `23514`). The app shows no report button on your own row.
- **Server time.** `created_at` is the server's clock (trigger `stamp_report_time`), because a client-set date would get round the daily limit.

## Table: `public.ai_proxy_usage` (`20261006000000_ai_proxy_usage.sql`)

Daily Gemini call counts for the `gemini-proxy` Edge Function, which holds the Gemini API key so it never ships in the app. Since Day 31 only signed-in accounts can use the AI Coach, and calls are counted per account.

| Column Name | Data Type | Constraints | Description |
|---|---|---|---|
| `device_id` | `TEXT` | part of `PRIMARY KEY` | The counter's key: `user:<account id>` for a signed-in account, or `'*'` for the project-wide total. (The column name dates from when calls were counted per install.) |
| `day` | `DATE` | part of `PRIMARY KEY` | UTC day. |
| `calls` | `INTEGER` | `NOT NULL, CHECK (calls >= 0)` | Calls claimed that day. |

- `claim_ai_proxy_call(device, device_limit, global_limit)` claims one call. It returns `'ok'`, `'device_limit'` (the account's limit) or `'global_limit'`. Both counters move in one transaction, and a refused global claim gives the device's call back. The first call of a day deletes rows older than 30 days.
- Only the service role can use either one. RLS is on with no policies, and the function is not executable by `anon` or `authenticated`.

## Table: `public.usernames` (`20261019000000_usernames.sql`)

Darood Party usernames, one per account (docs/darood-design.md, Usernames). A table of its own, not a `profiles` column: opted-in profile rows are readable by every signed-in player, and usernames must not be listable.

| Column Name | Data Type | Constraints | Default | Description |
|---|---|---|---|---|
| `user_id` | `UUID` | `PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE` | N/A | The account. Deleting it frees the name. |
| `username` | `TEXT` | `NOT NULL`, `^[a-z0-9_]{3,20}$`, not reserved (`username_is_reserved`), unique ignoring case (`lower(username)`, `20261019000001_usernames_ignore_case.sql`) | N/A | The name others invite by. Lowercased on every write. |
| `changed_at` | `TIMESTAMPTZ` | `NOT NULL` | `now()` | When it was last set. |

- **Read:** the owner's own row only. Signed out, nothing.
- **Write:** no client writes at all (no policy, no grant). Server functions set it.

## Darood Parties (`20261019000004` to `20261019000023`)

Darood Party's server side (docs/darood-design.md). Every rule below is enforced here; the app only mirrors it. Tested in `supabase/tests/local/darood_parties.test.ts`.

### `public.parties`
| Column | Type | Rules |
|---|---|---|
| `id` | `UUID` | Primary key |
| `name` | `TEXT` | 2 to 40 characters, trimmed |
| `leader_id` | `UUID` | The current leader; emptied if that account is deleted, then passed on |
| `max_members` | `INTEGER` | 2 to 30, default 10 |
| `milestone` | `BIGINT` | Null, or above 0; when changed, above the current total, and never after finishing |
| `milestone_set_at` | `TIMESTAMPTZ` | Set by the server when the milestone changes |
| `completed_at` | `TIMESTAMPTZ` | Set once, when the total first reaches the milestone; never changes after |
| `created_at`, `deleted_at` | `TIMESTAMPTZ` | A deleted party is hidden from everyone |

### `public.party_members`
| Column | Type | Rules |
|---|---|---|
| `party_id`, `user_id` | `UUID` | One row per person per party; deleted with the party or the account |
| `role` | `TEXT` | `leader` (one per party, always active) or `member` |
| `status` | `TEXT` | `invited`, `active`, `declined`, `left` or `removed` |
| `invited_by`, `invited_at`, `joined_at`, `updated_at` | | |

Invited and active rows together never exceed the party's limit, nor 30 (`party_full`).

### `public.darood_entries`
| Column | Type | Rules |
|---|---|---|
| `party_id` | `UUID` | An open party (not finished or deleted) the poster is an active member of |
| `user_id` | `UUID` | Always the caller; emptied (entry kept) if the account is deleted |
| `amount` | `INTEGER` | 1 to 100,000; at most 500,000 a member a day in a party |
| `local_date`, `tz` | `DATE`, `TEXT` | A real time zone; today there, or up to 7 days back |
| `created_at` | `TIMESTAMPTZ` | The server's time |

### Views
- `party_stats`: per party, its total, entries, active members, milestone and finish. Active members only.
- `party_member_stats`: per person who posted, their total, active days and last day, with their username where the reader may see it.

### Who may do what
| | Signed out | Outsider | Invited | Active member | Leader |
|---|---|---|---|---|---|
| Read the party | no | no | yes | yes | yes |
| Read members, entries, stats | no | no | own row only | yes | yes |
| Post an entry (as themselves) | no | no | no | yes | yes |
| Edit or delete an entry | no | no | no | no | no |
| Rename, set the milestone | no | no | no | no | yes |
| Invite, remove, hand over the leadership | no | no | no | no | yes |

### Functions (signed-in players)
| Function | What it does |
|---|---|
| `create_party(name, max_members, milestone)` | A party and its leader, together; needs a username |
| `invite_member(party_id, username)` | Leader only; answers `sent` whether or not the name exists; 20 an hour per account |
| `respond_invite(party_id, accept)` | Accept or decline an unanswered invitation |
| `leave_party(party_id)` | Members leave; the leader hands over first |
| `remove_member(party_id, user_id)` | Leader only |
| `transfer_leadership(party_id, user_id)` | Leader only, to an active member; the leader stays as a member |
| `set_milestone(party_id, target)` | Leader only; above the current total |

Refusals carry a token in `message` (`party_full`, `not_leader`, `daily_limit`, `milestone_too_low` and so on), with HTTP 400 for broken rules and 403 for permissions.

