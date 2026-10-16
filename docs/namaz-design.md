# Namaz Tracker: design

The Namaz Tracker records the five daily prayers, works out their times on the phone, and (later) reminds the player as each one starts. It is opt-in: with it off, PixelQuest looks and behaves as before. Phases 3–6 of the upgrade plan build it; this is the design they follow.

## Data model (Phase 3)

Two new Room tables, added in one migration (database version 7 → 8).

**`namaz_log`**: one row per prayer per day.

| Column | Type | Notes |
| --- | --- | --- |
| `id` | INTEGER, primary key | |
| `date` | TEXT (`LocalDate`) | The *prayer day*, see below |
| `prayer` | TEXT | `FAJR`, `DHUHR`, `ASR`, `MAGHRIB`, `ISHA` |
| `status` | TEXT | `PENDING`, `PRAYED`, `MISSED` |
| `prayedAt` | INTEGER, nullable | Epoch millis when marked prayed |
| `xpAwarded` | INTEGER | What marking it earned, so an undo can take back exactly that (Phase 6; 0 until then) |

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
| `MarkPrayerUseCase` | Marks a prayer prayed. Refuses one that hasn't started, or a day other than the current prayer day or the one before (Phase 6 tightens this). |
| `UndoPrayerUseCase` | Back to pending (or missed, if its window has passed), on the same prayer day only |
| `AutoMarkMissedUseCase` + `NamazMissedWorker` | Writes `MISSED` for pending prayers whose window has passed; hourly, and when the tracker opens |
| `NamazMidnightWorker` | Just after midnight: closes the old day and adds five `PENDING` rows for the new one |

Both workers do nothing while the tracker is off.

## XP (Phase 6)

Through the existing XP, level and streak pipeline, not a second one. Suggested values, all in `NamazXpConfig`:
- 10 XP per prayer in its window, 5 if marked after the next prayer started;
- +30 for all five in a day;
- +100 for a 7-day all-five streak;
- at most 80 a day.

`xpAwarded` makes undo exact, and awarding is idempotent. Simple Mode logs prayers with no XP text.

## Reminders (Phase 5, outline)

- **Alarms:** exact alarms (`setExactAndAllowWhileIdle` / `setAlarmClock`) at each prayer's start, minus the offset, for the next 48 hours, through `NamazAlarmScheduler`.
- **Rescheduled** when settings, the place or the day change, and after boot, an update, or a time or time-zone change.
- **Notification actions:** Prayed, Snooze 10 min and Dismiss. Its own channel, `namaz_reminders`.
- **Plumbing:** the existing `TaskAlarmReceiver` and `BootReceiver` patterns are reused.

## Backups (Phase 3)

- **Backups carry Namaz** (format version 5): the logs and the settings, with the place.
- **Older files** have no Namaz data, so restoring one leaves the phone's Namaz data as it is.
- **Android's own backup** already includes the database, so it carries the new tables too.
