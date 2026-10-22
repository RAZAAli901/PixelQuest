# Logging policy (S055)

Logcat isn't private. Anyone with the phone and a USB cable can read it, and so can bug-report tools. PixelQuest treats it as public.

## Rules

1. **One way in.** App code logs only through `util/AppLog` (`d`, `w`, `e`). `android.util.Log`, `println` and `printStackTrace` are not used directly. `AppLogTest` fails if they are; `RecompositionTracker`'s `println` is debug-only.
2. **Messages carry no personal data.** Ids, a prayer, a date or an action name are fine. Never names, usernames, quest titles, chat text, file names, emails, tokens, keys, locations or server answers.
3. **Release builds:**
   - `AppLog.d` writes nothing.
   - `AppLog.w` and `AppLog.e` write the message and only the exception's class name. An exception's message or stack trace can quote a server answer or a file.
   - R8 removes every `Log.v`, `Log.d` and `Log.i` call, in the app and in the libraries compiled into it (`proguard-rules.pro`).
   - supabase-kt's own logger is off (`defaultLogLevel = LogLevel.NONE`).
4. **Debug builds** log everything, as before.
5. **Server side:** the Edge Functions log through `_shared/log.ts`, with events and statuses only, never a prompt, an answer, a token or an account id (handler tests check gemini-proxy).
6. **Crashes:** `PixelCrashHandler` writes the crash to the app's private storage (not backed up: STORAGE.md) and hands it to Android, which prints its usual fatal-exception report.

## Adding a log line

Ask whether it would be safe to paste into a public bug report. If not, log an id instead.
