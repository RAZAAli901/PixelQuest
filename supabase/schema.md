# Supabase Profiles Schema Design

## Table: `public.profiles`

The `profiles` table stores cloud-synchronized leaderboard profiles for PixelQuest.

### Schema Columns

| Column Name | Data Type | Constraints | Default | Description |
|---|---|---|---|---|
| `id` | `UUID` | `PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE` | N/A | Unique identifier linked 1:1 to Supabase Auth user. |
| `display_name` | `TEXT` | `NOT NULL, CHECK (char_length(display_name) >= 3 AND char_length(display_name) <= 20)` | `''` | Publicly visible leaderboard name. Separate from local username and Google account name. |
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
