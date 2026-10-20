# PixelQuest Release Build QA & Regression Script

This document tracks the manual regression test suite executed against the minified **Release Build** (`app-release.apk`) to verify R8 minification and resource shrinking did not break reflection, database mapping, or UI rendering.

## Test Cases & Compliance Matrix

| # | Test Case | Expected Behavior | Minified Release Pass |
|---|-----------|-------------------|----------------------|
| 1 | Cold Start & Splash | App launches to splash in < 1.5s and auto-navigates to Onboarding/Today | PASSED |
| 2 | Onboarding Flow | Name entry, avatar selection, difficulty selection, summary, and transition to Today | PASSED |
| 3 | Task CRUD Operations | Create daily quest, edit title, mark complete, delete quest | PASSED |
| 4 | Room Database Persistence | App restart retains profile level, tasks, streak count, and log history | PASSED |
| 5 | Audio & SFX System | SoundPool plays click, completion, missed, and fanfare sounds | PASSED |
| 6 | Settings & Toggles | Sound, CRT filter, haptics, and notification toggles persist | PASSED |
| 7 | Data Backup Export/Import | JSON backup payload exports and imports cleanly without R8 obfuscation errors | PASSED |
| 8 | Alarm & WorkManager | AlarmManager exact alarms and WorkManager periodic workers execute | PASSED |
| 9 | Update from the last release | `scripts/update-check.sh <last tag>`, then play and update: quests, results, XP and level kept, no welcome steps (docs/UPDATES_AND_BACKUPS.md) | Not yet run on a release build. Passed on a debug build from v1.1.0 (Day 34) |
| 10 | Restore on the welcome screen | After a reinstall, I HAVE A BACKUP restores an exported file once its summary is confirmed | Not yet run on a release build. Passed on a debug build (Day 34) |
| 11 | Lost database | With `pixelquest.db`, `-wal` and `-shm` deleted, the next start asks RESTORE YOUR GAME? with the newest automatic backup | Debug builds only (`run-as` needs a debuggable app). Passed (Day 34) |
| 12 | Android 7 | On the `PixelQuest_API24` emulator (port 5556): the release build opens, onboarding finishes, and the dashboard, Namaz Tracker, Darood Party, Quest History and Stats open | PASSED (1.4.1, VERIFICATION.md Section P). 1.4.0 and earlier closed at launch |

Rows 9 to 11 were added on Day 34 (VERIFICATION.md, Section M). Run 9 and 10 on the minified build before the next release: R8 could break what a debug build doesn't show.
