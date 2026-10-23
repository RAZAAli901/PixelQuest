# ⚔️ PixelQuest — Level Up Your Life

[![Download APK](https://img.shields.io/badge/Download-Latest%20APK-brightgreen?style=for-the-badge&logo=android)](https://github.com/RAZAAli901/PixelQuest/releases/latest/download/app-release.apk)
[![GitHub Release](https://img.shields.io/badge/GitHub-Releases-blue?style=for-the-badge&logo=github)](https://github.com/RAZAAli901/PixelQuest/releases)
[![Version](https://img.shields.io/badge/Release-v1.8.0-orange?style=for-the-badge)](https://github.com/RAZAAli901/PixelQuest/releases/tag/v1.8.0)
[![License](https://img.shields.io/badge/License-MIT-purple?style=for-the-badge)](LICENSE)

![PixelQuest banner: Home and Deen in the Comic skin, Quests and Hero in the Pixel skin](docs/screenshots/banner.png)

**PixelQuest** is a production-grade, gamified daily task, habit tracker, and spiritual lifestyle Android application built with **Kotlin** and **Jetpack Compose**. It transforms real-world routines into RPG quests across two switchable design skins: **Comic** and **Pixel**.

Built with an offline-first **Clean Architecture** (MVI/MVVM + UDF), it integrates local Room persistence, Dagger Hilt dependency injection, background synchronization via WorkManager, Tanzil Uthmani Quran scripture reading, offline Namaz prayer timings, and optional cloud multiplayer features (Darood counting parties, study rooms, and server-refereed chess) powered by Supabase.

---

## ⚡ Direct Download

Download the latest signed release APK onto your Android device:

👉 **[📥 Direct Download app-release.apk (v1.8.0)](https://github.com/RAZAAli901/PixelQuest/releases/download/v1.8.0/app-release.apk)** 👈

Or browse releases and checksums on the **[📦 GitHub Releases Page](https://github.com/RAZAAli901/PixelQuest/releases)**.

> **Compatibility**: Android 7.0 (API level 24) through Android 15 (API level 35).  
> **Upgrades**: Backward compatible across all versions. Safe in-place upgrades keep your local database, streaks, and preferences intact.

---

## 🏗️ Architecture & Engineering Blueprint

PixelQuest follows an inward-pointing 5-tier **Clean Architecture** combined with **Unidirectional Data Flow (UDF)**:

```
┌─────────────────────────────────────────────────────────────┐
│                          UI Layer                           │
│        (Jetpack Compose Composables, Theme & Design Tokens) │
└──────────────────────────────┬──────────────────────────────┘
                               │ StateFlow / User Events
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                     Presentation Layer                      │
│     (Hilt ViewModels, SavedStateHandle, UiState & Effects)  │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                        Domain Layer                         │
│     (Use Cases, Domain Models, Repository Contracts)        │
└──────────────────────────────▲──────────────────────────────┘
                               │ Implements Contracts
                               │
┌─────────────────────────────────────────────────────────────┐
│                         Data Layer                          │
│     (Repository Implementations, Entity/DTO Mappers, Cache) │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                     Data Sources Layer                      │
│  (Room SQLite DB, DataStore, Supabase Ktor Client, Workers) │
└─────────────────────────────────────────────────────────────┘
```

### Key Architectural Tenets
1. **Unidirectional Data Flow (UDF)**: Each screen is driven by a single immutable `StateFlow<UiState>`. Composables are purely reactive and stateless where possible.
2. **Process Death & Rotation Survival**: ViewModels utilize `SavedStateHandle` to preserve transient inputs, search queries, and form drafts across configuration changes and process termination.
3. **Decoupled Business Logic**: The Domain layer has zero dependencies on Android SDK classes or database annotations, enabling sub-second JVM unit testing.
4. **Offline-First Resilience**: Room SQLite serves as the single source of truth. Remote synchronization happens asynchronously via WorkManager without blocking the UI thread.
5. **Centralized Design System**: Spacing, radii, and typography are unified into `PqDimens`, `PqColors`, and `PqType`, allowing seamless runtime switching between `Comic` and `Pixel` skins.

For detailed design rationale, see the [Architecture Decision Records (ADRs)](#-architecture-decision-records-adrs).

---

## 🛠️ Technology Stack

| Layer / Concern | Technology |
|---|---|
| **Language** | Kotlin 2.0+ (100% Kotlin codebase) |
| **UI Framework** | Jetpack Compose, Material 3, Custom Design Tokens |
| **Architecture** | Clean Architecture, MVI / MVVM, UDF |
| **Dependency Injection** | Dagger Hilt |
| **Local Database** | Room Database (SQLite) with versioned migrations |
| **Key-Value Storage** | AndroidX DataStore (Preferences) |
| **Asynchronous & Streams** | Kotlin Coroutines, StateFlow, SharedFlow |
| **Background Processing** | AndroidX WorkManager, Exact Alarm Manager |
| **Cloud & Backend** | Supabase (PostgreSQL with RLS, Auth, Realtime, Storage) |
| **AI Integration** | Gemini API via server-side secure proxy |
| **AOT & Startup Performance**| Android Baseline Profiles (`androidx.profileinstaller`), R8 full-mode shrinking |
| **Testing** | JUnit 4, Robolectric, Compose Test Rule, CashApp Turbine, MockK |
| **Code Hygiene** | Spotless (ktlint), Detekt, Android Lint |

---

## 🎮 Core Features

- **Gamified Quest System**: Complete daily and recurring habits, earn XP, maintain streaks, and level up your hero avatar across customizable difficulty modes.
- **Dual Visual Skins**: Switch instantly between **Comic** (rounded ink aesthetic, bold drop shadows, Bangers headers) and **Pixel** (retro 8-bit styling, stepped shadows, Press Start 2P headers).
- **Hero Dashboard & Analytics**: Inspect your hero tier (Bronze to Diamond), 17-week activity heatmap, 4-week completion trend charts, and milestone logs.
- **Global & Social Leaderboards**: Opt-in global leaderboard with Google Sign-In, ranking users by active streaks and total levels while honoring strict privacy controls.
- **Deen Hub (Spiritual Lifestyle)**:
  - **Offline Namaz Tracker**: Accurate prayer timetable calculated completely offline on-device based on astronomical coordinates, with start-of-prayer notifications and optional XP rewards.
  - **Tanzil Holy Quran**: Embedded Uthmani Arabic scripture and Saheeh International translation, featuring instant search, juz navigation, bookmarks, and sepia reader themes.
  - **Darood Counting Parties**: Real-time collaborative counting rooms for friends with live progress sync, individual share breakdowns, goal milestones, and weekly ranking summaries.
- **Study Parties & Real-time Chess**:
  - Collaborative group checklists, study timers, file attachments (up to 20 MB), and group reminders.
  - **Server-Refereed Live Chess**: 1 to 10 minute blitz clocks, ELO-inspired ratings, ladder ranks, direct board routing on challenge and rematch acceptance, match exit protection guard (`LeaveGameGuard`), clean Accept/Decline challenge sheets, and live spectator presence tracking.
- **Custom Comic Notification Design & Notification Channels (`PqNotify`)**: RemoteViews comic cards with kickers, headers, skin-adaptive backings (Gold, Paper, Sky), dynamic progress bars, lock-screen privacy guards, quiet-hours filtering, and 10 categorized notification channels (Prayer, Quests due, Streaks, Rewards, AI Coach, Daily plans, Darood parties, Study parties, Chess, and Live timers).
- **Animated Pix Mascot & Moody App Icons (`PqMascot`)**:
  - **Animated Mascot Logo**: Responsive 24×24 vector mascot featuring 13 distinct moods (Happy, Celebrate, LevelUp, OnFire, Worried, Sad, Sleepy, Focus, Challenge, etc.) with real-time micro-animations (bobs, jumps, binks, flame flickers) showcased on the splash screen, Home header, empty day cards, and level-up celebrations.
  - **Moody Launcher App Icons**: Dynamic launcher activity-aliases (`.LauncherPixHappy`, `.LauncherPixOnFire`, `.LauncherPixWorried`, `.LauncherPixSad`, `.LauncherPixSleepy`) that adapt your device's home screen icon to your current streak and habits.
- **Simple Mode**: One-tap toggle that strips away game mechanics (XP, levels, ranks) for a minimalist habit checklist experience.
- **Versioned Backup & Restore**: Export and import complete encrypted JSON backups across devices.

---

## 📂 Project Directory Structure

```
PixelQuest/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/pixelquest/app/
│   │   │   │   ├── audio/              # Sound effects and haptic feedback manager
│   │   │   │   ├── auth/               # Account authentication and cloud profiles
│   │   │   │   ├── chess/              # Real-time chess engine, game session, rules & UI
│   │   │   │   ├── darood/             # Darood counting parties, goals & weekly rankings
│   │   │   │   ├── data/               # Room entities, DAOs, repositories, DTO mappers
│   │   │   │   ├── di/                 # Dagger Hilt dependency injection modules
│   │   │   │   ├── domain/             # Pure business models, use cases, repository contracts
│   │   │   │   ├── namaz/              # Offline astronomical prayer timing engine
│   │   │   │   ├── party/              # Collaborative party base architecture and sync
│   │   │   │   ├── quran/              # Scripture reader, Tanzil text engine & search
│   │   │   │   ├── study/              # Study parties, shared task lists & timer rooms
│   │   │   │   ├── ui/                 # Jetpack Compose screens, components, themes & navigation
│   │   │   │   └── worker/             # WorkManager sync and notification tasks
│   │   │   ├── baseline-prof.txt       # ART ahead-of-time pre-compilation rules
│   │   │   └── res/                    # Vector drawables, fonts, strings, layouts
│   │   ├── test/                       # Fast JVM unit, ViewModel, domain, and Robolectric tests
│   │   └── androidTest/                # On-device integration and instrumentation tests
│   └── build.gradle.kts                # App module build configuration and dependencies
├── docs/                               # Engineering documentation, audits, and checklists
│   ├── adr/                            # Architecture Decision Records
│   ├── security/                       # Security audits, RLS tests, threat models
│   └── STARTUP_BENCHMARK.md            # Cold start performance and baseline profiling guide
├── scripts/                            # Automation scripts (benchmarks, lint checks, hooks)
├── gradle/libs.versions.toml           # Version catalog managing all dependencies
└── README.md                           # Project guide and overview
```

---

## 🏛️ Architecture Decision Records (ADRs)

Our core technical choices are recorded in [`docs/adr/`](docs/adr/):

- **[ADR 0001: Clean Architecture and Unidirectional Data Flow](docs/adr/0001-clean-architecture-and-udf.md)**
- **[ADR 0002: Offline-First Persistence with Room and Supabase RLS](docs/adr/0002-offline-first-room-and-supabase.md)**
- **[ADR 0003: Centralized Design Tokens and Dual Skins (Comic & Pixel)](docs/adr/0003-unified-design-tokens-and-skins.md)**
- **[ADR 0004: Process Death Survival and State Restoration via SavedStateHandle](docs/adr/0004-process-death-saved-state-handle.md)**
- **[ADR 0005: Multi-Tier Testing Pyramid: Domain Contracts, Fakes, Turbine & Robolectric](docs/adr/0005-testing-pyramid-and-fakes.md)**

---

## 🔒 Security & Privacy Posture

1. **Default-Deny Backend Policy**: All Supabase PostgreSQL tables enforce Row Level Security (RLS). Users can only access their own data.
2. **Zero Client Secrets**: No API keys or server-role secrets are shipped in the client APK. AI coach requests pass through an authenticated backend proxy.
3. **Local-First Privacy**: Sensitive prayer logs, personal habit names, and local settings never leave the device unless explicitly synced.
4. **Content Moderation**: Multiplayer chats and usernames feature client and server-side profanity filtering, rate limiting, and in-app reporting/blocking.
5. **Secure Backups**: Automatic system backups (`allowBackup`) exclude sensitive tokens, and manual JSON exports are validated against versioned schemas.

---

## 🧪 Building & Running Locally

### Prerequisites
- Android Studio Ladybug (2024.2.1+) or newer
- JDK 17 or JDK 21
- Android SDK 35 (compileSdk 35, minSdk 24)

### Commands
```bash
# Clone the repository
git clone https://github.com/RAZAAli901/PixelQuest.git
cd PixelQuest

# Verify code formatting and linting
./gradlew spotlessCheck

# Automatically format Kotlin code
./gradlew spotlessApply

# Run all unit and Robolectric tests (2,100+ tests)
./gradlew testDebugUnitTest

# Assemble Release APK
./gradlew assembleRelease
```

---

## 📄 License

PixelQuest is open-source software licensed under the [MIT License](LICENSE).
