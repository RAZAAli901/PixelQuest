# Foreground Work & Service Audit

**Application:** PixelQuest (`com.pixelquest.app`)  
**Target SDK:** Android 16 (API 36)  
**Audit Date:** November 2026  

---

## 1. Executive Summary

Android 14 (API 34) through Android 16 (API 36) mandate that every foreground service declare at least one specific `foregroundServiceType` in the manifest and runtime invocation, provide appropriate permission declarations, and undergo review by Google Play.

This audit catalogues every service, background worker, alarm, and scheduled task across the PixelQuest codebase to guarantee strict compliance and eliminate any risk of policy rejection.

---

## 2. Inventory of Services & Background Workers

### A. Foreground Services (1 Total)

| Component | Class | FGS Type | Manifest Permission | Trigger / Lifecycle |
|---|---|---|---|---|
| **Study Timer Service** | `com.pixelquest.app.study.timer.StudyTimerService` | `specialUse` (`FOREGROUND_SERVICE_TYPE_SPECIAL_USE`) | `FOREGROUND_SERVICE`<br>`FOREGROUND_SERVICE_SPECIAL_USE` | **User-initiated:** Player taps "Start Study Session" (Solo or Party). Posts an ongoing notification displaying elapsed/remaining time, pause/resume, and stop actions. Stops immediately when timer completes or user taps Stop. |

#### Special Use Subtype Property
Declared in `AndroidManifest.xml` under `<service android:name=".study.timer.StudyTimerService">`:
```xml
<property
    android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
    android:value="Study timer recording active focused study session duration" />
```

#### Runtime Invocation
`StudyTimerService.kt`:
```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
    startForeground(
        NOTIFICATION_ID,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
    )
} else {
    startForeground(NOTIFICATION_ID, notification)
}
```

---

### B. Standard Services (Non-Foreground) (1 Total)

| Component | Class | Exported | Purpose |
|---|---|---|---|
| **Push Message Service** | `com.pixelquest.app.push.PushService` | `false` | Receives incoming push payloads (study invites, reminders) and dispatches them to local notification manager. Does not invoke `startForeground()`. |

---

### C. WorkManager Workers (Periodic & One-Time Background)

All workers in PixelQuest inherit from `CoroutineWorker` / `Worker` and run as **standard deferred background jobs**. None call `setForeground()` or `setForegroundAsync()`:

| Worker | Trigger | Purpose | Network Constraint |
|---|---|---|---|
| `SyncWorker` | Periodic / Network change | Offline quest & habit sync to Supabase | `Connected` |
| `DailyStreakWorker` | Midnight periodic | Streak check & streak freeze evaluation | None |
| `AiCoachWorker` | Scheduled / On-demand | Daily insight generation via Gemini proxy | `Connected` |
| `ReminderWorker` | Periodic backup | Fallback reminder checking if alarms disabled | None |

---

### D. Alarms & Receivers (`AlarmManager`)

| Receiver | Intent / Action | Purpose |
|---|---|---|
| `NamazReminderReceiver` | Exact / Inexact Alarm | Delivers prayer notifications at calculated daily times |
| `StudyReminderReceiver` | Exact / Inexact Alarm | Fires pre-session study reminders |
| `StudyReminderActionReceiver` | Notification Action | Handles snooze / dismiss / start actions from reminder alerts |
| `BootReceiver` | `BOOT_COMPLETED` | Reschedules active prayer & study reminders on device reboot |

---

## 3. Compliance Verification Checklist

- [x] Every foreground service declares `android:foregroundServiceType` in `AndroidManifest.xml`.
- [x] Target SDK 34+ permission `FOREGROUND_SERVICE_SPECIAL_USE` declared in manifest.
- [x] Subtype property `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` declared with user-facing description.
- [x] Runtime `startForeground()` passes explicit type `ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE` on API 34+.
- [x] WorkManager does not leak undeclared foreground service types.
- [x] No hidden or third-party SDK foreground services.
