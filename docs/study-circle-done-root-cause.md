# Root Cause: Study Circle Item Not Disappearing After "Done"

## 1. Problem Statement
In Study Circles (Study Parties), when a user completes an assignment or quiz by tapping the Done checkmark, the item continues to remain visible in the active items list ("This week" and "All items") rather than disappearing or moving to a dedicated completed section.

---

## 2. Root Cause Analysis

### A. List Queries and ViewModel Mappings Do Not Filter by Current User's Completion
1. **DAO Query Unfiltered**:
   In `com.pixelquest.app.study.data.local.StudyDao`:
   ```kotlin
   @Query("SELECT * FROM study_item WHERE partyId = :partyId ORDER BY pinned DESC, dueAt IS NULL, dueAt, createdAt")
   fun observeItems(partyId: String): Flow<List<StudyItemEntity>>
   ```
   `observeItems(partyId)` returns every item in the party unconditionally. It does not join with or filter against `study_completion` for the current user's `userId` or `study_pending` for pending client completions. (In contrast, `observeUnfinishedDueBefore` specifically checks `NOT EXISTS (SELECT 1 FROM study_completion c WHERE c.itemId = i.id AND c.userId = :userId)`).
2. **`thisWeekOf` in `StudyPartyState.kt`**:
   ```kotlin
   fun thisWeekOf(rows: List<ItemRow>, weekStart: LocalDate, zone: ZoneId): List<ItemRow> = rows.filter { row ->
       val day = row.item.dueAt?.atZone(zone)?.toLocalDate()
       when {
           day == null -> !row.done
           !day.isBefore(weekStart) && day.isBefore(weekStart.plusDays(7)) -> true
           day.isBefore(weekStart) -> !row.done
           else -> false
       }
   }
   ```
   For any item with a due date falling within the current week (`!day.isBefore(weekStart) && day.isBefore(weekStart.plusDays(7))`), the predicate unconditionally returned `true`. As a result, even after a user marked an item done (`row.done == true`), the item remained directly in `thisWeek`.
3. **All Items Screen Tab**:
   In `StudyPartyScreen.kt` for `PartyTab.ALL_ITEMS`:
   `itemCards(state, state.items, R.string.study_items_empty, onOpenItem, actions)`
   `state.items` was passed directly without filtering out completed items, and no collapsed "Done" section existed at the bottom.

### B. Shared Party Items vs. Per-User Completion State
Unlike individual quests which have a single global `done` state per row, study circle items are multi-user shared resources:
- Each item is created once for the party (`study_item`).
- Each member has their own completion records in `study_completion` (with foreign key to `study_item` and `userId`), or locally in `study_pending` with `kind = PendingStudyKind.COMPLETION`.
- If an item list does not partition rows based on `row.done` (which evaluates `mine != null && mine.state != SyncState.FAILED` where `mine` is `myId`'s completion), user A completing an item either leaves it visible for user A, or if filtered via a global flag would incorrectly remove it for user B who has not yet completed it.

### C. Stale Local Cache and Missing Optimistic Partitioning
When tapping Done:
- `StudyRepository.complete(item)` inserts a `PendingStudyEntity` into `study_pending`.
- `StudyDao.observeCompletions` combines `study_pending` and `study_completion`.
- However, because `thisWeekOf` and `state.items` did not separate incomplete vs. completed items, the UI re-emitted the exact same item card in the active list (merely changing the checkmark to checked and sinking the card background), rather than removing it from the active list and placing it in a collapsed "Done" section.

### D. Server Refusal and Rollback Handling
If a completion is rejected by the server (e.g. permission error, already closed week, or network error), the pending record transitions to `SyncState.FAILED`. The UI must roll back the optimistic completion, display an informative notice to the user, and allow retry or discard without leaving the item in a corrupted state.

### E. Double-Tap Race Conditions on Scoreboard
If a user rapidly taps the completion circle multiple times, concurrent `complete()` invocations could attempt to insert duplicate completions or race to increment score points. While Room and repository check `pendingCompletionOf` and `completionOf`, rapid UI taps need debouncing or an immediate in-memory check to prevent duplicated score triggers.

---

## 3. Required Implementation Plan
1. **F02 Test**: Create a failing test simulating two users (User A and User B). When User A completes an item, it must disappear from User A's active list while remaining visible in User B's active list.
2. **F03 & F04 Fix**: Ensure study item queries and state pipelines filter by the current user's own `study_completions` / `study_pending` row. On Done tap, apply optimistic local cache update immediately, with graceful rollback and notice on server refusal.
3. **F05 Feat**: Update "This week" and "All items" to show only items the current user has not completed. Move completed items to a collapsed "Done" section at the bottom, supporting Undo within the permitted server window (24 hours).
4. **F06 Feat**: Display avatars and a clear count of members who finished each item (e.g., "3 of 5 done").
5. **F07 Feat**: When all active members have completed an item, display the "Everyone done" label and keep the item strictly in the Done section.
6. **F08 Test**: Comprehensive test suite covering multi-user completions, undo, offline sync, and real-time updates while the screen is open.
7. **F09 Fix**: Guard against rapid double taps on Done to ensure scores and completions cannot be double-counted.
