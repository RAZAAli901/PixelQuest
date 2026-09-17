# PixelQuest — Simple Mode Architecture & Suppression Specification

## Overview
**Simple Mode** is a dedicated minimalist mode in PixelQuest designed for users who want clean, focused task tracking without gamification anxiety, scorekeeping, streaks, or celebration popups. 

Day 18 implements the underlying data and logic layer. Day 19 implements the visual UI suppression based on this document.

---

## Authoritative Suppression List

The following 5 core gamification systems are suppressed when Simple Mode is enabled (`simpleModeEnabled = true`):

### 1. Streak Display & Mechanics
- **Suppressed UI**:
  - Streak flame badge and count in `StreakXpSummaryStrip`.
  - Longest streak statistics and streak counter on `StatsScreen`.
  - "STREAK BROKEN" warning banners on `TodayScreen`.
  - Streak-related flavor text phrases.
- **Suppressed Copy**:
  - Notification reminders referencing streaks (e.g., "Keep your streak!").
  - Missed task notifications referencing streak breaks.
- **Underlying Logic**:
  - `StreakEvaluationWorker` continues calculating streaks and perfect days in Room DB. No streak data is deleted or frozen; it is simply not displayed.

### 2. Points & XP Display
- **Suppressed UI**:
  - XP counter badge in top bar and `StreakXpSummaryStrip`.
  - Points breakdown in task creation/edit dialogs (`PointsCalculator`).
  - Total XP and level progress bars on `ProfileScreen` and `StatsScreen`.
  - Points awarded toast / animation trigger upon task completion.
- **Underlying Logic**:
  - Points calculations and DB logging (`TaskCompletionLogEntity.pointsAwarded`, `UserProfileEntity.totalXp`) continue running silently.

### 3. Level Badges & Celebrations
- **Suppressed UI**:
  - Level badge displayed next to avatar.
  - "Level Up" celebration full-screen modal/dialog (`LevelUpCelebrationScreen`).
  - Level progression milestones on `ProfileScreen`.
- **Underlying Logic**:
  - `LevelCalculator` calculations continue updating `profile.level` in Room DB.
  - Celebration signal (`LevelUpSignalManager.pendingLevelUp`) is gated so celebration dialogs do not trigger in Simple Mode.

### 4. Difficulty Selection & Thresholds
- **Suppressed UI**:
  - Difficulty selection card on `SettingsScreen` and dedicated `DifficultyScreen`.
  - Difficulty tier badges.
- **Underlying Logic**:
  - Difficulty is locked to standard internal default (`DifficultyLevel.MEDIUM`, 70% threshold, 7 days per level).
  - Modification via `DifficultyViewModel` or data layer is rejected while Simple Mode is active.

### 5. Gamification Audio & Visual Flourishes
- **Suppressed Audio**:
  - Level-up celebration fanfare.
  - Points-awarded chime on task complete (standard neutral check sound remains).
- **Suppressed Visuals**:
  - CRT scanline filter overlay (`PixelCrtOverlay`) is forced off.
  - Confetti and celebration animations.

---

## Screen & Component Suppression Matrix (For Day 19)

| Screen / Component | Gamified Mode (Default) | Simple Mode (Day 19 Target) |
|---|---|---|
| `TodayScreen` Header | Shows streak, XP, level strip | Replaced with clean task count ("X of Y tasks completed") |
| `TodayQuestCard` | XP badge, difficulty indicator | Clean task item with checkbox and time |
| Quick Complete Action | Plays fanfare + XP toast | Immediate checkmark feedback, neutral audio |
| `DidYouDoItScreen` Prompt | "⚔️ DID YOU DO IT?", "YES!" | "Did you complete this task?", "Completed", "Not yet" |
| `ProfileScreen` | Level, XP bar, perfect days | Clean profile details, join date, task completion stats |
| `StatsScreen` | XP charts, streak graphs | Plain completion rate (%) and task completion tally |
| `SettingsScreen` | Full gamification + difficulty settings | Simple mode banner + difficulty locked indicator |
| `PixelCrtOverlay` | Active if toggled by user in Pixel theme | Strictly suppressed / forced OFF |

