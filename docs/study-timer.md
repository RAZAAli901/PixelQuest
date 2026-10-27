# Study Timer in Notification: Design Specification

This document details the architecture, state machine, notification presentation, and foreground service lifecycle of the ongoing Study Timer for PixelQuest Study Circles ([study-party.md](study-party.md)).

---

## 1. Overview and Core Experience

Studying in a Study Circle allows members to log focused study time towards their circle's weekly goals. To provide a seamless experience while studying alongside other apps or with the screen off:

- **Ongoing Notification:** When a study timer is running or paused, PixelQuest presents an ongoing notification titled `"Studying with <Party Name>"`.
- **Live Chronometer:** Uses Android's native `NotificationCompat.Builder.setUsesChronometer(true)` and `setChronometerCountDown(false)`. The system UI updates the elapsed time display locally on the status bar and shade without per-second background CPU wakeups or battery drain.
- **Direct Notification Controls:** Includes instant notification action buttons:
  - When **Running**: `Pause` and `Stop`.
  - When **Paused**: `Resume` and `Stop`.
- **Tap to Open:** Tapping the notification body launches the main app and navigates directly to the active study circle's timer tab.
- **Persistent Across Lifecycle:** Survives app backgrounding, configuration changes, system process death, and device reboot.
- **Single Timer Invariant:** Only one study timer can run or be paused at any time across all circles on the device. Starting a timer in another circle prompts the user to stop the running one first.

---

## 2. State Machine and Persistence

### 2.1 Timer States

The timer operates according to a strict three-state finite state machine (FSM):

```
       start()                  pause()
 [STOPPED] -------> [RUNNING] <=========> [PAUSED]
    ^                  |        resume()     |
    |                  |                     |
    +--- stop() -------+---------------------+
```

1. **`STOPPED`**:
   - No study timer is active.
   - Foreground service is terminated and ongoing notification is dismissed.
   - Storage state is cleared.

2. **`RUNNING`**:
   - Timer is actively accumulating study time.
   - Ongoing notification displays a running chronometer starting from the compensated base timestamp.
   - Actions shown: `Pause`, `Stop`.

3. **`PAUSED`**:
   - Timer is frozen; no study time is accumulating.
   - Ongoing notification displays static elapsed study time (chronometer stopped) and status "Paused".
   - Actions shown: `Resume`, `Stop`.

### 2.2 Data Model & Chronometer Mathematics

To avoid clock drift and handle pause/resume accurately without per-second timers:

```kotlin
data class ActiveStudyTimer(
    val partyId: String,
    val partyName: String,
    val startedAt: Long,              // Wall-clock epoch millis when session began
    val pausedAt: Long? = null,       // Wall-clock epoch millis when paused, null if running
    val accumulatedPauseMs: Long = 0L // Total millis spent paused during this session
) {
    val isPaused: Boolean get() = pausedAt != null

    /** Effective elapsed study duration in milliseconds at given moment [now]. */
    fun elapsedMillisAt(now: Long): Long {
        return if (isPaused) {
            val pauseMoment = pausedAt ?: now
            (pauseMoment - startedAt - accumulatedPauseMs).coerceAtLeast(0L)
        } else {
            (now - startedAt - accumulatedPauseMs).coerceAtLeast(0L)
        }
    }

    /**
     * Chronometer base timestamp for NotificationCompat.Builder.setWhen().
     * Elapsed realtime is used so system chronometer matches elapsed active millis.
     */
    fun chronometerBase(currentRealtime: Long, now: Long): Long {
        val elapsed = elapsedMillisAt(now)
        return currentRealtime - elapsed
    }
}
```

### 2.3 State Persistence (`StudyTimerStore`)

All state transitions (`start`, `pause`, `resume`, `stop`) are immediately committed to persistent storage (`SharedPreferences` via `PrefsStudyTimerStore`):
- `timer_party_id` (String)
- `timer_party_name` (String)
- `timer_started_at` (Long)
- `timer_paused_at` (Long)
- `timer_accumulated_pause_ms` (Long)

Because the timestamps are persisted, if the app process is killed or the phone is rebooted while the timer is `RUNNING` or `PAUSED`, the state is fully restored on resurrection.

---

## 3. Foreground Service: `StudyTimerService`

