# Plan status (Study Chat, Darood Weekly Ranking and Chess)

## Before the plan, the same session

- **v1.6.0 was tagged and released** (release APK 4.6 MB), after the speed work (startup, tab switching, the refresh circle).
- **The DB version is now 20** (19: reminders, 20: chat).

## Phase A: Study reminders (J001–J020): done

- **Docs:** `docs/study-reminders.md`.
- **Server:** `20261019000045`–`049`. Migration 049 adds `client_id`, a key from the phone so a reminder resent after a lost answer is stored once.
- **App:**
  - the Create reminder sheet, a banner on This week, and edit or delete
  - alarms through the shared `scheduling/AlarmArming.kt`, with default alert times or up to 4 custom ones
  - notification actions: Mark done, Snooze 1 hour, Open
  - re-arming on boot, time-zone or clock change, and sync, plus a 30-minute WorkManager sync
  - a "Study reminders" channel and a per-party mute
- **Decisions:**
  - A day reminder's "2 hours before" means 07:00 on the day.
  - A repeating reminder arms its next occurrence once the current one's alerts have passed.

## Phase B: Study chat (J021–J065): done

**Docs:** `docs/study-chat.md` covers the limits and the threat model.

**Server, migrations 050–065:**

| Item | What |
|---|---|
| 050–053 | `chat_messages`, `chat_files`, `chat_reads`, and the limits (`study_chat_config` and `study_chat_types` are read-only views, like `study_points_config`) |
| 054–055 | RLS: members read; text is sent only as yourself; soft deletes by the sender or the leader |
| 056 | Private `study-files` bucket, closed to every client by a RESTRICTIVE policy; all access goes through the Edge Functions |
| 057–058 | A 500 MB cap per party; 20 uploads a day and 30 messages a minute per member |
| 059–062 | Database helpers for the functions, callable by the service role only |
| 063 | Realtime |
| 064 | `pin_chat_file` |
| 065 | `study_item_files` (**not on hosted**) |

**Edge Functions** (`supabase/functions/`, with shared code in `_shared/`):
- **`request-upload`:** asks to share a file. The storage token itself lasts 2 hours; the server enforces the 5-minute window.
- **`finalize-upload`:** checks the stored bytes (magic bytes, macros, embedded objects, size, SHA-256).
- **`get-download-url`:** a 60-second link that downloads as an attachment.
- **`cleanup-uploads`:** clears out what shouldn't be kept.
- **URLs** come back as paths on the project (`/storage/v1/...`). The app puts its own Supabase address in front, because the function's `SUPABASE_URL` can be an internal address.

