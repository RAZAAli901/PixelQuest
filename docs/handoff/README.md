# Handoff: start here

This folder is for the next agent picking up PixelQuest. Read these in order:

1. **README.md** (this file), then **[NEXT-J062.md](NEXT-J062.md)**: the brief for J062 onwards.
2. **[RULES.md](RULES.md)**: the owner's standing rules. They override your defaults.
3. **[STATUS.md](STATUS.md)**: the plan item by item, what's done, what's left, and the decisions made.
4. **[HOW-TO.md](HOW-TO.md)**: building, testing, the local Supabase stack, deploying, and how commits are made.
5. **[MAP.md](MAP.md)**: where the code for each feature lives.

## The project

PixelQuest is an Android app (Kotlin, Jetpack Compose, Room, Hilt, WorkManager) backed by Supabase (Postgres with row-level security, Storage, Edge Functions, Realtime). The repo is at `C:\Users\tipto\OneDrive\Desktop\PixelQuest` on Windows, with Git Bash for shell work. The GitHub repo is `RAZAAli901/PixelQuest`. The `main` branch is pushed directly and CI runs on every push.

## What's being worked on

The owner's plan is `C:\Users\tipto\Downloads\Study Chat, Darood Weekly Ranking and Chess Claude Code Plan.md`. It has 180 items (J001 to J180) in 6 phases, one commit per item. Read it.

| Phase | Items | State |
|---|---|---|
| A: Study reminders | J001–J020 | Done, tested, on main |
| B: Study chat with file uploads | J021–J065 | J021–J061 done; **J062–J065 left: see NEXT-J062.md** |
| C: Darood weekly ranking and goal extension | J066–J090 | Not started |
| D: Chess backend | J091–J135 | Not started |
| E: Plug in the chess UI | J136–J165 | Not started. The owner's UI is in `C:\Users\tipto\Downloads\chess-design\`; per the plan it goes into `/chess-ui` at the repo root before Phase E |
| F: Tests, abuse protection and release | J166–J180 | Not started |

## Exactly where it stopped

- The last commit is J059 (chat notifications), `457c55ef` or later.
- **Next: J060**, reporting a message or file (reusing the report flow) and honouring the block list. The long-press menu already has a Report item that stays hidden until `ChatActions.onReport` is set (`study/chat/ui/MessageMenu.kt`, `StudyChatPane.kt`).
- Then J061–J063 (styling and accessibility passes), J064 (UI tests), J065 (security tests). After that comes the Phase B report to the owner, then Phase C.

## The server

- **Hosted Supabase has migrations through `20261019000065`.** `066` (reports and blocks) is local only.
- **`20261019000065_study_item_files.sql` (J058) is local only.** Pushing it needs the owner's OK, like every hosted push.
- **Deployed Edge Functions:** `request-upload`, `finalize-upload`, `get-download-url`, `cleanup-uploads` (all with `--no-verify-jwt`; each checks the caller itself), plus the older `gemini-proxy`.
- **No schedule calls `cleanup-uploads` yet.** `request-upload` runs a small cleanup round on each call instead. A schedule needs the service key stored in the hosted database, which needs the owner's OK.

## Before you start

1. Run `git log --oneline -15`, and `git status` (`app/src/test/java/com/pixelquest/app/zz/` is a scratch folder: never commit it).
2. Check CI: `gh run list --limit 4`.
3. Start the local stack (see HOW-TO.md) if you'll touch SQL or functions.