### 3.1 Role and Lifecycle

`StudyTimerService` is an Android `ForegroundService` that:
1. Owns the active session state and exposes a reactive `StateFlow<ActiveStudyTimer?>`.
2. Posts and maintains the foreground notification with `Service.startForeground()`.
3. Handles notification action intents:
   - `ACTION_START`
   - `ACTION_PAUSE`
   - `ACTION_RESUME`
   - `ACTION_STOP`
   - `ACTION_CANCEL`
4. Enforces the 4-hour automatic safety stop limit.

### 3.2 Service Declaration & Target SDK Compatibility

- **Channel:** `pq_study_timer` (`NotificationChannels.Spec.STUDY_TIMER`)
  - Importance: `NotificationManager.IMPORTANCE_LOW` (silent, ongoing, non-intrusive).
- **Service Type:** Android 14+ (API 34+) requires a specific foreground service type:
  - `android:foregroundServiceType="specialUse"` or `dataSync`.
  - Declares `<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />` and `<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />`.

---

## 4. Notification Presentation and Actions

### 4.1 Visual Layout

- **Title:** `Studying with <party name>`
- **Content:**
  - Running: Live chronometer counting up (`setUsesChronometer(true)`).
  - Paused: `"Paused · <elapsed time>"` (`setUsesChronometer(false)`).
- **Icon:** Pixel mascot in Focus mood (`PixMood.Focus`) or app notification icon.
- **Ongoing:** `setOngoing(true)`, `setOnlyAlertOnce(true)`.

### 4.2 Interactive Actions

1. **Pause Action:**
   - Intent to `StudyTimerService` with `ACTION_PAUSE`.
   - Records `pausedAt = System.currentTimeMillis()`.
   - Updates notification to static text, swaps action to `Resume`.
2. **Resume Action:**
   - Intent to `StudyTimerService` with `ACTION_RESUME`.
   - Computes `additionalPause = System.currentTimeMillis() - pausedAt`.
   - Adds to `accumulatedPauseMs`, sets `pausedAt = null`.
   - Re-arms chronometer with updated base and swaps action to `Pause`.
3. **Stop Action:**
   - Intent to `StudyTimerService` with `ACTION_STOP`.
   - Calculates total active study minutes (`elapsedMillis / 60_000`).
   - If `>= 5 minutes`: Saves study session record to Room and remote party scoreboard, caps at 240 minutes, and triggers a brief transient `"Study time saved (<N> mins)"` notification.
   - If `< 5 minutes`: Discards session with `"Not saved: under 5 minutes"` notice.
   - Terminates foreground service and clears storage.
4. **Content Intent (Tap):**
   - Launches `MainActivity` with extra `OPEN_STUDY_PARTY_ID = partyId` and `FOCUS_TAB = WEEK`, opening the timer card directly.

---

## 5. In-App Synchronization & Single Timer Invariant

1. **Synchronized State:**
   - `StudyPartyViewModel` observes `StudyTimerStore.running` (fed directly by `StudyTimerService`).
   - When the user pauses or resumes from the notification, the in-app `StudyTimerCard` reflects the state change immediately.
   - When the user pauses, resumes, or stops from inside the app, it sends the command to `StudyTimerService`, keeping the notification in lockstep.
2. **Single Active Timer Invariant:**
   - If a timer is already active for Party A, and the user visits Party B:
     - `StudyTimerCard` in Party B displays: `"Timer already running for <Party A>"`.
     - Tapping start on Party B displays a confirmation dialog: *"Stop timer for <Party A> and start here?"*.

---

## 6. Auto-Stop and Process Death Resilience

1. **4-Hour Auto-Stop:**
   - Max allowable study session is 240 minutes (4 hours).
   - If the timer runs continuously or reaches 4 hours of active time, `StudyTimerService` automatically stops the timer, logs 240 minutes, posts a `"Study timer ended (max 4 hours saved)"` notification, and terminates.
2. **Reboot and Process Resurrection:**
   - On device boot (`BootReceiver`), if `StudyTimerStore` holds an active session:
     - If the elapsed duration exceeds 4 hours, the session is capped and saved or discarded if under 5 minutes.
     - Otherwise, `StudyTimerService` is restarted in the background to restore the ongoing notification.
