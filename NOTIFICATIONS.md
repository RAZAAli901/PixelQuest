# Notifications

How PixelQuest schedules and posts notifications, and what changed on Day 26.

## Pipeline

1. `TaskAlarmScheduler` arms one exact alarm per task (`setExactAndAllowWhileIdle`, request code = task id).
2. `TaskAlarmReceiver` fires, checks settings and the task, posts the reminder, then arms the next occurrence.
3. Reminder actions go to `TaskActionReceiver` (log completion or "not yet"); tapping opens `TaskPromptActivity`.
4. `MissedTaskWorker` (every 30 min) logs tasks still open two hours after their time and posts a missed-task notification.
5. `BootReceiver` re-arms every task after a reboot.

`ReminderSchedule` is the single place that works out the next trigger time from a task's first date, time and recurrence.

## Audit at the start of Day 26

| Area | Before Day 26 | Status |
| --- | --- | --- |
| Master toggle | Turning notifications off cancelled alarms, but alarms armed later (task create/edit, reboot) still posted. | Fixed (Step 8) |
| Simple Mode copy, sound, vibration | Settings existed but the receiver never passed them, so every reminder used gamified copy. | Fixed (Step 9) |
| Recurring tasks | Exact alarms fire once and nothing re-armed them, so a daily reminder fired one time only. | Fixed (Step 11) |
| Reboot | Trigger time added one day to the stored first date, so older tasks got alarms in the past that fired at once. | Fixed (Step 10) |
| Quick complete / skip | Cancelled the task's alarm entirely, ending all future reminders for it. | Fixed (Step 12) |
| Missed tasks | Logged silently; the missed-task notification builder was never called. | Fixed (Step 13) |
| Channels | One high-importance channel for everything. | Planned (Section C) |
| Per-task settings | None; every task reminded at its exact time. | Planned (Sections E–F) |

## Notification ids

| Notification | Id |
| --- | --- |
| Task reminder | `taskId` |
| Missed task | `taskId * 10 + 3` (replaces the reminder) |
| Reminder "yes" / "not yet" actions | request codes `taskId * 10 + 1` / `taskId * 10 + 2` |
