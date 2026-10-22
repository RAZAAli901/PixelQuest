# Time Injection Policy

In PixelQuest, reading system time directly via unparameterized calls (e.g. `LocalDate.now()`, `System.currentTimeMillis()`) is restricted in business logic, repositories, workers, and ViewModels. Time sources must be injectable to ensure predictable, deterministic unit tests.

## Clock Abstractions

1. **`java.time.Clock`**:
   - The platform standard for instant and zone time reads.
   - Provided as a singleton by `com.pixelquest.app.di.TimeModule`:
     ```kotlin
     @Provides
     @Singleton
     fun provideClock(): Clock = Clock.systemDefaultZone()
     ```

2. **`com.pixelquest.app.util.AppClock`**:
   - Domain-level reactive clock that wraps `java.time.Clock`.
   - Exposes reactive midnight flows (`today(): Flow<LocalDate>`) and minute ticker flows (`minuteTicks(): Flow<LocalDateTime>`) so long-running foreground screens update cleanly without polling or app restarts.

## Usage in Production Code

Inject `AppClock` or `Clock` via constructor:

```kotlin
@HiltViewModel
class ExampleViewModel @Inject constructor(
    private val clock: AppClock
) : ViewModel() {
    val today = clock.today()
    val now = clock.now()
}
```

## Usage in Unit Tests

Unit tests supply `FixedClock` or `Clock.fixed(instant, zone)`:

```kotlin
val fixedClock = FixedClock(LocalDateTime.of(2026, 10, 22, 12, 0))
val viewModel = ExampleViewModel(clock = fixedClock)
```
