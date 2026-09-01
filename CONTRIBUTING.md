# ⚔️ Contributing to PixelQuest

Thank you for your interest in contributing to **PixelQuest**!

---

## 🏗 Project Architecture & Structure

PixelQuest follows **MVVM + Clean Architecture** guidelines:

```
app/src/main/java/com/pixelquest/app/
├── audio/            # SoundPool audio engine & persisted sound manager
├── data/
│   ├── backup/       # JSON export & import payload converters
│   ├── local/        # Room Database entities, DAOs, and TypeConverters
│   └── repository/   # Repository implementations consuming DAOs
├── di/               # Dagger Hilt dependency injection modules
├── domain/           # Core domain models, calculators (Points, Streaks), repositories
├── notification/     # Android notification channels & builder helpers
├── scheduling/       # AlarmManager exact alarm scheduler & broadcast receivers
├── ui/
│   ├── components/   # Atomic retro Jetpack Compose UI component library
│   ├── navigation/   # Jetpack Navigation Compose graph & destinations
│   ├── screens/      # Feature screens (Today, Tasks, Stats, Profile, Onboarding)
│   └── theme/        # Pixel color palette, typography, shapes
├── util/             # Crash handler & safe database call wrappers
└── worker/           # WorkManager periodic background workers
```

---

## 🛠 Local Setup & Development Workflow

### Prerequisites
- JDK 17
- Android Studio Jellyfish (or higher) / Android SDK API 34

### Building & Running Unit Tests
```bash
# Run unit test suite
./gradlew test

# Run Android instrumented UI tests
./gradlew connectedCheck
```

### Building Release APK Locally
```bash
# Build signed release APK
./gradlew assembleRelease
```

---

## 📜 Commit Conventions & PR Rules
- Each pull request must be atomic, buildable, and pass `./gradlew test`.
- Respect existing Press Start 2P 8-bit visual aesthetics and design guidelines.
