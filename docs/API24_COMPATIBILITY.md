# PixelQuest Android API 24 (Nougat) Compatibility Report

This document records the backward compatibility verification for minimum supported SDK level **API 24** (Android 7.0 Nougat).

## Verification Matrix

| Area | API 24 Behavior | Status |
|------|-----------------|--------|
| AlarmManager | `setExactAndAllowWhileIdle` supported without API 31 exact alarm permission restrictions | COMPLIANT |
| Java 8 / java.time | Desugaring / `java.time` backport compatibility verified via compileOptions | COMPLIANT |
| SoundPool | `SoundPool.Builder` API (added in API 21) operates cleanly on API 24+ | COMPLIANT |
| Vector Drawables | `useSupportLibrary = true` enabled in `build.gradle.kts` | COMPLIANT |
| Notification Channels | `NotificationChannel` created safely with `Build.VERSION.SDK_INT >= Build.VERSION_CODES.O` checks | COMPLIANT |