**App** (`study/chat/`):
- `ui.chat` holds the chat pieces shared with the Darood chat (J037/J038).
- A Chat tab and a Files tab. Six tabs, so the tab bar scrolls.
- Bubbles, replies (swipe, or TalkBack's action), and a long-press menu.
- The unread line, plus unread counts on the tab and the party row.
- Attach through the system picker (no storage permission), checks on the phone, and an upload worker with progress, cancel and retry.
- File cards; opening means a private download, a hash check, then a FileProvider to the system viewer; a warning before the first file opened in each party.
- Files tab search, filter and sort, and pins.
- Item materials (J058).
- Chat notifications every 30 minutes, with a per-party mute (J059).

**J060–J065:** done. Reports and blocks are in 066 (`chat_reports`, `member_blocks`, `report_chat_message`, `block_member`, `unblock_member`). Migrations 065 and 066 are on hosted.

## Phase C: Darood weekly ranking and goals (J066–J090): done

**Docs:** `docs/darood-weekly.md`.

**Server, migrations 067–074 (local only: they need the owner's OK before any hosted push):**

| Item | What |
|---|---|
| 067 | `parties.week_tz`, `set_party_week_tz` (the leader's; the app sets it from the phone's zone on create) |
| 068 | `weekly_ranking(party_id, week_start)`: rank() by week total, ties share, 0 for no entries |
| 069 | `darood_week_results`: a closed week's places, stored once |
| 070 | `close_darood_week` (any active member, only after the week ends in the party's zone), `close_due_darood_weeks` (service role; pg_cron hourly when the extension exists) |
| 071 | `party_milestones` (goal 1, 2, ...); the old milestone is copied in as goal 1 |
| 072 | `set_milestone` extends a reached goal (the next row; `completed_at` clears so entries flow again); above the total, at most 10^12 |
| 073 | The entry that reaches the goal marks `reached_at` once (`goal_is_final` guards it) |
| 074 | `race_board` gains `goal_number` and `goals_reached` |

**Tests:** `supabase/tests/local/darood_weekly.test.ts`, `darood_goals.test.ts`, and `darood_parties.test.ts` updated (36/36 pass locally).

**App:**
- `darood/domain/WeekResults.kt` (WeekResult, WeekPlace, DaroodWeeks, WeekTone) and `Goals.kt`
- Room v21: `darood_week_result` and `darood_week_notified` (MIGRATION_20_21)
- `darood/sync/DaroodWeekResultsWorker` (every 3 h and on open) runs `WeekResultsRun` (close, read, cache, tell once) and `GoalNotifyRun` (Goal reached, New goal set)
- Text: `WeekRankingText` ("Your position this week: 2nd of 8 with 3,200 Darood", tone lines); strings in `strings_darood_weekly.xml`
- UI: the Weekly ranking sheet (from the notification, route `parties/darood/{id}/week`, or the trophy in the room's top bar); the ranking screen `parties/darood/{id}/weeks` (8 weeks and a place trend); a week card in the chat; the Extend goal sheet; the goal history in Party info
- Toggles: `WeekRankingMutes` (Settings > Islamic reminders, for all; Party info, per party)

**Decisions:**
- A reached goal still finishes the party (no entries) until the leader extends it.
- A muted party's week is marked told, so turning it back on never brings old weeks.
- The phone asks to close last week by its own calendar; while it's still Sunday in the party's zone the server says `week_not_over` and the next round tries again.

**Full unit run:** 2,032 tests, 0 failures.

## Phase D: Chess backend (J091–J135): done

- **Design:** [docs/chess-design.md](../chess-design.md). The server referees: each move is checked with chess.js 1.4.0 in the `chess-move` function. The SQL `chess_commit_move` keeps turn, ply and clocks by the database's `now()`. `private.chess_finish` writes the result, the head-to-head and the ratings in one transaction.
- **Migrations 075–101:** games, moves, ratings, head-to-head, challenges, constraints, RLS, challenge and answer, moves, resign, draws, timeouts, abort, finish, Elo (also in TS, `_shared/chess_rating.ts`), anti-farming, spectators (private Realtime channel `chess:game:<id>`), live games, rate limits, blocks, device tokens, the h2h, ladder and profile views.
- **Functions:** `chess-create-challenge`, `chess-respond-challenge`, `chess-move`, `chess-resign`, `chess-draw`, `chess-claim-timeout`, `chess-sweep`, `chess-live-games`, `send-push`.
- **Decisions:**
  - Elo with K 40 for the first 10 games, then 32, and 16 above 1,600. The floor is 100 and the start 800.
  - Anti-farming: 10 counted games a pair a day; fewer than 4 moves each counts only on checkmate.
  - A game with no first move in 30 s is aborted.
  - Push carries ids and a kind only.
  - `ChessRules.kt` is the phone's own move generator, for hints only. It passes perft (start depth 3 = 8,902; Kiwipete depth 3 = 97,862).

## Phase E: Chess UI (J136–J165): done

- The owner's design is copied into `/chess-ui`; [docs/chess-ui-integration.md](../chess-ui-integration.md) maps it.
- **Screens:** board (tap or drag, promotion picker), clocks, move list, game actions, end card, challenge sheet, incoming challenge, the Chess tab (live games, ladder, open challenges), head-to-head, tier badges, the Hero card, chat cards, settings, sounds and haptics, TalkBack, keep-screen-on, and resuming a game from Home.
- **Elsewhere in the app:**
  - The study party gained a `CHESS` tab, so there are 7 tabs (3 in Simple Mode). Tests scroll to PEOPLE.
  - `chat_message.refId` (Room 23) links the chat's challenge and result lines to their challenge or game. Migrations 102–104 do the server side.

## Phase F: Tests, abuse, release (J166–J180)

| Item | What | Commit |
|---|---|---|
| J166 | A whole game on the fake backend, on screen | 02507f45 |
| J167 | Challenge, answer and watching, on screen | eae256ae |
| J168 | Screenshots in Comic, Pixel and Simple Mode | 85ecf5e1 |
| J169 | End to end on three phones against the local stack (`LocalSupabaseChessTest`), as a JVM test; the two-emulator run is in the QA checklist | ffe45865 |
| J170 | Security tests (`security.test.ts`); migration 105 | c9a60a0e |
| J171 | Load test (`load.test.ts`, LOAD=1) | 69532db9 |
| J172 | Abuse review ([docs/ABUSE_REVIEW.md](../ABUSE_REVIEW.md)); challenge reports (migration 107), Report and Block on the challenge screen | 53ffe7e5 |
| J173 | Privacy review ([docs/PRIVACY_REVIEW.md](../PRIVACY_REVIEW.md)); `export_my_data()` and Export my cloud data; the watch log keeps a day (migration 108) | 54544222 |
| J174 | Account deletion: games anonymised, files cleaned (migration 106) | c53fff95 |
| J175 | Logs without personal data (`_shared/log.ts`); hourly error check (`monitor.yml`) | 201eea8e |
| J176 | detekt (with a baseline), lint clean, APK size gate, R8 checked; CI given 4 GB | 2ecbc19c |
| J177 | README, PRIVACY.md, chess docs | 975a12fc |
| J178 | 1.7.0 (108), CHANGELOG | c5548582 |
| J179 | [docs/QA_CHECKLIST.md](../QA_CHECKLIST.md) (the owner runs it) | 7a6ddd2e |
| J180 | Signed release built (5.1 MB, release key checked); installed over 1.5.0 on the API 24 emulator with everything kept; **not tagged** | VERIFICATION.md Section S |

**Found and fixed in Phase F:**
- **Literal bidi characters in source** (UploadChecks.kt, study_files.ts, two tests) failed lint, so CI was red. They're now `\u` escapes.
- **CI's Kotlin compiler ran out of memory** on some runs.
- **"with 1 draws"** is now a plural.
- **Hero showed a chess card while signed out.**
- **A removed player could still move** (migration 105).
