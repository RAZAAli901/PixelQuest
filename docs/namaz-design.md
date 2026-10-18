# Namaz Tracker: design

The Namaz Tracker records the five daily prayers, works out their times on the phone, reminds the player as each one starts, and adds XP for them to the game's own XP. It is opt-in: with it off, PixelQuest looks and behaves as before. Phases 3–6 of the upgrade plan built it, and it shipped in 1.3.0. This page describes it as built.

## Data model (Phase 3)

Two new Room tables, added in one migration (database version 7 → 8). Phase 6 adds one column (8 → 9).

**`namaz_log`**: one row per prayer per day.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | INTEGER, primary key | |
| `date` | TEXT (`LocalDate`) | The *prayer day*, see below |
| `prayer` | TEXT | `FAJR`, `DHUHR`, `ASR`, `MAGHRIB`, `ISHA` |
| `status` | TEXT | `PENDING`, `PRAYED`, `MISSED` |
| `prayedAt` | INTEGER, nullable | Epoch millis when marked prayed |
| `xpAwarded` | INTEGER | The prayer's own XP from marking it (10, 5, or less at the daily cap), so an undo takes back exactly that |
| `bonusXp` | INTEGER, default 0 | The bonuses this prayer unlocked: the all-five +30 and any week's +100 (version 9) |

A unique index on (`date`, `prayer`) keeps one row per prayer per day.

**`namaz_settings`**: a single row (`id` = 1).

| Column | Default | Notes |
| --- | --- | --- |
| `enabled` | false | The tracker's on/off switch (Phase 4). The side menu's entry also needs `SettingsRepository.namazAvailable`. |
| `method` | `KARACHI` | University of Islamic Sciences, Karachi. Also `MUSLIM_WORLD_LEAGUE`, `NORTH_AMERICA` (ISNA), `UMM_AL_QURA`, `EGYPTIAN`. |
| `asrMadhab` | `HANAFI` | or `SHAFI` |
| `highLatitudeRule` | null | null means the library's recommendation for the latitude |
| `latitude`, `longitude`, `city` | null | The last known place (cached so times work offline). Null falls back to Lahore (31.5204, 74.3587). |
| `reminderOffsetMin` | 0 | Minutes before each prayer (Phase 5) |
| `remindFajr` … `remindIsha` | true | Per-prayer reminders (Phase 5) |
| `adjustFajr` … `adjustIsha` | 0 | Minutes added to each time, for local custom |
| `xpEnabled` | true | Whether prayers earn XP (Phase 6) |

All the settings columns arrive in this one migration, so later phases don't need another.

## Prayer times (Phase 3)

- **Engine:** the Adhan library (`com.batoulapps.adhan:adhan2`, version 0.0.5). It calculates on the phone from coordinates, date, method and madhab, so it works offline.
  - Version 0.0.5 because 0.0.6 and later are built with Kotlin 2.2+, which this project's Kotlin 2.0 compiler can't read.
- **Wrapping:** `PrayerTimesCalculator` turns `NamazSettings` and a date into `PrayerDayTimes`: Fajr, sunrise, Dhuhr, Asr, Maghrib and Isha as `ZonedDateTime` in the phone's time zone. Each time includes the per-prayer adjustment.
- **Providing:** `PrayerTimeProvider` gives today's and tomorrow's times (`DefaultPrayerTimeProvider`), using the cached place or the Lahore fallback.
- **Prayer day:** a moment before that day's Fajr belongs to the *previous* day's prayers. Isha marked at 00:30 counts for yesterday, and after Isha the next prayer is tomorrow's Fajr.
- **Windows:** each prayer starts at its time. It is missed once the next one starts (Fajr: when Dhuhr starts; Isha: when the next day's Fajr starts). Sunrise is shown but isn't a prayer.

## Location (Phase 3)

- **Coarse location only** (`ACCESS_COARSE_LOCATION`): prayer times don't need more than city-level accuracy. The app asks only when the player turns the tracker on or taps "Use my location", after a short explanation.
- **Read with Android's own `LocationManager`.** The app doesn't include Google Play location services.
- **Refused, or no fix:** the player picks a city from a short built-in list, or the tracker uses the last cached place, else Lahore.
- **Privacy:** the coordinates stay on the phone. They're in the app's database, so they're in its backups (exported files and Android's own backup), and nowhere else.

## Logic (Phase 3)

| Use case | What it does |
| --- | --- |
| `NextPrayerUseCase` | The current prayer, the next one and the time until it, across the day boundary |
| `GetTodayNamazUseCase` | Today's times with each prayer's status; `PENDING` shows as `MISSED` once the next prayer has started |
| `MarkPrayerUseCase` | Marks a prayer prayed and awards its XP. Refuses one that hasn't started, a day other than the current prayer day or the one before, and a phone clock behind the last mark (see XP) |
| `UndoPrayerUseCase` | Back to pending (or missed, if its window has passed), on the same prayer day only, taking back its XP |
| `AutoMarkMissedUseCase` + `NamazMissedWorker` | Writes `MISSED` for pending prayers whose window has passed; hourly, and when the tracker opens |
| `NamazMidnightWorker` | Just after midnight: closes the old day and adds five `PENDING` rows for the new one |

