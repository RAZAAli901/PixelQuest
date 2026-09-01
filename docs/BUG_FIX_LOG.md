# PixelQuest Final Regression Bug Fix Log

This document records the resolved edge-case issues identified during the final Day 12 QA pass.

## Fixed Issues

1. **JSON Parser Malformed Types**: Wrapped `DataExportImport.importFromJson` date and enum parsing calls with fallback defaults to handle partial or corrupted JSON import payloads.
2. **Room DB Disk Full Guard**: Introduced `safeDatabaseCall` wrapper for `insertTask`, `updateTask`, and `deleteTask` to log database IO exceptions gracefully without hard crashing.
3. **Exact Alarm Security Exceptions**: Added `SecurityException` try/catch inside `TaskAlarmScheduler` when runtime exact alarm permissions are revoked on API 31+.
4. **Stats Query Inverted Date Ranges**: Added `startDate.isAfter(endDate)` guards returning empty flows inside `StatsRepositoryImpl`.
