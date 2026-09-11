# 📜 PixelQuest Changelog

All notable changes to **PixelQuest** are documented in this file.

---

## [1.1.0] - 2026-09-12 — The Global Leaderboard Extension

### 🏆 Cloud Leaderboard & Community Competition (Days 13–15)
- **Supabase Cloud Infrastructure & Authentication (Day 13)**:
  - Integrated Supabase backend with Google Sign-In via Android Credential Manager and OAuth Web Client ID.
  - Strict privacy-first design: leaderboard participation defaults to OFF (`false`). Public display names are completely separate pseudonyms from local hero names and Google emails.
  - PostgreSQL Row-Level Security (RLS) policies enforcing write isolation (`auth.uid() = id`), opted-in public visibility (`leaderboard_opt_in = true`), and spectator access.
- **Live Arcade Leaderboard & Background Sync Worker (Day 14)**:
  - New `LeaderboardScreen` featuring dual arcade tabs: **Top Streaks** and **Top Levels**.
  - Gold, Silver, and Bronze podium styling for top 3 ranks, dynamic active player row highlight pulsing, pinned bottom user ranking card, and retro pull-to-refresh.
  - `ProfileSyncWorker` background sync engine executing via WorkManager on habit completion and level-ups, with `ConnectivitySyncObserver` for instant reconnect sync dispatch.
  - Non-intrusive **Spectator Mode** enabling signed-in players to browse rankings without publicly exposing their own stats.
- **Privacy, Moderation, Conflict Resolution & Release Hardening (Day 15)**:
  - **Defense-in-Depth Moderation**: Client-side profanity/offensive-word filter with leetspeak normalization (`DisplayNameModerator`), PostgreSQL trigger function constraint on server, and community report flagging action on leaderboard rows.
  - **Account & Cloud Data Deletion**: Dedicated "Delete My Cloud Data" flow with double-confirmation dialog sequence, removing `public.profiles` row, invoking `delete_user_account()` RPC to erase Supabase auth account, and clearing local cloud references while preserving local quest data.
  - **Leaderboard Opt-Out UX**: Clearly discoverable "Leave Leaderboard" action with lightweight single confirmation dialog and immediate post-opt-out confirmation notice.
  - **Multi-Device Sync Conflict Resolution**: Last-Write-Wins (LWW) by `updated_at` timestamp comparison paired with an anti-regression guard preventing sync from ever pushing lower streak, level, or XP values than what the server holds.
  - **Outage Graceful Degradation**: Zero-crash isolation in `ProfileSyncWorker` with WorkManager exponential backoff, 10-second timeout on leaderboard fetch calls, and subtle non-blocking "cloud sync unavailable" indicator in `AccountScreen`.
  - **Privacy Documentation**: Comprehensive `PRIVACY.md` detailing local-first data storage, cloud sync scope, and deletion rights, linked directly in `AccountScreen` prior to sign-in.

---

## [1.0.0-verified] - 2026-09-02
- Post-release audit and verification pass complete (39 steps, 613 total commits).
- Real-device sideload flow testing verified 100% pass across all 9 core user flows.

---

## [1.0.0] - Initial Public Release

### ⚔️ Core Experience & Architecture (Days 1–3)
- **Day 1**: Project foundation, Jetpack Compose 5-destination navigation scaffold (Home, Tasks, Stats, Profile, Avatar), custom *Press Start 2P* typography, authentic retro UI components (`PixelButton`, `PixelCard`, `PixelProgressBar`, `PixelDialog`).
- **Day 2**: Local data layer powered by Room Database (`TaskEntity`, `UserProfileEntity`, `DifficultySettingsEntity`, `StreakEntity`, `TaskCompletionLogEntity`). Clean architecture repository pattern with reactive `Flow` streams.
- **Day 3**: Interactive Task Management screens (`TasksScreen`, `CreateTaskScreen`, `EditTaskScreen`) with category selection (Fitness, Health, Knowledge, Creative, Mindset, General), priority levels, and recurrence rules.

### ⏰ Notifications, Streaks & Gamification (Days 4–6)
- **Day 4**: Background scheduling engine with `AlarmManager` exact alarms and `WorkManager` periodic background workers for missed task evaluations. "Did You Do It?" prompt dialogs.
- **Day 5**: Dynamic Streak & Multiplier System (`PointsCalculator`, `StreakCalculator`) scaling XP rewards by streak length and customizable difficulty thresholds (Easy 50%, Medium 70%, Hard 90%, Hardest 100%).
- **Day 6**: Leveling & Profile Progression system. Level calculation formula ($Level = 1 + \lfloor \frac{XP}{100} \rfloor$), Level-Up celebration modals, tier frames (Bronze, Silver, Gold), and historical level log history (`PixelLevelHistoryList`).

### 🎨 Retro Audio, Scanlines & Dashboard Polish (Days 7–9)
- **Day 7**: Custom 8-bit Sound Engine powered by `SoundPool` with persisted audio toggle. Retro CRT scanline & vignette shader filter toggleable across all app screens. 6 pixel character class sprites (Hero, Mage, Rogue, Warrior, Paladin, Ranger).
- **Day 8**: Today's Dashboard Screen with live countdown timers, quick-complete drag & tap interactions, daily progress ring, perfect day banners, and dynamic motivation flavor text.
- **Day 9**: Analytics & Stats Dashboard (`StatsScreen`). Interactive 90-day activity heatmap grid (`PixelCalendarHeatmap`), per-task completion rates, and completion rate trend charts.

### 💾 Backup Engine & Polish Pass (Days 10–11)
- **Day 10**: Data Export & Import engine (`DataExportImport`) supporting local JSON backup files. Full app progress reset sequence with confirmation steps.
- **Day 11**: Haptic feedback engine (`PixelHaptics`), snappy retro screen transitions (`PixelTransitions`), keyboard avoidance (`imePadding`), and complete accessibility compliance pass (screen reader semantics, min 48dp touch targets).

### 🚀 Production Release (Day 12)
- **Day 12**: Automated CI/CD release pipeline, R8 minification & resource shrinking, ProGuard keep rules, defensive database & JSON crash guards, end-to-end instrumented UI test suite, and initial v1.0.0 release publication.