Both workers do nothing while the tracker is off.

## XP (Phase 6)

Prayers earn XP through the game's own XP path (`UserProfileRepository.addXp`, as a quest does), not a second system.

### Rewards
All in `NamazXpConfig`, worked out by `NamazXpCalculator` (pure):

| Reward | XP | When |
| --- | --- | --- |
| A prayer in its time | 10 | Marked before the next prayer starts (Isha: before the next Fajr) |
| A prayer marked late | 5 | Marked after that, which the day before allows |
| All five | +30 | With the day's fifth prayer |
| A week | +100 | With the fifth prayer of each 7th, 14th, 21st… day in a row with all five |
| Daily cap | 80 | The prayers and the all-five bonus together. The week's bonus comes on top, or the cap would block it |

### Storing and undoing
- **Stored apart:** a mark writes the prayer's own XP to `xpAwarded` and the bonuses it unlocked to `bonusXp`, then adds both to the profile.
- **Undo** takes back that prayer's XP and every bonus on its day, and clears them. Undoing any prayer of a finished day means the day no longer has five. Marking it again earns them again, once.
- **Once only:** marking and undoing share one lock (`NamazXpLock`), so the reminder's Prayed button and the screen's button pressed together award once.
- **The XP switch** (`xpEnabled`, on by default): off, prayers are recorded with no XP.

### What XP doesn't do
- **Levels:** in PixelQuest these come from perfect quest days (`StreakEvaluationWorker`), never from XP. Prayer XP raises the total but not the level. A level-up that is waiting shows its celebration on the Namaz screen too.
- **Streaks:** the Namaz streak (days in a row with all five) is its own. It doesn't change the quest streak.

### Fair play (`NamazRules`)
- A prayer can't be marked before it starts, for a day after the current prayer day, or more than a day back. Undo is for the current prayer day only.
- **Clock set ahead, then back:** a mark is refused, with a message, if the phone's clock reads more than 10 minutes before the latest mark around the current day. Setting the clock ahead marks prayers early. Setting it back again would let the same prayers be undone and marked for XP over and over, so marking waits until the real time catches up.

### Where it shows
- **The prayer's XP** pops up over the screen as it's marked, in the theme's style.
- **The fifth prayer** shows a card with a burst: "All five prayed!", the +30, and any week's +100.
- **The shown day's XP** appears under the progress bar ("+80 XP today"), and today's appears in the side menu's header.
- **Stats** has a Namaz card: its XP, the last 7 days' XP, and prayers prayed.
- **Simple Mode** hides every XP number and the XP switch, but prayers are still recorded and still earn XP.
- **The leaderboard** gets only the profile's totals, as before. Which prayers were marked, and when, never leaves the phone (`NamazLeaderboardPrivacyTest`, PRIVACY.md).

## Reminders (Phase 5)

A notification at the start of each prayer, even with the app closed, after a reboot, and across time-zone or place changes. It reuses the task reminders' plumbing: `AlarmManager` exact alarms, a manifest receiver, and `BootReceiver` for re-arming.

### Schedule

- **What's armed:** for the next 48 hours, one alarm per prayer at its start minus the reminder offset (at the time, or 5, 10 or 15 minutes before), for each prayer whose reminder is on. Optionally, a "still not prayed" nudge 20 minutes before the next prayer starts.
- **Planned, then armed:** `NamazReminderPlanner` works out the list (pure, testable), and `NamazAlarmScheduler` arms it.
  - Exact alarms use `setExactAndAllowWhileIdle(RTC_WAKEUP)`. Fajr, when it may sound through Do Not Disturb, uses `setAlarmClock`.
  - Without "Alarms & reminders" (Android 12+), it falls back to a 10-minute `setWindow`, and the screen says reminders may be late.
- **One identity per alarm:** each alarm's `PendingIntent` has its own action per kind (reminder, nudge, snooze, test) and a request code from the kind, the date and the prayer. Arming the same one again replaces it (`FLAG_UPDATE_CURRENT`), so rescheduling never doubles an alarm.
- **Rescheduling** cancels what's armed in the window and arms the new plan. It happens:
  - when the tracker's settings, place or reminder options change (watched from `PixelQuestApplication`);
  - at the day's change (the midnight worker);
  - after each alarm fires (the window rolls on);
  - from `BootReceiver`: `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIMEZONE_CHANGED`, `TIME_SET`, and the exact-alarm permission changing;
  - daily, from `NamazRescheduleWorker`, as a safety net.
- **Switched off:** turning the tracker (or every reminder) off cancels every Namaz alarm and removes its notifications and channels.

### Fire

- **`NamazAlarmReceiver`** (manifest, `exported=false`) checks that the tracker and that prayer's reminder are still on, and that it isn't already prayed. Then it posts the notification and rolls the schedule on.
- **The notification:**
  - its title is "Time for Fajr", and its text is the time and the place;
  - tapping it opens the Namaz screen;
  - it has three actions, Prayed, Snooze 10 min and Dismiss, handled by `NamazActionReceiver`.
