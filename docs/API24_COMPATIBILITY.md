# PixelQuest Android API 24 (Nougat) Compatibility Report

This document records the backward compatibility verification for minimum supported SDK level **API 24** (Android 7.0 Nougat).

## Verification Matrix

| Area | API 24 Behavior | Status |
|------|-----------------|--------|
| AlarmManager | `setExactAndAllowWhileIdle` supported without API 31 exact alarm permission restrictions | COMPLIANT |
| Java 8 / java.time | Core library desugaring (`desugar_jdk_libs` 2.1.5) bundles `java.time` into the APK for API 24-25 | COMPLIANT since 1.4.1 |
| SoundPool | `SoundPool.Builder` API (added in API 21) operates cleanly on API 24+ | COMPLIANT |
| Vector Drawables | `useSupportLibrary = true` enabled in `build.gradle.kts` | COMPLIANT |
| Notification Channels | `NotificationChannel` created safely with `Build.VERSION.SDK_INT >= Build.VERSION_CODES.O` checks | COMPLIANT |

## java.time before 1.4.1

The row above used to say COMPLIANT without desugaring being on. It wasn't true. From v1.0.0 to 1.4.0, every release used `java.time` (Room's converters among many) without it, and Android only has `java.time` from API 26. On Android 7.0 and 7.1 the app closed as it started, before its first screen:

```
java.lang.NoClassDefFoundError: Failed resolution of: Ljava/time/format/DateTimeFormatter;
    at com.pixelquest.app.data.local.Converters.<init>
    at com.pixelquest.app.data.local.AppDatabase_Impl...
    at com.pixelquest.app.PixelQuestApplication.onCreate
```

That's 1.4.0's release build on the API 24 emulator (2026-10-20). 1.4.1 turns desugaring on (`isCoreLibraryDesugaringEnabled`).

## How it's checked

- **Lint, on every push** (`.github/workflows/build.yml`). Its NewApi check knows which calls desugaring covers, and fails the build on any other call newer than API 24. `lintDebug` has 0 errors.
- **On a device:** the `PixelQuest_API24` emulator (AOSP Android 7.0), booted with `-port 5556` so it never meets the everyday emulator. See VERIFICATION.md, Section P.

