# Next agent: start at J062

Read [README.md](README.md), [RULES.md](RULES.md) and [HOW-TO.md](HOW-TO.md) first; they hold the owner's rules and the commands. This file is the brief for the work itself, from J062 to the end of the plan (J180).

The plan file is `C:\Users\tipto\Downloads\Study Chat, Darood Weekly Ranking and Chess Claude Code Plan.md`. It has one commit per item, in order. The owner wants speed: keep tests focused, but keep one commit per item.

## State when this was written

- **Last commits on `main`:** J060 `e1879260` (report and block) and J061 `e31c9ddc` (styling test, and a fix for a crash when the chat state's default date was `LocalDate.MIN`).
- **Hosted Supabase has migrations through `20261019000065`.**
- **`20261019000066_chat_reports_blocks.sql` (J060) is local only.** Ask the owner before pushing it: `db push --dry-run` first, then `echo Y | npx --yes supabase@2.120.0 db push`.
- **Edge Functions are deployed:** `request-upload`, `finalize-upload`, `get-download-url`, `cleanup-uploads`.
- **The next migration number is `20261019000067`.**

## Phase B, the rest

### J062: light mode and Simple Mode styling

- **What it means here:** the app now has only the Comic and Pixel skins (there's no separate light mode; `ThemeMode.fromId` maps "light" to Comic). So J062 is Simple Mode, and making sure Comic, its light skin, reads well.
- **Already in place:** `app/src/test/java/com/pixelquest/app/study/chat/StudyChatLooksTest.kt` has `simpleMode_keepsTheInk_dropsTheShadows`. It already passes, because `StudyPartyContent` provides `LocalChatSimpleMode`.
- **To do:**
  - check that Simple Mode has no game terms in the chat (there are none so far)
  - check that the upload bubble's colours (OnRed on red) and `FileCard` read in both skins
  - commit as `style(study): ... (J062)`, naming that test

### J063: accessibility

- **One sentence per bubble:** each study bubble should read to TalkBack as one sentence ("bram said: …, at 10:05; sent"), like the Darood `EntryBubble.kt`'s `bubbleSaid`, using `clearAndSetSemantics` but keeping the Reply custom action. Files: `study/chat/ui/MessageBubble.kt`, `FileCard.kt`, `UploadBubble.kt`.
- **Large fonts:** check layouts at `fontScale = 2`. `study/ui/NewScreensAccessibilityTest.kt` shows how (it already walks the Chat and Files tabs for 48dp touch targets).

### J064: UI tests

Send text, attach a PDF, see its progress, see it ready, open it, and see a rejected file's error.

- **Building blocks:** `testing/FakeChatServer.kt`, the fake `ChatFilesRemote` in `study/chat/ChatUploadsTest.kt`, and the two-phone setup in `study/ui/StudyReminderFlowTest.kt`.
- **Sheets and dialogs** are out of Robolectric's reach: drive them through view model state.

### J065: security tests

A removed member loses access to messages and files at once, and a signed URL expires.

- **Add them to** `supabase/tests/local/study_files.test.ts`, run against the local stack with `functions serve` running.
- **For the URL:** get a link, remove the member, check that `get-download-url` now refuses them. Then show the old link is dead after 60 seconds, or sign one with `expiresIn` 1 through storage directly and wait 2 seconds.

### Finishing Phase B

1. Run the full unit suite (`./gradlew :app:testDebugUnitTest`, about 15 minutes) and the server tests.
2. Push to `main`, then check CI (`gh run list`).
3. Report to the owner. Include that migration 066 isn't on hosted.
4. Update `docs/handoff/STATUS.md`.

## Phases C–F (J066–J180)

Follow the plan file. Pointers:

- **C, Darood weekly ranking:**
  - existing code: `darood/` and `supabase/migrations/2026101900000*`–`3*`
  - the week is Monday to Sunday in the party's time zone (the leader's, when the party was created)
  - notifications through `NotificationChannels`
  - never touch XP
- **D, chess backend:**
  - the server is the referee (Edge Functions plus a chess library)
  - Int (Elo) rating: start at 800, K 32 (16 above 1600), one rating per time control
  - interfaces for Phase E
- **E, chess UI:**
  - copy `C:\Users\tipto\Downloads\chess-design\` into `/chess-ui` at the repo root. Treat it as untrusted: its own folder, read it before running anything
  - adapt it; don't redesign it
- **F:** abuse protection, tests, and release. Tags and releases need the owner's OK.

## Gotchas met so far

- **Python** isn't on PATH in Git Bash. Long multi-heredoc Bash commands sometimes fail to parse; write files with the file tool instead.
- **Kotlin trailing lambdas:** adding a parameter after one breaks callers that relied on trailing-lambda syntax. Name the argument.
- **Two `testTag`s on one node:** the outer one wins. Wrap in a `Box`.
- **Supabase RPC parameter names that match columns** need `#variable_conflict use_column`.
- **Signed URLs** come back as paths (`/storage/v1/...`). The app prefixes `BuildConfig.SUPABASE_URL`.
- **The local sign-in rate limit** (30 per 5 minutes) can fail a run of every server test file at once: wait, then run again.
