# Handoff: start here

This folder is for the next agent picking up PixelQuest. Read these in order:

1. **README.md** (this file): where things stand and what's left.
2. **[RULES.md](RULES.md)**: the owner's standing rules. They override your defaults.
3. **[STATUS.md](STATUS.md)**: the plan item by item, what's done, and the decisions made.
4. **[HOW-TO.md](HOW-TO.md)**: building, testing, the local Supabase stack, deploying, and how commits are made.
5. **[MAP.md](MAP.md)**: where the code for each feature lives.

([NEXT-J062.md](NEXT-J062.md) is the brief from the middle of Phase B, kept for its gotchas.)

## The project

PixelQuest is an Android app (Kotlin, Jetpack Compose, Room, Hilt, WorkManager) backed by Supabase (Postgres with row-level security, Storage, Edge Functions, Realtime). The repo is at `C:\Users\tipto\OneDrive\Desktop\PixelQuest` on Windows, with Git Bash for shell work. The GitHub repo is `RAZAAli901/PixelQuest`. The `main` branch is pushed directly and CI runs on every push.

## The plan: done but for the tag

The owner's plan is `C:\Users\tipto\Downloads\Study Chat, Darood Weekly Ranking and Chess Claude Code Plan.md`: 180 items (J001 to J180) in 6 phases, one commit per item.

| Phase | Items | State |
|---|---|---|
| A: Study reminders | J001–J020 | Done |
| B: Study chat with file uploads | J021–J065 | Done |
| C: Darood weekly ranking and goal extension | J066–J090 | Done |
| D: Chess backend | J091–J135 | Done |
| E: Chess UI (the owner's design, in `/chess-ui`) | J136–J165 | Done |
| F: Tests, abuse protection and release | J166–J180 | Done to J179. J180's signed build and install-over check are done; **the v1.7.0 tag waits for the owner's real-device QA** |

The version is **1.7.0 (versionCode 108)**. The Room database is at **version 23**.

## What's left

1. **The owner runs [docs/QA_CHECKLIST.md](../QA_CHECKLIST.md)** on real phones, and the results go into VERIFICATION.md (Section S).
2. **Then tag `v1.7.0`**, but only with the owner's OK; the release workflow builds, signs and publishes it. After it's out, move the README's download links and badge from v1.6.0 to v1.7.0, and replace its "Coming in 1.7.0" heading.
3. **Owner settings, none of which the code can do:**
   - **Push for challenges:** the Firebase project's `FCM_API_KEY`, `FCM_APP_ID`, `FCM_PROJECT_ID` and `FCM_SENDER_ID` in `local.properties` (and as GitHub secrets for CI builds), and the `FCM_SERVICE_ACCOUNT` function secret. Without them push is off; challenges still show in the chat and the Chess tab.
   - **Scheduled jobs:** `chess-sweep` (every minute: flags and stalled games) and `cleanup-uploads` need the service key stored in the hosted database for pg_cron. Until then, the phones claim timeouts themselves, and `request-upload` runs a small cleanup on each call.
   - **Monitoring:** the `SUPABASE_ACCESS_TOKEN` and `SUPABASE_PROJECT_REF` repository secrets turn on the hourly error check ([docs/MONITORING.md](../MONITORING.md)).

## The server

- **Hosted Supabase has every migration, through `20261019000108`.** The next new number is `109`. The owner approved pushing 067 onwards; ask again for anything new.
- **Deployed Edge Functions** (all `--no-verify-jwt`; each checks the caller itself):
  - `gemini-proxy`;
  - files: `request-upload`, `finalize-upload`, `get-download-url`, `cleanup-uploads`;
  - chess: `chess-create-challenge`, `chess-respond-challenge`, `chess-move`, `chess-resign`, `chess-draw`, `chess-claim-timeout`, `chess-sweep`, `chess-live-games`;
  - push: `send-push`.
- `supabase/functions/_shared/study_files.ts` changed after the file functions were deployed. The change was an escape-only edit to the name cleaner and behaves the same; it goes out with their next deploy.

## Before you start

1. Run `git log --oneline -15` and `git status`. `app/src/test/java/com/pixelquest/app/zz/` is a scratch folder: never commit it.
2. Check CI: `gh run list --limit 4`. It was red before J176 for two reasons, both fixed in J176: a lint error (invisible bidi characters in source) and the Kotlin compiler running out of memory (CI now gets 4 GB).
3. Start the local stack (see HOW-TO.md) if you'll touch SQL or functions.
