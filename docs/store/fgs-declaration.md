# Google Play Foreground Service Declaration

**App Name:** PixelQuest  
**Package:** `com.pixelquest.app`  
**Target SDK:** Android 16 (API 36)  
**Declared Service Type:** `specialUse` (`FOREGROUND_SERVICE_TYPE_SPECIAL_USE`)  
**Declared Permissions:** `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`  
**Manifest Property Subtype:**  
```xml
<property
    android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
    android:value="Study timer recording active focused study session duration" />
```

---

## 1. Description of Foreground Service Purpose

PixelQuest provides gamified study sessions for individuals and collaborative "Study Parties". When a player starts a study session, the app activates the **Study Timer Service** (`com.pixelquest.app.study.timer.StudyTimerService`).

The service runs a real-time chronometer that tracks continuous study time, enforces daily duration caps (up to 4 hours), logs XP and study minutes to the database, and exposes an ongoing notification that allows the user to:
- Monitor elapsed / remaining study time at a glance without unlocking or reopening the app.
- Pause and resume the study session directly from the notification shade or lock screen.
- Stop and commit the study session to their personal stats and study party leaderboard.

---

## 2. Why Background Work (WorkManager) or Push Is Inadequate

1. **Immediate Real-Time User Feedback:** WorkManager is designed for deferred or opportunistic background execution that the OS can batch or delay by minutes to hours under battery saver, Doze mode, or low power conditions. A study timer requires exact second-level continuity and immediate user controls (pause/resume/stop).
2. **Ongoing System Notification:** Android only permits persistent, interactive ongoing notifications (with live chronometer updates and instant notification actions) while an active Foreground Service is running.
3. **Loss of Session State on Process Termination:** Without a foreground service, the Android OS kills background processes within seconds of the user switching apps (e.g., to reference books, notes, or browser). Terminating the timer disrupts active study tracking and causes loss of uncommitted study data.

---

## 3. User Impact if Denied

- **Data Loss:** Unsaved study minutes would be lost whenever the operating system reclaims memory or the user switches apps during their study session.
- **Degraded Experience:** Users would be unable to monitor their timer or pause/stop it from the lock screen, forcing them to repeatedly launch the full app and breaking their concentration/focus state.
- **Broken Party Sync:** In collaborative study parties, real-time presence indicators ("Studying now") would fail to reflect active study state accurately.

---

## 4. Reviewer Reproduction Instructions

Google Play review team can verify this functionality using the following steps:

1. Launch PixelQuest and complete initial setup or log in with guest/test credentials.
2. From the bottom navigation bar, tap **Parties** (or open the Navigation Drawer and select **Study Parties**).
3. Select any party or tap **Start Solo Session**.
4. Tap the prominent **Start Session** button.
5. Observe the ongoing notification appearing in the notification shade displaying:
   - PixelQuest focus mascot icon.
   - Live counting chronometer showing elapsed study time.
   - Action buttons: **Pause** and **Stop**.
6. Switch to the home screen or lock the device: observe that the notification remains visible and actively counting.
7. Tap **Pause** in the notification: the chronometer pauses and shows "Paused (Xm Ys)"; the button changes to **Resume**.
8. Tap **Resume**: timer resumes counting.
9. Tap **Stop**: session finishes, study time is logged, a summary notification appears, and the ongoing foreground service terminates cleanly.

---

## 5. Demo Video Specifications for Play Console

- **Link / Host:** Uploaded to YouTube (Unlisted) or Google Drive.
- **Duration:** ~45 seconds.
- **Coverage:**
  - 0:00 - 0:10: Tapping "Start Session" in Study Party.
  - 0:10 - 0:25: Pulling down notification shade showing ongoing notification with live counter.
  - 0:25 - 0:35: Tapping Pause then Resume directly from notification.
  - 0:35 - 0:45: Tapping Stop; verifying notification dismissal and study XP award.
