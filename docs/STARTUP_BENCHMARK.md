# Startup Benchmark & Baseline Profile Guide

PixelQuest utilizes Android Baseline Profiles and the Android Runtime (ART) Ahead-Of-Time (AOT) profile-guided compilation system to ensure rapid cold startup, zero initial frame jank, and smooth tab transitions.

---

## 1. Baseline Profile Overview

The profile definitions are maintained in `app/src/main/baseline-prof.txt` and packaged via `androidx.profileinstaller`.

### Critical Paths Covered
1. **Application & DI Initialization**:
   - `PixelQuestApplication` and Hilt component generation
   - `MainActivity` lifecycle methods (`onCreate`, `setContent`)
2. **Design System & Compose Theme**:
   - `PixelQuestTheme`, `PqColors`, `PqType`, `PqDimens`, `PqSkin`
   - Common UI components (`PqTopBar`, `PqCard`, `PqButton`)
3. **Navigation & Core Destinations**:
   - `PqNavGraph` route resolution
   - `HomeScreen`, `QuestsScreen`, `HeroScreen`, `NamazScreen`, `LeaderboardScreen`
4. **Data & Storage Layer**:
   - Room database initialization (`PixelQuestDatabase`)
   - Core DAOs (`TaskDao`, `UserProfileDao`, `NamazDao`)

---

## 2. Compilation Strategy

When the app is installed or updated on a physical device / emulator, Android executes:
```bash
cmd package compile -m speed-profile -f com.pixelquest.app
```
This pre-compiles classes listed with `HSPL` (Hot, Startup, Post-Startup) directly to native machine code in the DEX cache, bypassing the interpreter and eliminating runtime JIT pauses during initial user interaction.

---

## 3. Measuring Startup Latency

A benchmark harness script is available at `scripts/benchmark-startup.sh`.

### Manual Measurement
```bash
# 1. Force stop app
adb shell am force-stop com.pixelquest.app

# 2. Trigger speed-profile compilation
adb shell cmd package compile -m speed-profile com.pixelquest.app

# 3. Launch with timing output
adb shell am start-activity -W -n com.pixelquest.app/.MainActivity
```

Look for `TotalTime` in the command output (e.g., `TotalTime: 998`).

### Targets & Budgets
| Metric | Budget / Target | Measured Baseline (1.6.0+) |
|---|---|---|
| **Cold Startup (API 24 emulator)** | < 1,200 ms | ~1,000–1,050 ms |
| **Cold Startup (Physical Modern Device)** | < 800 ms | ~450–650 ms |
| **Warm Startup** | < 400 ms | ~200–300 ms |
| **Tab Switch Jank (99th percentile)** | < 60 ms | ~53 ms |
