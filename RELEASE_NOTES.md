# PixelQuest v1.1.0 — The Global Leaderboard Extension

### 🏆 Cloud Leaderboard & Community Competition
- **Supabase Cloud Infrastructure & Authentication**:
  - Integrated Supabase backend with Google Sign-In via Android Credential Manager and OAuth Web Client ID.
  - Strict privacy-first design: leaderboard participation defaults to OFF (`false`). Public display names are completely separate pseudonyms from local hero names and Google emails.
  - PostgreSQL Row-Level Security (RLS) policies enforcing write isolation (`auth.uid() = id`), opted-in public visibility (`leaderboard_opt_in = true`), and spectator access.
- **Live Arcade Leaderboard & Background Sync Worker**:
  - New `LeaderboardScreen` featuring dual arcade tabs: **Top Streaks** and **Top Levels**.
  - Gold, Silver, and Bronze podium styling for top 3 ranks, dynamic active player row highlight pulsing, pinned bottom user ranking card, and retro pull-to-refresh.
  - `ProfileSyncWorker` background sync engine executing via WorkManager on habit completion and level-ups, with `ConnectivitySyncObserver` for instant reconnect sync dispatch.
  - Non-intrusive **Spectator Mode** enabling signed-in players to browse rankings without publicly exposing their own stats.
- **Privacy, Moderation, Conflict Resolution & Release Hardening**:
  - **Defense-in-Depth Moderation**: Client-side profanity/offensive-word filter with leetspeak normalization (`DisplayNameModerator`), PostgreSQL trigger function constraint on server, and community report flagging action on leaderboard rows.
  - **Account & Cloud Data Deletion**: Dedicated "Delete My Cloud Data" flow with double-confirmation dialog sequence, removing `public.profiles` row, invoking `delete_user_account()` RPC to erase Supabase auth account, and clearing local cloud references while preserving local quest data.
  - **Leaderboard Opt-Out UX**: Clearly discoverable "Leave Leaderboard" action with lightweight single confirmation dialog and immediate post-opt-out confirmation notice.
  - **Multi-Device Sync Conflict Resolution**: Last-Write-Wins (LWW) by `updated_at` timestamp comparison paired with an anti-regression guard preventing sync from ever pushing lower streak, level, or XP values than what the server holds.
  - **Outage Graceful Degradation**: Zero-crash isolation in `ProfileSyncWorker` with WorkManager exponential backoff, 10-second timeout on leaderboard fetch calls, and subtle non-blocking "cloud sync unavailable" indicator in `AccountScreen`.
  - **Privacy Documentation**: Comprehensive `PRIVACY.md` detailing local-first data storage, cloud sync scope, and deletion rights, linked directly in `AccountScreen` prior to sign-in.
