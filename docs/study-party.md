# Study parties

A study party is a party ([party-core.md](party-core.md)) where friends keep each other going through a school or university week. Members pin the assignments and quizzes due, tick them off, log study time, and compete on a friendly weekly scoreboard. It lives in the side menu's **Parties**, outside the Islamic section.

The shared party core handles who is in it: members, invitations, the leader, and the 2-30 limit. Everything below is the study party's own data, in its own tables.

## Items

An item is something due: an **assignment** or a **quiz**.

| Field | Rules |
|---|---|
| `kind` | `assignment` or `quiz` |
| `title` | 2 to 80 characters, trimmed |
| `subject` | Optional, up to 40 characters (for example "Physics") |
| `notes` | Optional, up to 500 characters |
| `due_at` | A moment, with its time zone. **A quiz needs one**; an assignment may have none. |
| `pinned` | The leader's highlight, shown at the top of the party. Only the leader pins or unpins. |
| `created_by`, `created_at` | Set by the server |

Who may do what:
- **Adding:** any active member may add an item.
- **Editing and deleting:** only its creator or the leader.
- **Pinning:** only the leader.

## Completing

- **Ticking off:** a member ticks an item off for themselves only. One completion per member per item.
- **Undoing:** a member may undo their own completion within **24 hours**. After that it's final, so the scoreboard can't be rewritten.
- **Local day:** a completion records the member's own day and time zone (`local_date`, `tz`), as a Darood entry does. That decides which week it counts in.
- **Offline:** the phone saves a completion first and sends it once it can. Sending it twice never counts it twice: it has its own key, as Darood entries do.

## Study time

- **A session:** 5 to 240 minutes, on a day (`local_date`, `tz`). It comes from the study timer, or minutes added by hand.
- **Corrections:** sessions can't be edited. A mistake stays, as a Darood entry does; the daily cap on points keeps it fair.

## Points

Points are the party's own. **They never touch XP, levels or streaks.** Every value lives in `StudyPointsConfig`, mirrored on the server.

| Action | Points |
|---|---|
| Complete an assignment | 10 |
| Complete a quiz | 15 |
| Finish an item a day or more before it's due | +5 |
| Log study time | 1 per 10 minutes, at most 12 a day |
| Weekly winner | The win is recorded on the board |

- **Study minutes:** they add up across a day's sessions before the cap applies. For example, 25 minutes and 40 minutes make 65 minutes, which earns 6 points.
- **Early bonus:** it uses the moment of completion and the item's `due_at`. An item with no due date gets no early bonus.

## Weeks

- **Each member's own week:** it runs Monday to Sunday in that member's time zone, using the day recorded with each completion and session.
- **The scoreboard:** it shows each member's points for their current week, ranked. Equal points share a place.
- **Closing a week:** a week closes once it has ended everywhere, that is, from the day after it ends plus one more day, since time zones run up to about a day apart. Then its winner, and any ties, are recorded in `study_week_winners`.
  - Any member may ask for a week to be closed. The server checks it has ended, and closing it again changes nothing. The simplest way to close weeks without a scheduler.

## Privacy

- **Members only:** only a study party's active members read its items, completions, sessions and scores. Outsiders and the invited read nothing of them.
- **Usernames:** shown as for any party, only to the people in it.
- **Not public:** nothing about a study party is on any public board.
- **Realtime:** items, completions and sessions are published to the party's members alone, for the activity feed.

## Notifications

All of them can be turned off.
- **Reminders:** an item the player hasn't finished, a day and two hours before it's due.
- **Weekly summary (opt-in):** on Sunday evening, the player's points and rank.

## In Simple Mode

The due list only, with no points, ranks or scoreboard text, as Simple Mode has no game terms.

## Where it is now (after Phase E)

**On the server** (`20261019000035` to `20261019000044`): the three tables, their limits and row-level security, the points view, closing a week, and Realtime. Tested in `supabase/tests/local/darood_parties.test.ts`, and the app's calls in `LocalSupabaseStudyTest` (opt-in).

**On the phone**, in `com.pixelquest.app.study` (Room version 18 adds the study tables):

| Piece | What |
|---|---|
| `domain` | `StudyItem`, `StudyCompletion`, `StudySession`, `WeekScore`, `WeekWinner`; `StudyRules`, `StudyPointsConfig` (held equal to the server's by a test), `StudyRefusal` |
| `data` | `StudyRepository`: the cache first, ticks and study time saved then sent with their keys; `remote.StudyRemoteDataSource`; `local.StudyDao`; the timer, reminder and winner-seen stores |
| `sync` | `StudySyncWorker` (sends what waits), `StudyLiveSource` (Realtime), `StudyDueWorker` (reminders), `StudyWeeklyWorker` (the Sunday summary) |
| `ui` | `StudyPartyScreen` (This week, All items, Scoreboard, People), `StudyItemScreen`, the Add and Edit sheet, the timer, the board and past weeks, the activity feed, and the Home card |

**Routes:** `parties/party/{partyId}` and `parties/party/{partyId}/item/{itemId}`; the party's info stays at `parties/party/{partyId}/info`.

**Since, in Phase F, Phase A, and Phase T:**
- End to end: `StudyEndToEndTest`, a party created, joined, pinned, finished and won on two phones; and `StudyPartyFlowTest`.
- A TalkBack and large-text pass on every screen (`NewScreensAccessibilityTest`).
- Settings' Islamic card has the due reminders' and Sunday summary's switches too, and Parties has a first-run tip.
- Signing out sends waiting ticks and study time first, then clears the party, its rows, the timer and what was seen.
- **24-Hour Due Alarms (Phase A)**: Exact alarm alerts 24 hours before due dates with full-screen intent, alarm channel sound/vibration, and actions Done, Snooze, Open. See [study-alarms.md](study-alarms.md).
- **Ongoing Study Timer (Phase T)**: Background foreground service with system chronometer notification, actions Pause/Resume/Stop, session saving (5-240m), single active timer across circles, and reboot restore. See [study-timer.md](study-timer.md).

**Simplest choices made:**
- Items, edits and undoing a tick already sent need a connection; ticks and study time don't.
- A due date is picked from the next four weeks, and a time from the hours of the day.
- Realtime brings new and changed rows; what's taken away shows at the next refresh.
- Tapping a notification opens the app and directly navigates to the active study circle.

