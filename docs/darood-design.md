# Darood Party: design

A Darood Party is a small group (2 to 30 people) that pools the Darood its members read. Each member posts how many they read as a number, the party sees one running total, and the leader sets a milestone for the party to reach. It is a friendly game only: it never gives XP and never touches levels, streaks or the leaderboard. Phases A–F of the Darood Party plan build it; this page is the design they follow.

## What it builds on (audit, Phase A)

### Supabase client
- **One client** (`SupabaseClientProvider`, `data/remote/SupabaseClient.kt`), with the Postgrest and Auth plugins only. `NetworkModule` provides the `SupabaseClient`, `Postgrest` and `Auth` to Hilt.
- **The URL and key** come from `BuildConfig` (`local.properties` or CI secrets). Without them the client points at a placeholder, and every call fails as a network error.
- **Every call is wrapped** in `safeSupabaseCall` (`SupabaseResult.kt`), which turns timeouts and errors into `SupabaseResult` (Success, NetworkError, AuthError, ServerError, UnknownError) with a 10-second limit.
- **No Realtime plugin yet.** Phase C adds it, for live entries.

### Sign-in
- **`AuthRepository`** (`auth/`): Google (an ID token exchanged with Supabase) and an emailed 6-digit code. `currentUser` is the signed-in account, and `serverUnreachable` says when its session can't be refreshed.
- **`AuthViewModel`** drives the Account screen (`Screen.Account`), which holds every sign-in button. Other features send signed-out players there: the AI Coach's and the Leaderboard's SIGN IN buttons navigate to `Screen.Account`.
- **Switching accounts** resets per-account choices (`CloudAccountLink`). Signing out clears the AI Coach's data (`AiSignOutCleanup`).

### The profile and the leaderboard
- **`public.profiles`**: one row per account, `id` = `auth.users.id` (deleted with the account).
  - Columns: `display_name`, `current_streak`, `longest_streak`, `level`, `total_xp`, `leaderboard_opt_in`, `updated_at`.
  - `display_name` is the public leaderboard name. It is unique ignoring case (a `lower(display_name)` unique index), and empty until the player joins the board.
  - Triggers keep streaks and levels plausible against the XP (`profile_progress_plausibility`).
- **Row-level security:** a signed-in player reads every opted-in row and their own, and writes only their own.
- **`CloudProfileDto`** is that row in the app. `CloudProfileRepository` upserts it when progress syncs (`ProfileSyncWorker`), and only sends totals.
- **`LeaderboardRepository`** reads the opted-in rows with `select *`, ranks them, and files reports (`public.reports`).
- **`delete_user_account()`** removes the profile and the `auth.users` row. Tables that reference `auth.users` with `ON DELETE CASCADE` go with it.
- **Tests:** the server's rules are tested against a local Supabase stack (`supabase/tests/local/*.test.ts`, docs/LOCAL_SUPABASE.md), on this PC and in CI (`cloud-tests.yml`).

### The side menu
- **`MenuItems`** (`ui/menu/MenuItem.kt`): each entry has an emoji icon, a label (and a Simple Mode label), a route in `Screen`, and `visibleWhen` over a `MenuContext`.
- **`MenuViewModel`** builds the header (the hero's name, avatar, level and progress) and the `MenuContext`.
- **The Namaz entry** shows while `SettingsRepository.namazAvailable` is on (a preference, on by default). Darood's entry follows the same pattern.

### What this means for Darood
- **Parties need an account.** Every rule on the server is checked against `auth.uid()`, so signed-out players see a sign-in prompt that opens the Account screen.
- **Usernames get their own table, not a `profiles` column.** Every signed-in player can read an opted-in profile row in full, and the leaderboard reads them with `select *`. A username there would list every board player's username, tied to their board name and numbers, to anyone signed in. Inviting by username (Phase B) is meant to reveal nothing about who exists. So `public.usernames` is readable only by its owner, and the availability check and invites go through server functions.
- **Darood has its own tables.** Nothing in it reads or writes `profiles`, XP, levels, streaks or the leaderboard.
