# PixelQuest ⚔️ Level Up Your Life

**PixelQuest** is a gamified daily task and habit tracker Android application built with **Kotlin** and **Jetpack Compose**.
Designed with a retro 8-bit arcade RPG aesthetic, PixelQuest turns daily productivity into an engaging quest.

---

## 🎮 Concept & Features
- **Gamified Habit & Task Tracking**: Earn XP, level up your character, and build streaks by completing daily real-world quests.
- **Retro 8-Bit Pixel UI**: Authentic pixel-art buttons, panel cards, progress bars, navigation icons, and custom typography powered by Google Fonts' *Press Start 2P*.
- **Interactive Retro Components**: `PixelButton` with press micro-animations, `PixelCard` containers, `PixelProgressBar`, and `PixelDialog` prompts.
- **Seamless Navigation Scaffold**: Timed animated splash screen transitioning into a 4-destination navigation shell (Home, Tasks, Stats, Profile).

---

## 🛠 Tech Stack & Architecture
- **Language**: Kotlin 1.9.23
- **UI Framework**: Jetpack Compose (Compose BOM `2024.02.01`, Material 3)
- **Typography**: Press Start 2P (SIL Open Font License)
- **Navigation**: Jetpack Navigation Compose (`2.7.7`)
- **Local Database**: Room Database (`2.6.1`)
- **Background Tasks & Dependency Injection**: WorkManager (`2.9.0`) & Dagger Hilt (`2.51`)
- **Image Loading & Async**: Coil Compose (`2.6.0`) & Kotlin Coroutines (`1.8.0`)
- **CI/CD**: GitHub Actions building debug APK and uploading workflow build artifacts

---

## 🚀 Current Status (Day 1 of 12)
- **Foundation & Pixel UI Kit Bootstrap Completed**
- **Atomic Progress**: 45 atomic git commits logged in `BRIEF.md`
- **Asset Attribution**: UI pack details logged in `ASSETS.md`
- **End-to-End Navigation Flow**: Animated Splash -> Bottom Nav Scaffold -> Home / Tasks / Stats / Profile placeholder screens.

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