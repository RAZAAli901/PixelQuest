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

## The rules

The server enforces every one of these (constraints, triggers, row-level security and server functions, Phase B). The app mirrors them so a player hears "no" before sending anything, but never relies on that.

### Usernames
- **Who:** every signed-in player can claim one. Parties invite by username.
- **Form:** 3 to 20 characters: `a`–`z`, `0`–`9` and `_`. Stored in lowercase, so `Aria_K` is `aria_k`.
- **Unique,** ignoring case. Some words are reserved (`admin`, `support`, `pixelquest` and the like).
- **Changing it:** once every 30 days. Deleting the account frees it.
- **Private by default:** only its owner can read their own username. Others can check whether a name is free, and a leader can invite by it.

### Parties
- **Size:** a name of 2 to 40 characters, and 2 to 30 members. Whoever creates it is its leader, and it starts with them alone.
- **Joining:** the leader invites people by username, and each one accepts or declines. Invited and active members together never exceed the party's limit, and never 30.
- **Leaving:** a member can leave. The leader can remove a member, rename the party, hand the leadership to an active member, or delete the party. Entries from people who left or were removed stay in the total.

### Entries
- **Numbers only.** A member posts how many Darood they read, a whole number from 1 to 100,000. There is no free text anywhere, so nothing needs moderating.
- **At most 500,000 a day** per member in a party, to catch typos. The day is the member's own: each entry stores its local date and time zone.
- **Honour system.** Nobody checks the count itself.
- **Final:** an entry can't be edited or deleted. Only active members of a party that isn't finished or deleted can post.

### The total, contribution and consistency
- **One total:** every entry in a party adds to its one total.
- **What each member sees:** their own contribution (their total, their share, their place in the party), and how steady they are: days with at least one entry, the current streak and the best streak.

### The milestone and the race
- **The milestone:** the leader sets a target total, and can raise it later, but only above the current total.
- **Finishing:** the first time the total reaches the milestone, the party is finished, and that time is kept for good.
- **The race board:** parties that choose to join it are ranked by finish time, then by progress. It shows only a party's name, size, total, milestone, progress and finish time. It never shows members' names or entries.

### Kept apart from the game
- **Darood never gives XP,** and never touches levels, streaks, perfect days or the leaderboard. A test proves no Darood code reaches them.
- **Not in backups:** the party data is cached on the phone for speed and offline use. It isn't in the game's backups, and it's cleared on sign-out.
- **Never logged:** no entry, username or party detail is written to the log.

## Usernames and the feature's home (Phase A, as built)

### On the server (`supabase/migrations`, tested in `supabase/tests/local/usernames.test.ts`)
| Migration | What it adds |
| --- | --- |
| `20261019000000_usernames.sql` | `public.usernames` (`user_id`, `username`, `changed_at`), deleted with the account. The form check, the reserved words (`username_is_reserved`) and row-level security: the owner reads their own row, and clients can't write at all |
| `20261019000001_usernames_ignore_case.sql` | Every write lowercased by a trigger, and unique on `lower(username)` |
| `20261019000002_is_username_available.sql` | `is_username_available(name)`: yes or no, never whose. The caller's own name counts as free |
| `20261019000003_set_username.sql` | `set_username(name)`, the only way in: answers `ok`, `invalid`, `taken` or `too_soon`, with the name held and when it can change next (30 days after the last change). The row is locked, so two changes at once can't both get through |

Each migration can be run again, and each has a rollback note. None has been pushed to the hosted project yet: that's a deployment, done when the feature ships.

**What the availability check gives away:** a signed-in player can ask whether a name is taken, one name at a time. That much is needed for the live check. It never says whose a name is, and usernames can't be listed.

### In the app (`com.pixelquest.app.darood`)
- **`Username`:** the value class: parsing, normalising (`@Aria_K ` becomes `aria_k`) and the reserved words, mirroring the server.
- **`UsernameRepository`:** reads the account's own name (`fetchMyUsername`), checks a name (`isUsernameAvailable`) and saves one (`setUsername`), through Postgrest. Its DTOs are in `darood/data/remote`, which R8 keeps.
- **`UsernameSetupScreen`:** typing is shown in lowercase, the rules are explained as it's typed, and availability is checked once typing pauses for 400 ms. The server has the last word on Save. The route is `username_setup`.
- **Asking once:** `UsernamePromptViewModel` opens that screen for a signed-in account with no username, once per account on each phone (`pixelquest_darood` preferences), past the splash and onboarding. Its check runs at every launch and sign-in, and also refreshes the name the phone keeps.
- **Showing it:** `UsernameCache` keeps the last name the server gave, with its account. `MyUsername` gives the signed-in account's own, never another's. The side menu's header and Profile show `@name`, and tapping it on Profile opens the username screen.
- **The menu entry:** Darood Party (📿, with drawn bead icons in Pixel and Comic) opens `darood`. Signed out, it asks the player to sign in, and its button opens the Account screen. Signed in, it's a placeholder until the party list (Phase D).
- **The feature flag:** `SettingsRepository.daroodAvailable` (`DaroodFeatureFlag.DEFAULT_ON`). Off, there's no menu entry and nobody is asked for a username.

### Known gaps
- **Android 7:** `java.time`, here as across the app, needs core-library desugaring on API 24-25. That change is on its own branch.
- **No Settings switch for the flag yet,** as with `namazAvailable`.

