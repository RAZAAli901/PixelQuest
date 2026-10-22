# ADR 0005: Multi-Tier Testing Pyramid: Domain Contracts, Fakes, Turbine & Robolectric

- **Status**: Accepted
- **Date**: 2026-10-23
- **Deciders**: PixelQuest Core Engineering

## Context
A mission-critical health, habit, and prayer application requires high test coverage across business logic, asynchronous coroutine flows, database contracts, and Compose UI layouts. Running all tests on physical Android devices or slow emulators hinders fast developer iteration.

## Decision
Establish a structured testing pyramid executed predominantly on the local JVM:
1. **Tier 1: Domain Rules & Use Case Tests**:
   - 100% pure Kotlin unit tests verifying XP calculation, leveling thresholds, streak policies, and moderation limits.
2. **Tier 2: Repository Contract Tests with In-Memory Fakes**:
   - Implement thread-safe in-memory fakes implementing domain repository interfaces.
   - Test data caching, offline persistence, and failure recovery without SQLite or network overhead.
3. **Tier 3: ViewModel State Transition Tests with Turbine**:
   - Test `StateFlow<UiState>` emissions and asynchronous coroutine pipelines using CashApp Turbine and `UnconfinedTestDispatcher`.
4. **Tier 4: UI Smoke & Screenshot Tests via Robolectric**:
   - Compose UI tests running with `@RunWith(RobolectricTestRunner::class)` and `createComposeRule()` verifying layout rendering, semantics, and accessibility across all main screens.

## Consequences
- Fast CI test runs (< 60s for thousands of tests) with zero flaky emulator issues.
- Comprehensive regression barrier guarding against regressions across all application layers.
