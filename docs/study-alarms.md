# Study Due Alarms: 24-Hour Due Alert Design

This document details the architecture, scheduling, delivery, and lifecycle of alarm-style alerts for Study Circle assignments and quizzes, scheduled exactly 24 hours before their due time.

---

## 1. Overview and Core Principle

Inside a Study Circle ([study-party.md](study-party.md)), assignments and quizzes have an optional due moment (`dueAt: Instant`). To ensure active members have ample time to prepare and finish coursework:

- **Target Audience:** Each active member of the study party on their own device.
- **Trigger Time:** Exactly 24 hours before the item's due moment: `alertTime = dueAt - 24 hours`.
- **Items Eligible:** Study items of kind `ASSIGNMENT` or `QUIZ` that have a non-null `dueAt` timestamp and have not yet been marked completed by the active user.
- **One Alert Per User Per Item:** Each item triggers at most one 24-hour reminder per user (plus optional snoozes).

---

## 2. Timing Rules and Edge Cases

### 2.1 The 24-Hour Rule
- When `dueAt` is set, the alarm is calculated as `dueAt.minus(Duration.ofHours(24))`.
- Example: If an assignment is due on Friday at 15:00 UTC, the alarm is scheduled for Thursday at 15:00 UTC.

### 2.2 Retroactive Alert Prevention (The 24-Hour Window Guard)
- If an item is created or first synced when less than 24 hours remain before its due date (i.e. `dueAt - 24h <= now`), **no retroactive alert is scheduled or fired**.
- Past alarms are never delivered late: alerting the user about an alert moment that passed hours ago is disruptive and unhelpful.
- If `dueAt <= now`, the item is already past due and no alarm is scheduled.

### 2.3 Daylight Saving Time (DST) and Time Zone Shifts
- `dueAt` is stored as a UTC `Instant`.
- The alarm is armed in UTC epoch milliseconds via `AlarmManager.RTC_WAKEUP`.
- When users cross time zones or clock shifts occur (DST), the absolute instant `dueAt - 24 hours` remains invariant. Alarms are re-evaluated on system time zone and clock changes (`ACTION_TIMEZONE_CHANGED`, `ACTION_TIME_CHANGED`) to ensure accurate local wall-clock presentation.

---

## 3. Architecture and Scheduling Component

### 3.1 `StudyDueAlarmScheduler`
The scheduler interfaces between Room database changes, sync events, and the system `AlarmManager` (via the unified `AlarmScheduler` interface):
- **Request Code Generation:** Computes a unique, deterministic 32-bit request code per study item ID (e.g. `(itemId.hashCode() and 0x3FFFFFFF)` with namespace masks for alarm vs snooze).
- **Batch Scheduling:** Scans unfinished items with `dueAt` across all active study parties and arms alarms for eligible future items.
- **Single Item Operations:** Provides `scheduleForItem(item)`, `cancelForItem(itemId)`, `rescheduleForItem(item)`.

### 3.2 Exact Alarm Permissions & Fallbacks
- On Android 12+ (API 31+), exact alarms require `SCHEDULE_EXACT_ALARM` or `USE_EXACT_ALARM`.
- `AlarmScheduler.canScheduleExactAlarms()` checks permission.
- **Fallback:** If exact alarm permission is revoked or denied, the scheduler falls back to `AlarmArming.arm` using `AlarmManager.setWindow(RTC_WAKEUP, atMillis, 10 minutes, pendingIntent)` and sets a visible warning notice in Settings/Party screen directing the user to grant exact alarm privileges.

---

## 4. Delivery & Alarm-Style Experience

### 4.1 Broadcast Receiver: `StudyDueAlarmReceiver`
When `AlarmManager` fires:
1. Validates that the user is signed in and notifications are globally enabled.
2. Checks user settings: verifies alarm-style alerts are enabled, and the specific study party is not muted.
3. Checks local Room cache: verifies that the item has not been marked completed by the user and has not been deleted.
4. Posts the high-importance notification.

### 4.2 Notification Channel: High Importance & Alarm Category
- **Channel:** `pq_study_due_alarms` (or `NotificationChannels.Spec.STUDY_DUE_ALARMS`).
- **Importance:** `NotificationManager.IMPORTANCE_HIGH`.
- **Sound & Vibration:** Plays chosen alarm sound on the `AudioAttributes.USAGE_ALARM` stream with a persistent vibration pattern.
- **Category:** `NotificationCompat.CATEGORY_ALARM`.

### 4.3 Full-Screen Intent (When Screen is Off)
- Following the `TaskPromptActivity` pattern, when the screen is locked or off, a full-screen intent is launched (`TaskPromptActivity` / `StudyPromptActivity`) to wake the display and show the prompt immediately, allowing one-touch response.
- Governed by `NotificationHelper.canUseFullScreenIntent(context)`.

---

## 5. Notification Copy and Interactive Actions

### 5.1 Presentation
- **Title:** Formatted with item kind and due time:
  - Quiz: `Quiz due tomorrow at 10:00: <title>`
  - Assignment: `Assignment due tomorrow at 17:30: <title>`
- **Body:** Name of the study circle / party and brief prompt to complete before the deadline.

### 5.2 Actions
1. **Done:**
   - Broadcast intent to mark the item completed immediately.
   - Triggers optimistic local completion via `StudyRepository.complete(item)`, dismisses notification, and cancels any pending snooze.
2. **Snooze 1 hour:**
   - Arms a one-shot alarm for `now + 1 hour`.
   - Dismisses current notification; re-alerts in 60 minutes.
3. **Open:**
   - Launches main app navigation to the specific study party's "This week" tab focusing on the item.

---

## 6. Lifecycle Management and Alarms Maintenance

Alarms must never be orphaned or lost:

| Trigger Event | Action Taken |
|---|---|
| **Item Marked Done** | Alarm for `itemId` is cancelled immediately. |
| **Item Due Date Edited** | Previous alarm cancelled; new alarm calculated for `updatedDueAt - 24 hours` (if `> now`). |
| **Item Deleted** | Alarm for `itemId` is cancelled immediately. |
| **User Leaves Party** | All alarms for that `partyId` are cancelled immediately. |
| **Device Reboot (`BOOT_COMPLETED`)** | `BootReceiver` invokes `StudyDueAlarmScheduler.rescheduleAll()`. |
| **Timezone / Clock Changed** | Receiver triggers `rescheduleAll()`. |
| **Background Sync** | Every sync cycle re-verifies and arms missing alarms. |

---

## 7. User Settings and Muting

1. **Global Toggle:** `study_alarm_style_enabled` (Boolean, default `true`). When turned off, due alerts post as standard non-alarm notifications.
2. **Alarm Sound Choice:** Allows user to select between default alarm tone, gentle chime, or system default.
3. **Party Mute:** Each study party provides a mute toggle `isMuted` in its settings sheet. Muting a party keeps its chat and items in Room but suppresses all alarm and reminder broadcasts for that party.
