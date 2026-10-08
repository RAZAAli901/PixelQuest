# 📜 PixelQuest Changelog

All notable changes to **PixelQuest** are documented in this file.

---

## [Unreleased] — Themes, Simple Mode, AI Coach and a Correctness Pass (Days 16–30)

### 🎨 Themes
- **Light mode** (Days 16–17): a warm daylight palette, with every screen, chart and dialog adapted and checked for contrast.
- **Comic mode** (Days 20–23, 27–28): a pop-art theme with ink borders, halftone panels and comic versions of every component, unlocked for everyone. Headings use a darker coral ink so they read on paper.

### 🧘 Simple Mode (Days 18–19)
- An un-gamified view: quests become tasks, and XP, levels, flourishes and celebrations are hidden. Streaks, perfect days and levels keep counting in the background with your current difficulty.

### 🤖 AI Coach (Days 24–30)
- Optional, off by default: Google Gemini writes short habit debriefs and daily reminder lines from anonymous stats. Task names never leave the device.
- Cached for 12 hours, at most one live call per 6 hours, and capped at 4 calls a day and 60 a month.
- Released builds reach Gemini through PixelQuest's own server, which holds the key. The key is never inside the APK.

### 🔔 Reminders (Days 26–28, 31)
- Reminder lead times and styles, a "Did you do it?" prompt, missed-quest notices, and snooze.
- One result per quest per day: double taps and late answers no longer count twice.
- Two quests due in the same minute both get their prompt.
- An evening nudge (around 19:00) when your streak is at risk, saying how many more quests keep it (Day 31).
- Reminder fixes (Day 31):
  - Snoozing one quest no longer cancels another's reminder.
  - Answering a late-evening reminder after midnight counts for the right day.
  - A reminder can't ring twice for the same day after the app restarts.
  - Reminders follow you across time zones.
  - A deleted quest's leftover reminder can't award XP.

### 🗓️ Quests and progress
- Weekly quests on chosen days (Day 27).
- Days per level are 3 / 7 / 14 / 30 for Easy, Medium, Hard and Hardest (Day 29).
- A new quest created after its time of day starts tomorrow (Day 30).
- The task form keeps your edits when the screen rotates (Day 30).
- Backups now include the streak, completion history and level timeline (Days 28, 30).
- Backups are checked when imported (Day 31). A file that isn't a backup says so. Out-of-range values in an edited file are corrected instead of crashing the app. Settings now says whether a backup was saved or restored.
- Leaving the leaderboard while offline is queued until you're back online (Day 30).

### ♿ Accessibility (Days 30–31)
- TalkBack reads full day names, which chips, difficulty cards and history filters are selected, heatmap days with their results, and named close, back, delete and refresh buttons.

### ☁️ Builds without cloud or AI (Day 31)
- A build without a Supabase project says sign-in, sync and the leaderboard aren't set up, instead of offering a sign-in that can't work. A build that can't reach Gemini doesn't offer the AI Coach.

### ⚠️ Upgrading from 1.1.0 or earlier
- From this release on, PixelQuest is signed with a permanent release key. Earlier releases were each signed with a temporary key, so Android won't install this one over them.
- Before upgrading, open Settings → **EXPORT QUEST DATA** and save the backup. Then uninstall the old app, install this one, and restore the backup with Settings → **IMPORT QUEST DATA**.

### Release notes
- Releases built without the signing or Supabase secrets now say so at the top of their notes.
- Tagged releases are checked against the release key's certificate before they're published.

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
