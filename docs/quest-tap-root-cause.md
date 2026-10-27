# Root Cause Analysis: Newly Created Quest Cannot Be Tapped (B01)

## Overview
When a quest is created in PixelQuest, players expect to immediately interact with it—tapping the card body or its controls to complete, edit, or view details. However, several interrelated issues across the touch handling architecture, list keying, state caching, and scheduling flows prevented immediate taps from firing reliably until app restart or screen refresh.

---

## Detailed Investigation of Hypotheses

### 1. List Key or ID Used Before Database Row ID Exists
- **Investigation**: In `TaskFormViewModel.saveTask`, a new `TaskEntity` was initially created with `id = state.taskId ?: 0`. Room's `@Insert` asynchronously inserts the row and generates the auto-incremented primary key (`insertedId`).
- **Impact**: In UI lists (`LazyColumn`), items are keyed by `key = { "${tab}_${g.key}_${it.id}" }` in `QuestsScreen` or `key = { it.id }` in `HomeScreen`. If an intermediate state emission occurs where `id == 0`, Compose retains the node associated with key `0`. When the database emits the updated item with its generated ID, Compose treats it as a structural key substitution. Lookups in `todayById[quest.id]` looking for `0` vs the new ID resulted in `null` references, causing `onToggleQuest` to fail or route incorrectly to edit mode instead of completing.

### 2. Click Handler Only Attached After a Refresh / Lifecycle Resume
- **Investigation**: In `HomeScreen.kt`, an observer on `ON_RESUME` explicitly called `todayViewModel.refresh()`. However, `QuestsScreen.kt` had **no lifecycle observer** calling `refresh()`.
- **Impact**: `todayById` in `QuestsScreen` was derived via `remember(todayItems) { todayItems.associateBy { it.task.id } }`. If `todayViewModel`'s Flow had not re-evaluated or was suspended, `todayById` retained its pre-creation snapshot. In `QuestsScreen`:
  ```kotlin
  onToggleQuest = { quest ->
      val item = todayById[quest.id]
      when {
          item == null -> onEditQuest(quest.id) // Falls back or no-ops
          item.status != TaskItemStatus.DONE -> todayViewModel.completeTask(item.task)
      }
  }
  ```
  Consequently, tapping the complete circle on a newly created quest fell through to `onEditQuest` or did nothing because `item` was missing from the unrefreshed local map.

### 3. Enabled Flag or Loading State That Stays True Until Restart
- **Investigation**: In `TaskFormViewModel`, `isSubmitting` was toggled during save. In the UI layer, if an action was disabled during transition or if a pointer input filter remained active, touch events were blocked.
- **Impact**: Furthermore, `rememberSwipeToDismissBoxState` in `SkippableQuestRow` initializes positional anchor state on first composition. When a new row entered the composition without an explicit key tied to the persistent item ID, the swipe state remained tied to the default offset and consumed touch pointers.

### 4. Flow That Has Not Emitted the New Row / Delayed Scheduling Day
- **Investigation**: In `TaskFormViewModel.saveTask`, newly created quests previously computed `scheduledDay` using `QuestStart.firstDay(now, time)`.
- **Impact**: When a quest was created after its scheduled time of day (e.g. at 2:00 PM for a 9:00 AM daily task), `QuestStart` pushed `scheduledDay` to **tomorrow**. `getTasksForDay(today)` queries Room for `scheduledDay <= day AND isActive = 1`. Because `scheduledDay` was in the future, Room's query did not emit the newly created quest for today at all, rendering it completely absent or un-interactive until the next day.

### 5. Overlay or Animation Layer Swallowing Touches & Gesture Conflicts
- **Investigation**: In `HomeCards.kt` (`SkippableQuestRow`), the composable hierarchy placed `modifier = Modifier.testTag(tag)` on `SwipeToDismissBox`:
  ```kotlin
  SwipeToDismissBox(
      state = swipe,
      modifier = Modifier.testTag(tag)...
  ) {
      QuestRow(quest, ...)
  }
  ```
  `SwipeToDismissBox` contains horizontal drag gesture detectors across its entire bounds. Inside it, `QuestRow` attached `comicPressable(onClick)` across the entire `Row`, while also embedding `CheckCircle` with `.toggleable()` and `FloatingReward`.
- **Impact**:
  1. **UI Tests**: Automated tests finding nodes by `onNodeWithTag(QuestsTags.quest(id)).performClick()` targeted `SwipeToDismissBox`, which lacks an `OnClick` semantics action, causing immediate test failures.
  2. **Touch Swallowing**: The swipe gesture detector in `SwipeToDismissBox` contested down/up touch events with `comicPressable`'s clickable modifier, swallowing slight dragging or micro-movement taps.
  3. **Lack of Distinct Zones**: Because `comicPressable` wrapped the entire `Row`, tapping the category icon or anywhere on the card collided with the complete circle, leading to ambiguous gesture outcomes.

### 6. State Held in a Cached Copy
- **Investigation**: `QuestsScreen` subscribed to three independent ViewModels (`todayViewModel`, `allViewModel`, and `historyViewModel`).
- **Impact**: When a new quest was inserted, `TaskViewModel` (which listens to `getAllTasks()`) and `TodayViewModel` (which combines `getTasksForDay()`, `getLogsForDate()`, and `streak`) updated asynchronously. When displaying `QuestTab.All`, the quest list came from `allGroups` while tap actions (`onToggleQuest`) still looked up `todayById` from `todayItems`. If `todayById` lagged behind `allGroups`, tapping the newly created quest failed to find the task entity in the cache.

---

## Summary of Fix Strategy (B02 to B15)
1. **B02 / B04**: Add tests ensuring that creating a quest produces an immediately interactive, tappable row before and after rotation/restart.
2. **B03**: Ensure `insertTask` row ID is immediately bound, ensure `HomeScreen` and `QuestsScreen` bind test tags and click semantics directly to the interactive card elements, and eliminate stale map lookup fallbacks.
3. **B09 - B14**: Refactor the quest card into three distinct, non-conflicting touch zones (Category Icon for Edit, Card Body with long-press for Open, and Check Circle for Complete) with minimum 48dp touch targets and TalkBack accessibility actions:
   - **Zone 1: QuestIconZone**: Minimum 48dp touch target (`sizeIn(minWidth = 48.dp, minHeight = 48.dp)`), `Role.Button`, label `R.string.pq_action_edit`, executing `onIconClick`.
   - **Zone 2: QuestCardBody**: Minimum 48dp height target (`defaultMinSize(minHeight = 48.dp)`), `Role.Button`, label `R.string.pq_action_open`, executing `onClick` on tap and `onLongClick` on long press.
   - **Zone 3: QuestCompleteZone**: Minimum 48dp touch target (`sizeIn(minWidth = 48.dp, minHeight = 48.dp)`), `Role.Checkbox`, label `R.string.pq_action_complete`, executing `onToggle` across the full zone.
   - **TalkBack Custom Actions**: `QuestRow` exposes `pq_action_complete`, `pq_action_edit`, and optional `pq_action_open` custom accessibility actions.
   - **Verification**: `QuestTouchZonesTest` verifies all 3 touch zones, their >=48dp bounds, non-conflicting isolated touch handling, long-press gestures, and TalkBack custom accessibility actions.
