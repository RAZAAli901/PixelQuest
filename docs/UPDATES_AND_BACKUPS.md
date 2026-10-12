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

### Upgrading from 1.1.0 or earlier
Android won't install today's release over 1.1.0, because the older releases were signed with a temporary key (RELEASE_SIGNING.md). So:
1. In the old app: Settings → **EXPORT QUEST DATA**, and save the file.
2. Uninstall the old app and install the new one.
3. Settings → **IMPORT QUEST DATA** and pick the file. The dialog says what it holds before you confirm.

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

A backup made by a newer version restores what this version knows, and Settings says the rest was left out.

## For developers

### Changing a database table
The layouts players have are in `app/schemas` (one JSON per version; the README there says where the older ones came from).
1. Bump `version` in `AppDatabase` and write a `Migration(old, new)`.
2. Add it to `AppDatabase.ALL_MIGRATIONS`, the one list the app and the tests use.
3. Build. Room writes the new `N.json`. Commit it.
4. Run `DatabaseUpgradeTest`. It builds every earlier version's database with a played game in it, then opens it the way the app does, so Room checks the result.

Never change a shipped version's layout without a new version. The build would rewrite that version's file, and CI's **Database layouts match the code** step fails. On a phone, Room would refuse to open the database at every start.

There's no wipe-and-start-over for updates any more (`fallbackToDestructiveMigrationOnDowngrade` only). A missing migration fails loudly at start, with the data intact, instead of quietly deleting it.

### Changing the backup format
- Bump `DataExportImport.FORMAT_VERSION`, add the change to the history in its comment, and keep reading older files.
- Read new fields with defaults, as older files won't have them.
- Add a test with an old file as the old app wrote it. `OldBackupRestoreTest` has v1.0.0's and v1.1.0's.

### Trying an update on a device
Unit tests check the database and backups. Before a release, also try a real update, which covers what they can't: the start-up path, onboarding, alarms. `scripts/update-check.sh <from> [to]` builds the two versions as a separate app (`com.pixelquest.app.updatecheck`), so the installed PixelQuest and its data aren't touched. The script prints the install and update commands.

On Day 34, `scripts/update-check.sh v1.1.0` found a real bug. The database came through, but every 1.1.0 player was sent through onboarding, and finishing it reset their level and XP (fixed: `OnboardingGate`). After the fix the update keeps everything (VERIFICATION.md, Section M).

### Settings
Settings are stored in SharedPreferences (`pixelquest_settings`) under fixed key names. Renaming a key resets that setting for every player on update. `ReleasedSettingsKeysTest` reads the released names. Add new keys; don't rename old ones.
