# PixelQuest Testing Architecture & Guide

This document defines the testing strategy, architecture, and standards for PixelQuest (Kotlin, Jetpack Compose, Room, Hilt, WorkManager, AlarmManager, Supabase, and Gemini proxy).

---

## 1. Test Pyramid

PixelQuest follows a disciplined test pyramid to ensure rapid developer feedback, comprehensive regression defense, and deterministic CI execution.

```
            /  \
           / E2E \          Few End-to-End Tests (<5%)
          / Smoke \         Critical user journeys & smoke runs on GMD
         /----------\
        / Integration \      Fewer Integration Tests (~15-20%)
       /  Components   \     Robolectric, in-memory Room, fake services
      /------------------\
     /    Unit Tests      \   Many Unit Tests (~75-80%)
    / Fast, Deterministic  \  Use cases, ViewModels, pure domain, state machines
   /------------------------\
```

### Layer Breakdown

1. **Unit Tests (Fast, Local JVM, High Volume)**
   - Target: Use Cases, ViewModels, State Machines, Parsers, Room Converters, Domain Algorithms (Chess engine, Prayer calculation, Level calculation, XP distribution).
   - Environment: Local JVM with `kotlinx-coroutines-test` (`runTest`, `StandardTestDispatcher`, `UnconfinedTestDispatcher`).
   - Dependencies: Injected fakes (`FakeTaskRepository`, `FakeClock`, `FakeNotificationManager`) and test doubles.
   - Execution time: Milliseconds per test.

2. **Integration Tests (Component & Subsystem Level)**
   - Target: Repositories with in-memory Room databases, foreground services (`StudyTimerService`), Alarm Receivers, WorkManager Workers (`DailyReminderWorker`, `BackupWorker`), and local Supabase contract verification (`FakeSupabaseBackend`).
   - Environment: Local JVM with Robolectric 4.12+ and `androidx.test.core`.
   - Execution time: Seconds per suite.

3. **End-to-End & UI Tests (GMD & Smoke)**
   - Target: Full screen Compose navigation flows, app startup smoke test (`AppSmokeTest`), theme toggling (Pixel vs Comic vs Simple Mode), and critical user journeys.
   - Environment: Gradle Managed Devices (GMD) phone/tablet profiles and Robolectric Compose test runner (`createComposeRule()`).

---

## 2. Directory & Folder Layout

PixelQuest separates testing assets into standard Android source sets with a dedicated `sharedTest` directory configured in `app/build.gradle.kts`:

```
app/
├── src/
│   ├── main/java/com/pixelquest/app/          # Production code
│   │   ├── data/                              # Room DAOs, entities, repositories
│   │   ├── domain/                            # Use cases, models, business rules
│   │   ├── ui/                                # Compose screens, components, theme
│   │   └── util/                              # AppClock, loggers, helpers
│   ├── test/java/com/pixelquest/app/          # JVM Unit & Robolectric tests
│   │   ├── data/                              # Repository & DAO integration tests
│   │   ├── domain/                            # Use case & state machine tests
│   │   ├── ui/                                # ViewModel & Compose screenshot/logic tests
│   │   └── workers/                           # WorkManager test workers
│   ├── sharedTest/java/com/pixelquest/app/    # Shared test infrastructure
│   │   └── testing/                           # Fakes, builders, rules, dispatchers
│   └── androidTest/java/com/pixelquest/app/   # On-device & GMD instrumented tests
```

---

## 3. Naming Conventions

Consistency in naming allows instant navigation and clear failure reports in CI:

- **Test Classes**: Must end with `Test` matching the system-under-test.
  - Examples: `TaskRepositoryTest`, `HomeViewModelTest`, `StudyTimerStateMachineTest`, `LevelCalculatorTest`.
- **Test Doubles / Fakes**:
  - `Fake<Interface>`: In-memory, realistic stateful fake implementing the production interface (e.g., `FakeTaskRepository`, `FakeClock`, `FakeSupabaseBackend`).
  - `Test<Component>`: Test specific rules or providers (e.g., `TestDispatcherProvider`, `MainDispatcherRule`).
- **Test Methods**:
  - Follow the format: `<methodOrAction>_<conditionOrState>_<expectedOutcome>` or descriptive snake_case/camelCase sentences.
  - Examples:
    - `insertTask_withDuplicateId_replacesExisting()`
    - `onPause_whenTimerRunning_freezesElapsedAndSetsPausedState()`
    - `calculateXp_withPerfectDayStreak_awardsDoubleBonus()`

---

## 4. Layer Coverage Targets

Minimum line coverage thresholds are codified in the testing configuration (`config/coverage-rules.json`):

| Layer | Target Coverage | Scope & Inclusions |
|---|---|---|
| **Domain Layer** | **80%** | Use cases, calculators, state machines, business policies |
| **Data Layer** | **70%** | Repositories, DAOs, entities, serializations, Room mappers |
| **ViewModels** | **80%** | ViewModels, UI state reducers, intents, presentation flows |
| **UI & Screens** | **50%** | Compose screens, custom graphics, theme rendering, tap targets |

---

## 5. Determinism Rules (Zero Flakiness Policy)

A flaky test is a critical bug. Every test in PixelQuest must adhere to the following zero-flakiness rules:

1. **Deterministic Time**:
   - Never call `System.currentTimeMillis()`, `LocalDate.now()`, or `Instant.now()`.
   - Always inject `java.time.Clock` or `AppClock` and provide `FakeClock` or `Clock.fixed()`.
2. **Controlled Coroutines**:
   - Never use `Dispatchers.IO` or `Dispatchers.Default` directly in testable code.
   - Use `MainDispatcherRule` and `TestDispatcherProvider` (`StandardTestDispatcher` / `TestScope`).
   - Use `advanceUntilIdle()` or `runCurrent()` to advance virtual time deterministically.
3. **No Real Delays or Sleeping**:
   - `Thread.sleep` and real delay loops are strictly prohibited in tests.
   - An automated build linter enforces this requirement.
4. **Isolated & In-Memory State**:
   - Use Room `inMemoryDatabaseBuilder` with `.allowMainThreadQueries()`.
   - Re-instantiate fakes before each test run; never share mutable state across test boundaries.
5. **Deterministic Test Data**:
   - Use seeded random generators for mock builders to ensure reproducible test fixtures.

---

## 6. Test Suite Baseline

*Updated automatically during Phase A (V020) and subsequent CI milestones.*

- **Initial Unit Tests Count**: 2,349 tests
- **Initial Failures**: 0
- **Initial Ignored**: 20
- **Initial Run Duration**: ~7m 27s


---

## 7. Test Writing Guide

For full worked examples covering each architectural layer (Use Case, ViewModel, Repository, Screen, and WorkManager Worker), see [docs/TEST_WRITING_GUIDE.md](TEST_WRITING_GUIDE.md).
