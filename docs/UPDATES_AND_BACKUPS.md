# Updates and backups

How PixelQuest keeps a player's progress through updates, reinstalls and backups, and what to do when changing the app.

## For players

### Updating
An update keeps everything:
- quests and their results;
- streaks, XP, level and level history;
- settings;
- your cloud account and leaderboard choice.

That's tested from every earlier version of the app's database (version 1, then v1.0.x and v1.1.0 to today), and was tried on a device from v1.1.0 (VERIFICATION.md, Section M).

When an update changes how the game is stored, PixelQuest first keeps a copy of the old file on the phone, until the next such update. It's there in case an update still went wrong, so a fixed one could bring the game back.

### Upgrading from 1.1.0 or earlier
Android won't install today's release over 1.1.0, because the older releases were signed with a temporary key (RELEASE_SIGNING.md). So:
1. In the old app: Settings → **EXPORT QUEST DATA**, and save the file.
2. Uninstall the old app and install the new one.
3. On the welcome screen, tap **I HAVE A BACKUP** and pick the file. The dialog says what it holds before you confirm, and your hero takes the place of the welcome steps. (Settings → **IMPORT QUEST DATA** works too, after starting a hero.)

A 1.1.0 backup brings back:
- your hero, level and progress toward the next level;
- your difficulty and streak;
- every quest.

It can't bring back what 1.1.0 didn't save:
- each day's results (the history behind Stats);
- the level history;
- your settings.

Set the settings again once. Backups made from now on include them.

### What a backup holds
| In the file | Not in the file |
| --- | --- |
| Hero, level, XP, progress to the next level | Your cloud account, leaderboard choice and public name (they belong to the account; sign in again) |
| Difficulty, streaks, perfect days | The AI Coach consent (you give it again) and saved AI insights |
| Quests with their reminders and weekdays | Sign-in sessions and keys |
| Every day's results, and the level history | |
| Settings: sound, CRT, haptics, reduce motion, notifications, theme, Simple Mode | |
| Namaz Tracker: the prayers marked, and its settings with its place | |

A backup from before the Namaz Tracker leaves the phone's prayer log and Namaz settings as they are. A backup made by a newer version restores what this version knows, and Settings says the rest was left out.

### Saving, restoring and undoing
- **Saving:** EXPORT QUEST DATA saves a dated file, such as `pixelquest-backup-2026-10-12.json`.
  - The app reads the file back to check it's complete before saying "Backup saved."
  - Settings shows when the last good backup was saved.
- **Restoring:** the dialog says what the file holds, and which PixelQuest saved it, before anything is replaced.
- **Undoing:** before restoring, the app keeps what the phone had. **UNDO LAST RESTORE** in Settings puts it back, until the next restore replaces that copy.
  - It's kept only when there was progress to go back to. A restore on the welcome screen, or into a game not yet played, has nothing to undo.

### Automatic backups on the phone
Once a day, and again an hour after you open the app, PixelQuest keeps a backup of your progress in its own storage on the phone. It keeps one per day, for the last 3 days. Nothing is saved before you've played (some XP, a level, or a completed quest).
- If the database file is ever corrupted, Android deletes it and the game starts over empty. The next time you open PixelQuest, it asks **RESTORE YOUR GAME?** with the newest automatic backup, what it holds and its day.
  - **NOT NOW** leaves the game as it is. That backup isn't offered again.
  - If the app hadn't yet recorded that you'd finished the welcome steps, the welcome screen offers **RESTORE MY GAME FROM 11 OCT** instead.
- **RESET ALL PROGRESS** is on purpose, so the old game isn't offered after it. Starting a new hero instead of the welcome screen's offer is the same.
- Settings → **Automatic backups on this phone** restores any of them, after a mistake too, with the same confirmation and undo as a file.
- These copies stay on the phone. They are not your exported file.

### Android's own backup
If backup is on in your phone's settings, Android saves PixelQuest's progress and settings with the rest of the phone's backup, and restores them after a reinstall or on a new phone.
- **What's included:** only the quest database, the settings, a pending level-up, and which account last signed in (`res/xml/backup_rules.xml` and `data_extraction_rules.xml`).
- **What's never included:** your sign-in session, so you sign in again. Nor AI Coach data, reminder bookkeeping, the automatic backups, the undo copy, or the copy kept before an update.

## For developers

### Changing a database table
The layouts players have are in `app/schemas` (one JSON per version; the README there says where the older ones came from).
1. Bump `version` in `AppDatabase` and write a `Migration(old, new)`.
2. Add it to `AppDatabase.ALL_MIGRATIONS`, the one list the app and the tests use.
3. Build. Room writes the new `N.json`. Commit it.
4. Run `DatabaseUpgradeTest`. It builds every earlier version's database with a played game in it, then opens it the way the app does, so Room checks the result.

Before Room migrates an older file, `DatabaseModule` copies it (with `-wal` and `-shm`) to `files/pre-update/pixelquest-v<old>.db` (`PreUpdateCopy`, which reads the version from the SQLite header without opening the file). The next layout change replaces the copy. A failed copy never stops the game from opening.

Never change a shipped version's layout without a new version. The build would rewrite that version's file, and CI's **Database layouts match the code** step fails. On a phone, Room would refuse to open the database at every start.

There's no wipe-and-start-over for updates any more (`fallbackToDestructiveMigrationOnDowngrade` only). A missing migration fails loudly at start, with the data intact, instead of quietly deleting it.

### Changing the backup format
- Bump `DataExportImport.FORMAT_VERSION`, add the change to the history in its comment, and keep reading older files.
- Read new fields with defaults, as older files won't have them.
- Add a test with an old file as the old app wrote it. `OldBackupRestoreTest` has v1.0.0's and v1.1.0's.

### Trying an update on a device
Unit tests check the database and backups. Before a release, also try a real update, which covers what they can't: the start-up path, onboarding, alarms. `scripts/update-check.sh <from> [to]` builds the two versions as a separate app (`com.pixelquest.app.updatecheck`), so the installed PixelQuest and its data aren't touched. The script prints the install and update commands.

On Day 34, `scripts/update-check.sh v1.1.0` found a real bug. The database came through, but every 1.1.0 player was sent through onboarding, and finishing it reset their level and XP (fixed: `OnboardingGate`). After the fix the update keeps everything (VERIFICATION.md, Section M).

The same day's device run of the backup features found three more things unit tests had missed:
- The daily backup saved a fresh install's empty game, because missed starter quests counted as play.
- An undo after a welcome-screen restore would have put the empty starter hero back.
- A deleted database opened an empty dashboard rather than the welcome screen, because the onboarding flag lives in the settings, which survive. So the automatic backup was never offered (now `LostProgress`).

To try the last one, delete `databases/pixelquest.db`, `-wal` and `-shm` with `adb shell run-as com.pixelquest.app.updatecheck rm -f ...` (one file per command; `run-as` doesn't expand `*`).

### Settings
Settings are stored in SharedPreferences (`pixelquest_settings`) under fixed key names. Renaming a key resets that setting for every player on update. `ReleasedSettingsKeysTest` reads the released names. Add new keys; don't rename old ones.