- **Prayed** marks the prayer through `MarkPrayerUseCase`, the same as the screen's button, so it gets the screen's rules and, from Phase 6, its XP.
- **Snooze** arms a one-off alarm 10 minutes later.
- **The nudge** posts only if the prayer is still unmarked.

### Channels and sound

- **Channels:**
  - `namaz_reminders` is high importance, with the chosen sound;
  - `namaz_reminders_silent` has no sound or vibration;
  - `namaz_nudge` is low importance.
- **Sound** is fixed per channel on Android 8+, so each sound option has its own channel. The options are the system sound, a short bundled chime, or silent.
- **No Adhan recording is bundled.** That needs a licensed recording; the bundled sound is a short chime made for the app.
- **Do Not Disturb is respected:** reminders are `CATEGORY_REMINDER`. An opt-in lets Fajr through, as an alarm-category notification on its own channel with `setAlarmClock`, which DND's default "Alarms" exception allows. Apps can't set a channel's own DND bypass.

### Options

- **In the Namaz settings:** the per-prayer toggles and the reminder offset are in `NamazSettingsEntity` (and in backups).
- **In SharedPreferences (`pixelquest_namaz_reminders`):** the nudge, the sound and the Fajr DND opt-in. That avoids a database migration for three switches. A restore keeps the phone's.

### Permissions

- **Notifications** (Android 13+): asked with a short explanation when reminders are first turned on. While they're blocked, the screen says so with a way to fix it.
- **Exact alarms** (Android 12+): a card links to "Alarms & reminders". Without it, reminders arrive within 10 minutes.
- **Battery:** a dismissible card links to the battery-optimisation list, so the phone doesn't hold reminders back.

### As built

- **Fired once:** a reminder or nudge is recorded as it fires (`PrefsNamazDeliveries`, `pixelquest_namaz_delivered`, the last few days). A reschedule skips it, so moving the reminder time later, or a time-zone change, can't remind a prayer twice.
- **Off means off:** turning the tracker off cancels every Namaz alarm (snoozes too), takes its notifications away and deletes its channels. That happens once, as it goes off, and not again at every launch while it stays off.
- **The app's NOTIFICATIONS switch:** while it's off, nothing is armed for the prayers either.
- **The Namaz screen's reminder checks** appear while any prayer's reminder is on: exact alarms not allowed, notifications blocked, and battery optimisation (that tip can be hidden for good, in `pixelquest_namaz_reminders`).
- **R8:** the receivers and the Namaz enums (stored by name in intents, preferences and the database) are kept in `proguard-rules.pro`.

### Checking it on a phone

- **Debug builds:** the Namaz settings sheet has *Test reminder in 10 seconds*. It arms a real alarm for the current prayer, so the whole path shows: the alarm, the receiver, the notification and its buttons.
- **Instrumented test:** `NamazReminderInstrumentedTest` posts a real reminder and marks it from the Prayed button, on an in-memory database. Run it with `adb shell am instrument -w -e class com.pixelquest.app.notification.NamazReminderInstrumentedTest com.pixelquest.app.test/androidx.test.runner.AndroidJUnitRunner`. Don't use `connectedDebugAndroidTest`, which uninstalls the app and wipes its data.
- **Reboot:** with reminders on, `adb reboot`. Then `adb shell dumpsys alarm | grep pixelquest` lists the Namaz alarms armed again by `BootReceiver`.

## Backups (Phase 3)

- **Backups carry Namaz** (format version 5): the logs, with each prayer's XP and bonus, and the settings, with the place.
- **Older files** have no Namaz data, so restoring one leaves the phone's Namaz data as it is.
- **Android's own backup** already includes the database, so it carries the new tables too.

## Screenshots

Still to be captured for the 1.3.0 release page. Each is a Pixel 6 sized phone in Comic Pop unless it says otherwise. Each goes in `docs/screenshots/namaz/` under the name given:

| File | What it shows |
| --- | --- |
| `menu.png` | The side menu open from Home, with the Namaz Tracker entry and today's prayer XP in its header |
| `off.png` | The Namaz screen with the tracker off: the explainer and the switch |
| `setup.png` | The first-time setup sheet: location, method and Asr |
| `day.png` | A day under way: the Hijri date, the next-prayer countdown, two prayers prayed, the rest locked, "+20 XP today" |
| `xp-popup.png` | The "+10 XP" pop-up just after marking a prayer |
| `all-five.png` | The all-five card with its burst and "+30 XP bonus" |
| `week.png` | The seven-day strip with its dots, and the streak |
| `settings.png` | The Namaz settings sheet: method, madhab, adjustments, reminders, XP switch |
| `reminder.png` | The "Time for Asr" notification with Prayed, Snooze 10 min and Dismiss |
| `stats.png` | Stats with the Namaz card |
| `pixel.png`, `light.png`, `simple.png` | The day screen in Retro Pixel, Light and Simple Mode (no XP) |
