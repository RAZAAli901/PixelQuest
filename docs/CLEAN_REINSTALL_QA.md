# PixelQuest Clean Uninstall & Reinstall Pass

What happens to a player's game when PixelQuest is uninstalled and installed again.

## What an uninstall removes

1. **The game**: the Room database, `pixelquest.db` with its `-wal` and `-shm` files.
2. **Settings**: the SharedPreferences, including whether the welcome steps were finished.
3. **The copies kept on the phone**: the automatic backups, the undo copy and the copy kept before an update (PRIVACY.md).

An exported backup file (in Downloads, say) stays. It's the player's file, not the app's.

## What a reinstall shows

- **With Android's own backup off**, or nothing backed up yet, the app opens on the welcome screen with **START YOUR JOURNEY** and **I HAVE A BACKUP**. No automatic backup is offered, because those went with the uninstall.
- **With Android's own backup on**, Android may put the game and settings back at install (docs/UPDATES_AND_BACKUPS.md), and the app opens on the dashboard.

## Day 34 pass

Emulator (API 34), debug build installed as a separate app (`com.pixelquest.app.updatecheck`), so the installed PixelQuest wasn't touched.

1. **Fresh install:** welcome screen with START YOUR JOURNEY and I HAVE A BACKUP only. Before Step 29 it also offered "RESTORE MY GAME FROM 12 OCT", a backup of the empty game.
2. **Played and exported:** onboarded, completed a quest (50 XP), then EXPORT QUEST DATA saved `pixelquest-backup-2026-10-12.json`. Settings said "Backup saved." and "Last backup: 12 Oct 2026".
3. **Uninstalled and reinstalled:** welcome screen. I HAVE A BACKUP, then the file. The dialog said "This backup has Aria, level 1 and 3 quests, with settings. Saved by PixelQuest 1.1.0." (versionName is still 1.1.0).
4. **Restored:** the dashboard with 50 XP and the completed quest, without the welcome steps.

Android's own backup wasn't tried: Backup Manager is off on the emulator.
