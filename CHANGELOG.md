# 📜 PixelQuest Changelog

All notable changes to **PixelQuest** are documented in this file.

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
