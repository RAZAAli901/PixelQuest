# Backup and Export Format Specification

PixelQuest backup and export format specification and compatibility rules (`Q044`).

## Version History

| Format Version | Introduced | Added Features | Backward Compatibility |
|----------------|------------|----------------|------------------------|
| **0** (Legacy) | v1.0.x - v1.1.0 | Unnumbered root payload with `userProfile`, `streak`, `tasks`, `difficultySettings` | Fully supported. Missing fields receive schema defaults (e.g. `isActive = true`, `reminderStyle = STANDARD`). |
| **3** | Day 34 | Added `formatVersion` and `appVersion` metadata headers | Reads format 0 and 3. |
| **4** | Day 34 | Added `settings` object (`BackupSettings` containing sounds, haptics, theme, simpleMode) | Reads formats 0, 3, and 4. Null settings retain current phone preferences. |
| **5** | Current | Added `namazLogs` and `namazSettings` for Namaz Tracker | Reads all previous versions. Missing Namaz data preserves local state on restore. |

## Format Payload Schema (v5)

```json
{
  "formatVersion": 5,
  "appVersion": "1.7.2",
  "userProfile": {
    "id": 1,
    "username": "Hero",
    "avatarId": "avatar_hero",
    "level": 5,
    "totalXp": 2500,
    "perfectDaysTowardNextLevel": 3
  },
  "difficultySettings": {
    "id": 1,
    "difficultyLevel": "MEDIUM",
    "perfectDayThreshold": 0.7,
    "daysRequiredPerLevel": 7
  },
  "streak": {
    "id": 1,
    "currentStreak": 5,
    "longestStreak": 12,
    "lastCompletedDate": "2026-10-23"
  },
  "tasks": [
    {
      "id": 1,
      "name": "Daily Quest",
      "description": "Complete daily habits",
      "scheduledTime": "08:00",
      "scheduledDay": "2026-10-23",
      "recurrenceType": "DAILY",
      "category": "FITNESS",
      "isActive": true,
      "createdAt": 1729641600000,
      "reminderEnabled": true,
      "reminderLeadMinutes": 0,
      "reminderStyle": "STANDARD",
      "weeklyDays": [1, 3, 5]
    }
  ],
  "logs": [
    {
      "id": 1,
      "taskId": 1,
      "completedDate": "2026-10-22",
      "wasCompleted": true,
      "pointsAwarded": 50
    }
  ],
  "levelHistory": [
    {
      "id": 1,
      "level": 2,
      "achievedDate": 1729000000000,
      "difficultyAtTimeOfLevelUp": "MEDIUM"
    }
  ],
  "settings": {
    "sound": true,
    "crtEffect": false,
    "haptics": true,
    "reduceMotion": false,
    "notifications": true,
    "notificationSound": true,
    "notificationVibration": true,
    "theme": "COMIC",
    "simpleMode": false
  },
  "namazLogs": [],
  "namazSettings": null
}
```

## Compatibility Guarantees

1. **Forward Compatibility**: When importing a backup with `formatVersion > CURRENT_FORMAT_VERSION`, known fields are parsed and restored while unknown fields are ignored without crash. The user is notified via `BackupMessages.RESTORED_FROM_NEWER`.
2. **Backward Compatibility**: Any backup from version 0 through 5 is deterministically parsed. Missing fields fall back to sensible defaults.
3. **Safety & Security**: Sensitive tokens, cloud sessions, and external party caches are strictly excluded from backups.
