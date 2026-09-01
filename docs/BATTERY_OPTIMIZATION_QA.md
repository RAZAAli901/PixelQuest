# PixelQuest Battery Optimization & Alarm Delay Verification

This document verifies app behavior under aggressive OEM Android battery optimization (Doze mode, Doze Standby, app hibernation).

## Verification Details

1. **Exact Alarm Mechanism**: `setExactAndAllowWhileIdle()` is invoked for exact quest triggers, allowing alarms to fire even while device is in deep Doze mode.
2. **WorkManager Fallback**: `MissedTaskWorker` is scheduled as a periodic worker with `setRequiresBatteryNotLow(true)` constraint to catch up missed task calculations when unthrottled.
3. **User Guidance in Settings**: `SettingsScreen` includes an explicit "⚙️ OS NOTIFICATION SETTINGS" button directing users to disable battery optimization for PixelQuest if alarms are delayed by vendor-specific power management.
4. **Result**: User guidance and fallback alarms verified adequate.
