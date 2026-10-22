# ADR 001: Target Clean Architecture

- **Status**: Accepted
- **Date**: 2026-10-22
- **Context**: Q021 architectural standardisation for PixelQuest Android client.

## Context

PixelQuest features have grown across habits, leveling, global leaderboards, prayer tracking (Deen/Namaz), Tanzil Quran, collaborative study parties, and chess. To keep the codebase scalable, testable, and maintainable, a unified architectural blueprint is established across all feature packages.

## Decision

Adopt an inward-pointing 5-tier Clean Architecture adhering to Unidirectional Data Flow (UDF):

```
       +---------------------------------------------+
       |                  UI Layer                   |
       |  (Jetpack Compose Composables & Navigation)  |
       +---------------------------------------------+
                              |
                              v
       +---------------------------------------------+
       |             Presentation Layer              |
       |   (ViewModel, UiState, Events, Effects)     |
       +---------------------------------------------+
                              |
                              v
       +---------------------------------------------+
       |                Domain Layer                 |
       |   (Use Cases, Domain Models, Repos Interfaces|
       +---------------------------------------------+
                              ^
                              |
       +---------------------------------------------+
       |                 Data Layer                  |
       |  (Repository Impls, Model Mappers, Offline) |
       +---------------------------------------------+
                              |
                              v
       +---------------------------------------------+
       |             Data Sources Layer              |
       |  (Room DAOs, Supabase Realtime, WorkManager)|
       +---------------------------------------------+
```

### 1. UI Layer (Jetpack Compose)
- **Role**: Pure visual rendering and user event capture.
- **Rules**:
  - Composables render state from an immutable `UiState` and forward user interactions via event lambdas.
  - No direct calls to DAOs, network clients, `WorkManager`, or coroutine dispatchers.
  - Platform-specific resources and theme tokens (Comic and Pixel skins) are consumed through centralized design tokens.

### 2. Presentation Layer (ViewModel)
- **Role**: Owns screen state lifecycle and converts domain signals into UI-ready states.
- **Rules**:
  - Exposes exactly one immutable `StateFlow<UiState>` per screen.
  - One-off events (navigation, toasts, dialog triggers) are handled via effects, not persistent state flags.
  - Coroutines are strictly scoped to `viewModelScope`. No use of `GlobalScope`.
  - Injects use cases or domain repository interfaces; never concrete remote data sources or Room DAOs.

### 3. Domain Layer
- **Role**: Business logic, policies, and domain entity contracts.
- **Rules**:
  - Zero dependencies on Android framework (`android.*`), Jetpack Compose, or Room/Supabase annotations.
  - Business rules (XP computation, streak logic, difficulty calculations, moderation checks) reside in use cases.
  - Declares repository interfaces; implementations are supplied by the data layer.

### 4. Data Layer
- **Role**: Single source of truth coordinating offline-first persistence and cloud synchronization.
- **Rules**:
  - Implements domain repository interfaces.
  - Maps database entities (`*Entity`) and remote DTOs (`*Dto`) to clean domain models before emitting to callers.
  - Error handling: Low-level SQLite or HTTP exceptions are caught and mapped to domain-safe `Result` types.
  - All I/O is dispatched on injected background dispatchers (`@IoDispatcher`).

### 5. Data Sources Layer
- **Role**: Technology-specific adapters (Room SQLite, Ktor/Supabase Realtime, Android Keystore, WorkManager).
- **Rules**:
  - Room DAOs return `Flow` for reactive queries and suspend functions for writes.
  - All mutations are idempotent.
  - Supabase client interactions enforce server-authoritative Row Level Security.

## Consequences

- Clear boundary enforcement simplifies modularisation and unit testing with lightweight fakes.
- Facilitates the large-file refactoring (Q023) and repository pattern standardisation (Q029-Q033).
