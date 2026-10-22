# Manifest audit

S039, from the merged release manifest of 1.7.2 (the app's own `AndroidManifest.xml` plus its libraries'). The check in S042 keeps this list honest: a new exported component fails the build unless it's added to the allowed list with a reason.

## Permissions

| Permission | From | Why |
|---|---|---|
| `INTERNET` | app | Supabase, the AI proxy, Firebase |
| `ACCESS_NETWORK_STATE` | app, WorkManager | Workers that need the network wait for it (sync, sends) |
| `POST_NOTIFICATIONS` | app | Reminders, prayers, study, chat and chess notifications. Asked for when first needed |
| `SCHEDULE_EXACT_ALARM` | app | Quest and prayer reminders at the minute. See [PLAY_POLICY.md](PLAY_POLICY.md) |
| `RECEIVE_BOOT_COMPLETED` | app, WorkManager | Re-arm alarms after a restart |
| `USE_FULL_SCREEN_INTENT` | app | The "did you do it?" prompt for PROMPT-style reminders. See PLAY_POLICY.md |
| `ACCESS_COARSE_LOCATION` | app | Prayer times for the city; asked only when the player turns the Namaz Tracker on or taps Use my location |
| `WAKE_LOCK`, `FOREGROUND_SERVICE` | WorkManager | Its own job handling |
| `com.google.android.c2dm.permission.RECEIVE` | Firebase Messaging | Receiving push |
| `com.pixelquest.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | AndroidX Core | Signature-level; protects receivers registered at run time |

S049 checked each is used, and none is unused.

## Components

### Exported: reachable by other apps

| Component | Guard | Why it's exported |
|---|---|---|
| `MainActivity` | none: the launcher | The home-screen icon must start it |
| `scheduling.BootReceiver` | **was none** | Fixed in S040: not exported. System broadcasts still reach it |
| WorkManager `SystemJobService` | `BIND_JOB_SERVICE` (system only) | The job scheduler binds it |
| WorkManager `DiagnosticsReceiver` | `DUMP` (system/adb only) | Diagnostics |
| ProfileInstaller `ProfileInstallReceiver` | `DUMP` | Baseline profile installation |
| Firebase `FirebaseInstanceIdReceiver` | `c2dm.permission.SEND` (Google Play services only) | Push delivery |
| Google sign-in `RevocationBoundService` | its own signature permission | Google account revocation |

### Not exported

These are the app's own: `TaskPromptActivity`, `PushService`, `TaskAlarmReceiver`, `TaskActionReceiver`, `NamazAlarmReceiver`, `NamazActionReceiver`, `StudyReminderReceiver`, `StudyReminderActionReceiver`, `FileProvider` (opened chat files; grants per file, read-only), and the startup `InitializationProvider`.

The libraries also add non-exported components: Firebase's messaging and discovery services, WorkManager's alarm service and constraint proxies, the Credential Manager and sign-in activities, Room's invalidation service, and the datatransport components.

## Notes

- **`TaskPromptActivity`:** shows over the lock screen and turns the screen on (`showWhenLocked`, `turnScreenOn`), for PROMPT-style reminders. S041 hides the quest's name until the phone is unlocked.
- **The FileProvider** exposes only `cache/study-files/` (`res/xml/study_file_paths.xml`), and only to the app a file is handed to, with a read grant.
- **No WebView, no deep links** (S044, S045): the only intent filter that takes outside input is the launcher's.
