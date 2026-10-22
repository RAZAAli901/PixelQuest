# ADR 0001: Clean Architecture and Unidirectional Data Flow (UDF)

- **Status**: Accepted
- **Date**: 2026-10-22
- **Deciders**: PixelQuest Core Engineering

## Context
PixelQuest has expanded from a simple daily habit checklist into an RPG life-gamification client featuring offline quest tracking, Namaz prayer schedules, Tanzil Quran reader, real-time Darood & study groups, and refereed chess challenges. To prevent coupling between presentation, data, and external platform SDKs, a structured architecture is required.

## Decision
Adopt a strict 5-tier Clean Architecture adhering to Unidirectional Data Flow (UDF):
1. **UI Layer (Jetpack Compose)**: Stateless Composables rendering immutable `UiState` and emitting user events via lambdas.
2. **Presentation Layer (ViewModel)**: Exposes a single `StateFlow<UiState>` per screen, handles user events, and uses `viewModelScope`.
3. **Domain Layer**: Pure Kotlin business models and Use Cases. Free from Android framework and database annotations. Defines repository interfaces.
4. **Data Layer**: Implements repository interfaces, mapping Room entities (`*Entity`) and network transfer objects (`*Dto`) into clean domain models (`*Model`).
5. **Data Sources Layer**: Encapsulates Room DAOs, DataStore preferences, Supabase Realtime, and WorkManager background tasks.

## Consequences
- Every layer has clear single responsibilities and is isolated for testability.
- Fast JVM unit testing without Android emulator dependencies for domain and data logic.
- Avoids state inconsistencies and recomposition bugs across Compose screens.
