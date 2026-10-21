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

## Phase B: Study chat (J021–J065): J021–J059 done

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

**Left:**

| Item | What |
|---|---|
| J060 | Report a message or file (reuse the report flow; the `public.reports` table exists) and honour a block list. No block list exists yet: decide the simplest safe option and write it in the commit body. |
| J061, J062 | Pixel/Comic chat styling; light mode and Simple Mode styling. Mostly already follows `chatLook()`. Check and fix gaps. |
| J063 | TalkBack descriptions for bubbles and files, and large-font layouts (study `MessageBubble.kt` against the Darood `EntryBubble.kt`'s `bubbleSaid`). |
| J064 | UI tests: send text, attach a PDF, see progress, ready, open, and a rejected file's error. |
| J065 | Security tests: a removed member loses access at once; a signed URL expires (add to `supabase/tests/local/study_files.test.ts`). |

After J065, run all the unit tests and report to the owner, with a reminder that 065 isn't on hosted yet.

## Phases C–F: not started

See the plan for each item.
