# Notifications

How PixelQuest schedules and posts notifications. Rewritten on Day 26.

## Pipeline

1. `TaskAlarmScheduler` arms one alarm per task (request code = task id). `ReminderSchedule` works out the next trigger from the task's first date, time, recurrence and lead time.
   - With "Alarms & reminders" allowed: `setExactAndAllowWhileIdle`.
   - Without it (Android 14 starts with it off): `setWindow` with a 10-minute window, and Settings shows an "Allow exact reminder times" prompt.
2. `TaskAlarmReceiver` fires, checks the master toggle and the task (deleted, inactive or reminder off means nothing is posted), posts the reminder unless the task already has a result for that day (Day 28), then arms the next occurrence.
3. Reminder actions go to `TaskActionReceiver`: "Yes, I did it" records the task as done through `TaskResultRecorder` (once per day, so a second Yes or completing in the app as well awards nothing extra); "Not yet" records nothing and only dismisses the reminder (Day 28; it used to mark the day missed); "Snooze 10 min" arms a separate one-off alarm. Tapping opens `TaskPromptActivity`, whose answers behave the same way.
4. `MissedTaskWorker` (every 30 min) records tasks still open two hours after their time as missed and replaces the reminder with a missed-task notification. It also checks yesterday, so quests at 22:00 or later are marked missed after midnight; those are recorded without a notification. A missed quest can still be completed from Today that day.
5. Once a task has a result (done or skipped, in the app or from a reminder), `TaskAlarmScheduler.clearReminder` removes its reminder or missed notice from the shade and cancels a pending snooze.
6. Alarms are re-armed on app launch, after a reboot (`BootReceiver`), and when the exact-alarm permission changes.

All `@HiltWorker` workers depend on `PixelQuestApplication` providing `HiltWorkerFactory` (the default WorkManager initializer is removed in the manifest).

## Channels

| Channel id | Name | Importance | Used for |
| --- | --- | --- | --- |
| `pq_reminders` | Task reminders | High | Reminders when reminder sound is on |
| `pq_reminders_silent` | Silent reminders | Low | Reminders with sound off, or tasks set to Silent |
| `pq_missed` | Missed tasks | Default | Missed-task notifications |
| `pq_progress` | Streaks and progress | Default | Reserved for streak nudges |
| `pq_coach` | AI Coach | Low | "Insight ready" when the AI Coach cooldown ends |
| `pq_sync` | Leaderboard sync | Low | Foreground notice while stats sync |

The pre-Day 26 channel `pixelquest_reminders_channel` is deleted on startup. Settings lists each user-facing channel with a link to its system page, where Android 8+ controls sound and vibration.

## Reminder content

- Title: category icon plus task name ("📚 Quest Time: Read 15 Pages"), or "Quest in 15 min" when the task has a lead time. Simple Mode uses "Time to do" / "Coming up in 15 min" and no game words.
- Expanded text: today's progress ("1 of 3 quests done today", with the streak in gamified mode), a streak-at-risk line when there is a streak and work left, then one encouragement line.
- Two or more reminders at once are grouped under an inbox-style summary ("2 quests due").

## Per-task reminder settings

Stored on `TaskEntity` (added by `MIGRATION_4_5`; existing tasks keep the defaults) and included in JSON backups.

| Field | Values | Effect |
| --- | --- | --- |
| `reminderEnabled` | true / false | Off cancels the task's alarm |
| `reminderLeadMinutes` | 0, 5, 15, 30, 60 | Remind early; if the lead window has already started, remind at the task time |
| `reminderStyle` | Standard, Silent, Full screen | Silent uses the silent channel; Full screen adds a full-screen intent (Android 14 may refuse it for non-alarm apps, and it then shows as a heads-up) |

## AI-written encouragement (optional)

Off by default and only available while AI Coach is on (Settings → AI Habit Coach → AI reminder messages).

- `EncouragementPackWorker` runs at most once a day with a network connection. It sends only the streak length, the 7-day completion rate and the number of active habits; task names are never sent.
- The call counts toward the existing AI caps (4 per day, 60 per month) when Gemini answered; an offline or refused attempt doesn't (Day 30). Failures are not retried.
- Release builds send it through the `gemini-proxy` Edge Function, which holds the Gemini key (see `docs/GEMINI_PROXY.md`).
- `EncouragementSanitizer` drops lines that are too long (over 90 characters), contain links or markup, or use game words in Simple Mode.
- Reminders use a pack from today or yesterday in the matching tone; otherwise they use the built-in lines in `StaticEncouragementBank`.

## Streak at risk (Day 31)

`StreakAtRiskWorker` is the sender for the "Streaks and progress" channel (`pq_progress`). It runs once a day around 19:00. It posts one notification only when all of these hold:

- notifications are on, and it's 18:00 or later (WorkManager can run a periodic job early);
- the player has a streak (current streak of 1 or more);
- quests are due today and the ones done aren't yet enough for a perfect day at the current difficulty.

It says how many more quests keep the streak: "🔥 Your 5-day streak is at risk!" / "Clear 2 more quests before midnight to keep the flame alive." Simple Mode reads "Your 5-day streak ends tonight" / "Complete 2 more tasks today to keep it going." It sends at most once a day (`pixelquest_streak_nudge` preferences), and tapping it opens the app. The logic is `StreakAtRisk` (pure, tested); the worker is tested with Robolectric.

## "Did you do it?" prompts

`TaskPromptActivity` is `singleTop`, so a second quest's prompt arriving while one is open reaches the open activity as a new intent. Since Day 30 it joins a queue (`PromptQueue`): answering or dismissing a prompt shows the next one, and the activity closes when the queue is empty. The queue survives rotation, and the same quest is queued only once. Before Day 30 the second prompt was dropped, so two quests due in the same minute showed only the first.

## Notification ids

| Notification | Id |
| --- | --- |
| Task reminder | `taskId` |
| Missed task | `taskId` with tag `missed` (Day 28; `taskId * 10 + 3` collided with other tasks' reminders) |
| Reminder group summary | `900001` |
| AI Coach insight ready | `900002` |
| Streak at risk | `900003` |
| Action / snooze request codes | `taskId * 10 + 1` (yes), `+ 2` (not yet), `+ 4` (snooze) |

## Day 26 audit: what was broken

| Problem | Fix |
| --- | --- |
| Every `@HiltWorker` failed to construct (`NoSuchMethodException`), so missed-task checks, streak evaluation and leaderboard sync never ran | Step 50 |
| On Android 14 exact alarms are off by default and the scheduler silently skipped the alarm, so reminders were never scheduled | Steps 47–49, 51 |
| Recurring tasks only counted on the day they were created (Today screen, streaks, missed checks) | Step 22 |
| Exact alarms fire once and nothing re-armed them, so a daily reminder fired one time | Step 11 |
| Quick complete / skip cancelled the task's alarm for good | Step 12 |
| Reboot re-arm computed triggers in the past, firing at once | Step 10 |
| The master toggle was ignored by alarms armed later (create, edit, reboot) | Step 8 |
| Simple Mode copy, sound and vibration settings were never passed to reminders | Step 9 |
| Missed tasks were logged silently; the notification builder was never called | Step 13 |
| One high-importance channel for everything, including the sync notice | Steps 15–17 |

## Known gaps

- Streak-at-risk nudge (Day 31): not yet seen on a device at 19:00; covered by `StreakAtRiskWorkerTest`.
- AI insights and the daily AI reminder-message pack were verified against live Gemini (`gemini-2.5-flash`) on 3 Oct 2026: the pack held five lines in the gamified tone, and a reminder ended with one of them.
