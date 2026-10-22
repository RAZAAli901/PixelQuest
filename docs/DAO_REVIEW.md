# DAO Architecture & Thread Safety Review (Q036)

## Summary
A comprehensive audit of all Room Data Access Objects (DAOs) was performed across all modules to guarantee asynchronous execution, proper transaction boundaries, and optimal table indexing.

## Audit Matrix

| DAO Interface | Entity Tables Covered | Read Dispatch | Write Dispatch | `@Transaction` Methods | Index Coverage |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `TaskDao` | `tasks` | `Flow<T>`, `suspend` | `suspend` | N/A (atomic per statement) | `id` (PK), `scheduledDay`, `isActive` |
| `TaskCompletionLogDao` | `task_completion_logs` | `Flow<T>`, `suspend` | `suspend` | N/A | `[taskId, completedDate]` (unique) |
| `UserProfileDao` | `user_profile` | `Flow<T>` | `suspend` | Atomic updates | `id` (PK) |
| `DifficultySettingsDao`| `difficulty_settings` | `Flow<T>` | `suspend` | Single row | `id` (PK) |
| `LevelHistoryDao` | `level_history` | `Flow<T>` | `suspend` | Single table | `achievedDate` (ordered) |
| `StreakDao` | `streaks` | `Flow<T>` | `suspend` | Single row | `id` (PK) |
| `InsightCacheDao` | `insight_cache` | `Flow<T>`, `suspend` | `suspend` | Single table | `generatedAt` (ordered) |
| `NamazLogDao` | `namaz_log` | `Flow<T>`, `suspend` | `suspend` | Upsert handling | `[date, prayer]` (unique) |
| `NamazSettingsDao` | `namaz_settings` | `Flow<T>`, `suspend` | `suspend` | Single row | `id` (PK) |
| `PartyDao` | `darood_party`, `darood_party_member` | `Flow<T>`, `suspend` | `suspend` | `replaceParties`, `replaceMembers`, `replaceMembersIfKept` | `id` (PK), `partyId`, `[partyId, role, total]` |
| `DaroodDao` | `darood_entry`, `darood_pending_entry`, `darood_week_result` | `Flow<T>`, `suspend` | `suspend` | `upsertEntriesIfKept`, `confirmSent`, `upsertWeekResultsIfKept` | `partyId`, `clientId`, `[partyId, rowid]` |
| `StudyDao` | `study_item`, `study_completion`, `study_session`, `study_week_score`, `study_week_winner`, `study_pending` | `Flow<T>`, `suspend` | `suspend` | `removeItem`, `replaceItems`, `replaceCompletions`, `replaceSessions`, `replaceScores`, `replaceWinners`, `confirmCompletionSent`, `confirmSessionSent` | `partyId`, `itemId`, `dueAt`, `weekStart`, `clientId` |
| `StudyReminderDao` | `study_reminder`, `study_reminder_pending`, `study_reminder_done` | `Flow<T>`, `suspend` | `suspend` | `replaceReminders`, `replacePendingFor`, `confirmSent` | `partyId`, `weekStart`, `dueAt`, `reminderId` |
| `ChatDao` | `chat_message`, `chat_message_pending`, `chat_file`, `chat_upload`, `chat_read` | `Flow<T>`, `suspend` | `suspend` | `upsertMessagesIfKept`, `insertPendingIfKept`, `confirmSent`, `upsertFilesIfKept`, `insertUploadIfKept`, `markRead` | `partyId`, `createdAt`, `clientId`, `state` |
| `QuranDao` | `quran_surah`, `quran_ayah`, `quran_juz`, `quran_translation`, `quran_ayah_fts` | `Flow<T>`, `suspend` | `suspend` | Managed by `RoomTransactionRunner` in importer | `number`, `surah`, `ayah`, FTS virtual index |
| `QuranBookmarkDao` | `quran_bookmark` | `Flow<T>`, `suspend` | `suspend` | N/A | `[surah, ayah]` (PK) |
| `ChessDao` | `chess_rating`, `chess_head_to_head`, `chess_finished_game` | `Flow<T>`, `suspend` | `suspend` | `replaceRatings`, `keepFinishedGame`, `clearAll` | `[userId, timeControl]`, `[me, them, timeControl]`, `partyId` |

## Enforcement & Guarantees
1. **Zero Main Thread Dispatches**: All queries either return cold `Flow` (dispatched on Room's background query executor) or are `suspend` functions with mandatory coroutine context.
2. **Atomic Invariants**: Compound data operations that replace parent/child entities (such as party syncs, item removals, or chat read pointers) use `@Transaction` to eliminate partial state reads.
