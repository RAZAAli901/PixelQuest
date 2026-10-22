# Coding standards

PixelQuest favours small, feature-owned changes over broad rewrites. Preserve existing behaviour,
add a characterisation test before changing unclear code, and keep a task's commit focused.

## Names and packages

- Package by feature. Code shared by several features belongs in an existing shared/core package;
  do not create a generic `utils` dumping ground.
- Name classes after their role: `…Screen`, `…ViewModel`, `…Repository`, `…Dao`, `…Worker`,
  `…UseCase`, and `…Scheduler`. Interfaces describe the capability; implementations describe the
  technology (for example, `Supabase…` or `Room…`).
- Prefer explicit domain types and named constants over booleans, strings, and magic numbers.

## Layers and dependencies

The UI renders state and sends events. A ViewModel owns screen state and delegates business rules to
use cases. Use cases depend on repository interfaces; repositories coordinate local storage and
remote data sources. Android framework APIs remain at the outer edge behind injected abstractions
when they affect time, I/O, scheduling, or networking.

Dependencies point inward: UI must not call DAOs, Supabase, WorkManager, or `Dispatchers` directly.
Domain code must not import Compose or Android types. Map network DTOs and database entities at the
data boundary before exposing domain models.

## State, events, and effects

- Expose one immutable `UiState` `StateFlow` per screen. State contains everything needed to render.
- Send user intent through explicit events/functions; do not mutate state from a composable.
- Emit one-off navigation, snackbar, and share actions as effects, not persistent state.
- Use `viewModelScope`; never use `GlobalScope`. Collect flows with lifecycle-aware Compose APIs.
- Put `remember`, `derivedStateOf`, and stable list keys at the UI boundary. Keep composables small
  and side-effect free apart from `LaunchedEffect`/`DisposableEffect` with accurate keys.

## Errors, time, and concurrency

Use the project result/error type at repository boundaries. Convert low-level errors to a safe,
actionable domain error and log only non-sensitive diagnostics through `AppLog`. Never expose
tokens, message contents, or account data in an error.

Inject dispatchers and a `Clock` where work or time affect behaviour. Room and network work must be
off the main thread. Workers need a unique name, explicit constraints and backoff, and an idempotent
operation so retries are safe.

## Quality gate

Before opening a pull request, run `./gradlew :app:spotlessCheck :app:detekt :app:lintDebug
:app:testDebugUnitTest`. CI runs the same Android checks. New Kotlin warnings and new lint/detekt
findings fail the build; fix them instead of extending a baseline without a documented reason.
