# PixelQuest ⚔️ Level Up Your Life

**PixelQuest** is a gamified daily task and habit tracker Android application built with **Kotlin** and **Jetpack Compose**.
Designed with a retro 8-bit arcade RPG aesthetic, PixelQuest turns daily productivity into an engaging quest.

---

## 🎮 Concept & Features
- **Gamified Habit & Task Tracking**: Earn XP, level up your character, and build streaks by completing daily real-world quests.
- **Retro 8-Bit Audio & Sound System**: Custom 8-bit sound effects (click, quest complete, missed, level-up fanfare) powered by `SoundPool` with persisted mute settings.
- **Avatar Sprite & Selection System**: Choose from 6 pixel character class sprites (Hero, Mage, Rogue, Warrior, Paladin, Ranger) with level-based tier frames (Bronze, Silver, Gold).
- **Retro CRT / Scanline Filter**: Optional real-time CRT scanlines and vignette overlay toggleable globally across all screens.
- **Retro 8-Bit Pixel UI Kit**: Authentic pixel-art buttons, panel cards, progress bars, navigation icons, 8-bit adaptive launcher icon, and custom typography powered by *Press Start 2P*.
- **Interactive Retro Components**: `PixelButton` with press micro-animations, `PixelCard` containers, `PixelProgressBar`, `PixelAvatarFrame`, `PixelDailyProgressRing`, and `PixelDialog` prompts.
- **Seamless Navigation Scaffold**: Timed animated splash screen transitioning into a 5-destination navigation shell (Home, Tasks, Stats, Profile, Avatar Selection).

---

## 🛠 Tech Stack & Architecture
- **Language**: Kotlin 1.9.23
- **UI Framework**: Jetpack Compose (Compose BOM `2024.02.01`, Material 3)
- **Audio Engine**: Android `SoundPool` with Hilt `SoundManager` singleton
- **Typography**: Press Start 2P (SIL Open Font License)
- **Navigation**: Jetpack Navigation Compose (`2.7.7`)
- **Local Database**: Room Database (`2.6.1`)
- **Background Tasks & Dependency Injection**: WorkManager (`2.9.0`) & Dagger Hilt (`2.51`)
- **Image Loading & Async**: Coil Compose (`2.6.0`) & Kotlin Coroutines (`1.8.0`)
- **CI/CD**: GitHub Actions building debug APK and uploading workflow build artifacts

---

## 📸 Finalized Visual Identity & Screenshots
*(Screenshots section — placeholders for app visual showcases)*
- `[Screenshot Placeholder: Home Screen with XP Progress Ring & Streak Flame]`
- `[Screenshot Placeholder: Profile Screen with Gold Tier Avatar Frame]`
- `[Screenshot Placeholder: Avatar Selection Grid]`
- `[Screenshot Placeholder: Retro CRT Scanline Filter Active]`

---

## 🚀 Current Status (Day 7 Complete)
- **Visual Identity Finalized**: 8-bit Sound System, Avatar Selection & Level Progression, Retro CRT Filter, Adaptive App Icon & Polish complete.
- **Progress Log**: All daily increments tracked line-by-line in `BRIEF.md`.
- **Asset Inventory**: Logged in `ASSETS.md`.

---

## 🔨 Build & Run Instructions

### Prerequisites
- JDK 17 or higher
- Android SDK (API Level 34 compile, API Level 24 minSdk)

### Command Line Build
```bash
# Build debug APK
./gradlew assembleDebug
```
Output APK location:
`app/build/outputs/apk/debug/app-debug.apk`