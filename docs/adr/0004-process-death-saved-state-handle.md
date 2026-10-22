# ADR 0004: Process Death Survival and State Restoration via SavedStateHandle

- **Status**: Accepted
- **Date**: 2026-10-23
- **Deciders**: PixelQuest Core Engineering

## Context
When Android terminates the app in the background due to low memory or configuration changes (screen rotation, foldables, language changes), transient user inputs like quest creation drafts, search queries, or selected filters could be lost.

## Decision
1. **SavedStateHandle Integration**: ViewModels with user-entered transient state (`TaskFormViewModel`, `QuranSearchViewModel`, `LeaderboardViewModel`) inject `SavedStateHandle`.
2. **State Backing**: User inputs (title drafts, descriptions, query strings, tab selections) are saved to `SavedStateHandle` on mutation and restored during ViewModel instantiation.
3. **Automated Verification**: State survival is verified with unit tests simulating cold restarts and process death using `SavedStateHandle`.

## Consequences
- Zero data loss when users switch apps or rotate their screen while composing quests or searching scriptures.
- Consistent user experience adhering to Android Modern Architecture guidelines.
