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
