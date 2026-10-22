# What the phone stores, and what leaves it

S031, as of 1.7.2. Everything below is in the app's private storage (`/data/data/com.pixelquest.app`). Other apps can't read it, and Android encrypts it at rest (file-based encryption, Android 7 and later, unlocked with the screen lock).

## Stores

| Store | Holds | Sensitive? |
|---|---|---|
| `databases/pixelquest.db` (Room) | The player's quests, logs, levels, Namaz log and settings. Caches of their parties: Darood entries, study items, chat messages and file names, chess games, other members' usernames | Personal; the caches include **other people's** messages and names |
| Supabase session: the library's default preferences (`<package>_preferences.xml`) | The access and refresh tokens | **Secret**: the account itself |
| `pixelquest_settings.xml` | Settings (skin, sounds, reminders, the onboarding flag) | No |
| `pixelquest_level_up.xml` | A pending level-up celebration | No |
| `pixelquest_cloud_account.xml` | The last signed-in account's id | An id, not a secret |
| `pixelquest_ai_usage`, `pixelquest_chess`, `pixelquest_darood`, `pixelquest_darood_goals`, `pixelquest_study`, `pixelquest_study_chat`, `pixelquest_tips` | Per-device bookkeeping: AI counts and cached lines, notification ledgers, mutes, hints shown | No (the AI cache is the player's own coaching text) |
| `files/auto-backups/` | Daily automatic backups (JSON): quests, logs, levels, Namaz, settings. No tokens, nobody else's data | Personal (the player's own) |
| `files/before-restore.json`, `files/pre-update/` | A copy kept before a restore or an update, to undo it | Personal (the player's own) |
| `files/pixelquest_crash_log.txt` | The last crash's stack trace | May name screens; no user data by design (S055) |
| `cache/study-files/<id>/` | Chat files the player opened, handed to a viewer app through the FileProvider | Other people's documents |
| DataStore (`quran_*`) | The Quran's reading settings and last place | No |

## What Android backup copied before the S032 fix

`android:allowBackup` is on, with include-only rules (Day 34): `backup_rules.xml` (Android 11 and lower) and `data_extraction_rules.xml` (12 and up).

- **Copied:** the database, `pixelquest_settings`, `pixelquest_level_up` and `pixelquest_cloud_account`.
- **Never copied:** the session tokens, the AI data, the per-device bookkeeping, the caches, the crash log.

The gap was the database. It holds the party caches, so other members' chat messages and names went to Google Drive with the player's backup. S032 closes it.

## Sign-out and deletion

- **Sign-out** clears the party caches (Darood, study, chat, chess) and the push token (`DaroodAccountCleanup`, `PushRegistration`).
- **Opened chat files** in `cache/study-files` stay until Android clears the cache. S036 deletes them on sign-out and account deletion.
