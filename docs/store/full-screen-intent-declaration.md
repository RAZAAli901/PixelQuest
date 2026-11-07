# Google Play Full-Screen Intent Declaration

**App Name:** PixelQuest  
**Package:** `com.pixelquest.app`  
**Target SDK:** Android 16 (API 36)  
**Permission:** `android.permission.USE_FULL_SCREEN_INTENT`  
**Policy Category:** Special App Access / Alarm & Reminder Functionality  

---

## 1. Feature Purpose & Core Use Cases

PixelQuest is a gamified productivity, habit-tracking, and study companion app. The `USE_FULL_SCREEN_INTENT` permission is requested for two specific user-facing alarm features:

1. **Study Session Due Alarms ("Alarm Style"):** When a player enables "Alarm-style study reminders" in Settings, study prompts behave like alarm clocks. When the reminder fires while the device is locked, the full-screen prompt allows the user to immediately start studying, snooze for 10 minutes, or mark the session completed without manually unlocking the phone.
2. **"Did You Do It?" Real-Time Habit Prompts:** When scheduled for critical daily habits (such as morning medicine, prayer, or urgent tasks), the prompt displays a full-screen completion screen over the lock screen for immediate acknowledgment.

---

## 2. Why High-Priority Notifications Alone Are Inadequate for the Primary Feature

- When a device is locked and in Doze mode or deep sleep, standard notifications often fail to wake the display or command immediate attention for time-sensitive commitments (e.g. morning study sessions or timely prayers).
- The full-screen prompt gives the user instant large touch targets ("Yes, I did it", "Snooze", "Not yet") on the lock screen, mirroring alarm clock interactions.

---

## 3. Graceful Fallback When Denied (Android 14+ / API 34+)

PixelQuest strictly adheres to Android 14+ behavior and Google Play policy:
- **Runtime Permission Check:** The app checks `NotificationManager.canUseFullScreenIntent()` before scheduling or attaching any full-screen intent (`FullScreenIntentPermission.canUseFullScreenIntent()`).
- **Automatic Fallback:** If the permission is denied or not granted by default, the app automatically falls back to a **high-priority heads-up notification** (`NotificationCompat.PRIORITY_HIGH`) with standard action buttons. The app **never crashes** and no reminder is lost.
- **Transparent User Settings:** Under **Settings > Notifications**, a dedicated **Full-screen alerts** row shows the current status ("Allowed" or "Permission required") with a one-tap link to the system `ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT` screen.

---

## 4. Instructions for Google Play Reviewers

1. Launch PixelQuest and navigate to **Settings > Notifications**.
2. Verify the **Full-screen alerts** row. Tapping it opens the system "Alarms & reminders / Full-screen alerts" special app access setting for PixelQuest.
3. Turn on **Alarm-style study reminders** in Settings.
4. Schedule a study party or task reminder for 1 minute in the future.
5. Lock the phone:
   - If full-screen intent is enabled: observe the full-screen prompt waking the device.
   - If full-screen intent is disabled: observe the high-priority notification waking the ambient display / heads-up banner with actions.
