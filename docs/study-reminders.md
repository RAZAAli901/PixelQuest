# Study reminders

Inside a study party ([study-party.md](study-party.md)), a member can make a reminder of their own: "Quiz this week", "Exam on Thursday", "Hand in the lab report at 10 am". It's for themselves, or for the whole party, and each phone alerts at the right times.

A reminder is not an item. An item is something due that members tick off and score points for; a reminder only reminds. It never touches points, XP, levels or streaks.

## A reminder

| Field | Rules |
|---|---|
| `kind` | `quiz`, `assignment`, `exam` or `custom` |
| `title` | 2 to 80 characters, trimmed |
| `scope` | `week`, `day` or `time` (below) |
| `week_start` | The Monday of the week: required for a week reminder, empty otherwise |
| `due_at` | A moment with its time zone: required for a day or time reminder, empty for a week one |
| `tz` | The creator's time zone, a name such as `Asia/Karachi`: which day and week the reminder means |
| `repeat` | `none`, `daily` or `weekly` |
| `audience` | `party` (every active member) or `me` (only its creator) |
| `alert_offsets` | Optional: the creator's own alert times, in minutes before the reminder's moment (below). Empty means the defaults. |
| `created_by`, `created_at` | Set by the server |

### Scopes

- **This week** (`week`): "Quiz this week". It belongs to a week, Monday to Sunday in `tz`, with no day of its own.
- **A day** (`day`): "Exam on Thursday". `due_at` is the start of that day in `tz`; the day is what counts.
- **A day and time** (`time`): "Hand in at 10:00 on Thursday". `due_at` is that moment.

### Repeating

- `none`: once.
- `daily`: the same time every day (a `day` or `time` reminder only).
- `weekly`: the same day and time, or the same week's span, every week.

A repeating reminder alerts for its next occurrence only; the one after is armed when that one has passed.

## When it alerts

Each alert is a moment, worked out on the phone in the reminder's `tz`.

| Scope | Default alerts |
|---|---|
| This week | Monday at 09:00, and Saturday at 18:00 (the day before the week's last day) |
| A day | The evening before at 18:00, and on the day at 07:00 (two hours before a 09:00 start) |
| A day and time | One day before, and 30 minutes before |

- **Custom times:** the creator can choose the alerts instead, as minutes before the reminder's moment (the start of its week or day, or its time). Up to 4 of them, each from 0 to 14 days before. Everyone the reminder is for gets the same alerts.
- **In the past:** an alert whose moment has passed is skipped, never fired late, except one missed while the phone was off within the last hour.

## Who sees what

- **Reading:** a party reminder is read by every active member of its study party; a `me` reminder only by its creator. Nobody else reads either, not even the leader.
- **Adding:** any active member adds a reminder, for the party or for themselves. The server fills in who and when.
- **Changing and deleting:** only its creator or the party's leader. Its party, creator and audience never change.
- **Leaving:** a member who leaves or is removed stops reading the party's reminders at once; their own `me` reminders go with their membership.

## On the phone

- **Cache:** reminders are kept on the phone (Room), like items, so they show and alert offline. Creating one offline waits and is sent when there's a connection.
- **Sync:** the phone fetches a party's reminders when its screen opens, when live updates say something changed, and every 30 minutes in the background (WorkManager, with a network), so a reminder made by someone else is armed even if the app stays closed.
- **Alarms:** each alert is an exact alarm when Android allows it, otherwise an inexact one in a 10-minute window, as quest reminders are. They're armed again after a reboot, a change of time zone or clock, an update of the app, an edit, a delete and every sync.
- **The notification:** the reminder's title and when it is, with **Mark done**, **Snooze 1 hour** and **Open** (the party's This week tab). Mark done stops the rest of that occurrence's alerts on this phone; it isn't shared.
- **Channel:** "Study reminders", its own Android notification channel. Each party can be muted on the phone, which keeps its reminders but posts none.
- **This week:** reminders of the current week show as a banner at the top of the party's This week tab.

## Privacy

A reminder is the party's (or its creator's) alone, under the same rules as items: row-level security on the server, default deny. Its text is never sent anywhere but the party's database and the phones of the people it's for, and a notification hides its text on the lock screen.