---

## Underlying Game Tracking Decision (Section B, Step 6)

### Decision: Keep Internal Tracking Running Continuously
**PixelQuest explicitly retains 100% active internal tracking of streaks, XP points, and player levels in the background while Simple Mode is enabled.**

### Architectural Rationale:
1. **Zero Data Loss on Mode Toggle**: If a user spends three weeks using Simple Mode and decides to return to Full Game Mode, all habit consistency, streak continuity, accumulated XP, and earned level milestones must remain intact. Disabling background tracking would reset or freeze player progression, creating severe friction and punishing users for toggling modes.
2. **Reversibility Guarantee**: Simple Mode is purely an un-gamified lens over the core PixelQuest task engine. The user's habit records remain faithful to their real-world task completions.
3. **Leaderboard Consistency**: A user in Simple Mode can still participate in the social leaderboard (see Section E) because their underlying XP and level continue calculating accurately without requiring an alternate ranking system.
4. **Implementation Contract**:
   - `StreakEvaluationWorker` runs nightly and evaluates daily completion logs without interruption.
   - `LevelCalculator` calculates level increments when requirements are met.
   - `TaskCompletionLogEntity` records calculated `pointsAwarded`.
   - The UI and audio layers simply suppress the visibility and celebration of these metrics.

---

## CRT Filter Suppression Decision (Section B, Step 11)

### Decision: Forced OFF While Simple Mode Is Active
**The CRT scanline filter overlay (`PixelCrtOverlay`) is strictly forced OFF whenever Simple Mode is enabled, regardless of whether the user previously enabled the CRT toggle in Settings.**

### Architectural Rationale:
1. **Aesthetic Conflict**: CRT scanlines are a heavy, nostalgic "retro arcade gamer" flourish. Simple Mode exists precisely to provide a clean, modern, calm task management interface. Scanlines over a minimalist list create visual dissonance and eye strain.
2. **Preference Preservation**: The underlying `isCrtEnabled` preference in `SettingsRepository` is NOT overwritten or mutated. If the user toggles Simple Mode OFF, their CRT setting immediately resumes its active state.
3. **Enforcement Mechanism**: The policy is codified in `CrtFilterPolicy.shouldApplyCrt(isCrtSettingEnabled, effectiveThemeMode, isSimpleModeEnabled)`. In `MainActivity.kt`, the overlay resolves this policy at compose time.

---

## Onboarding Strategy Decision (Section D, Step 20)

### Decision: Gamified-By-Default Onboarding (No Upfront Mode Choice)
**PixelQuest retains its canonical gamified onboarding flow on initial installation. Simple Mode is NOT presented as an upfront toggle during onboarding, but is discoverable in Settings.**

### Architectural Rationale:
1. **Preserving Brand Identity**: PixelQuest is fundamentally built around retro 8-bit arcade storytelling, avatar personalization, and quest adventures. New users should encounter and experience the app's full thematic identity first.
2. **Eliminating First-Launch Decision Fatigue**: Asking users during onboarding whether they prefer "Gamified Mode" or "Simple Mode" forces a premature architectural decision before they have experienced either mode.
3. **Onboarding Flow Integrity**: The Day 10 onboarding sequence (`NameEntry` -> `AvatarPick` -> `DifficultyPick` -> `Summary`) remains stable and focused.
4. **Discoverability**: Once users are in the app, the Simple Mode toggle in Settings provides a self-directed transition accompanied by the Step 19 explanatory dialog.

---

## Data Layer Contracts (Day 18)
1. `SettingsRepository.simpleModeEnabled`: StateFlow<Boolean> defaulting to `false`.
2. Instant reactivity: Changes emit across all ViewModels without requiring app restart.
3. No data loss: Switching between Simple Mode and Full Game Mode preserves 100% of historical progress.
