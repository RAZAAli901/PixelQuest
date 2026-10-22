# Caching and Offline Strategy per Repository (Q038)

## 1. Core Architecture Principles

PixelQuest adheres to an **Offline-First, Single Source of Truth (SSOT)** design:
1. **Local Room Database First**: ViewModels observe Room `Flow` queries. The UI immediately renders local state upon app launch without waiting for network responses or showing blocking spinners.
2. **Pending Queue Pattern**: When mutations occur while disconnected or during transient failures, changes are appended to local `_pending` tables with unique UUID client IDs and retry counters.
3. **Idempotent Background Synchronization**: WorkManager workers drain pending queues, post them to the remote Supabase API, and in a single `@Transaction` replace the local staging entry with the authoritative server record.
4. **Data Isolation & Clean Cleanup**: If a party or account is deleted/left, all associated cached records cascade or are purged atomically.

---

## 2. Strategy Matrix by Domain & Repository

### A. Core Task & Progression Domain
| Repository | Strategy | Local Storage | Remote Source | Offline Behavior | Conflict Resolution |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `TaskRepository` | Offline-First (Local Primary) | `tasks` | None (Local SSOT) | 100% operational offline | N/A (Device authoritative) |
| `TaskCompletionRepository` | Offline-First (Local Primary) | `task_completion_logs` | None (Local SSOT) | 100% operational offline | Unique `(taskId, completedDate)` constraint prevents duplicates |
| `StreakRepository` | Offline-First (Local Primary) | `streaks` | None (Local SSOT) | 100% operational offline | Device authoritative |
| `UserProfileRepository` | Offline-First (Local Primary) | `user_profile` | None (Local SSOT) | 100% operational offline | Device authoritative |
| `LevelHistoryRepository` | Offline-First (Local Primary) | `level_history` | None (Local SSOT) | 100% operational offline | Insert-only audit trail |
| `DifficultySettingsRepository` | Offline-First (Local Primary) | `difficulty_settings` | None (Local SSOT) | 100% operational offline | Device authoritative |
| `CloudProfileRepository` | Sync-on-Demand / Periodic | `user_profile` | Supabase `profiles` table | Queued; synced when online | Server compares timestamps; higher XP / streak wins |

### B. Islamic Hub & Quran Domain
| Repository | Strategy | Local Storage | Remote Source | Offline Behavior | Conflict Resolution |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `NamazRepository` | Offline-First | `namaz_log`, `namaz_settings` | None (Offline calculations via Adhan library) | 100% operational offline; prayer times calculated astronomically on device | Unique `(date, prayer)` constraint |
| `QuranRepository` | Bundled Asset Seed + FTS | `quran_surah`, `quran_ayah`, `quran_ayah_fts`, `quran_juz` | Local pre-indexed assets; Tanzil translations | 100% operational offline; instant full-text search via SQLite FTS4 | Read-only import transaction |
| `BookmarkRepository` | Offline-First | `quran_bookmark` | None (Device authoritative) | 100% operational offline | Unique `(surah, ayah)` composite key |

### C. Social & Study Party Domain
| Repository | Strategy | Local Storage | Remote Source | Offline Behavior | Conflict Resolution |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `PartyRepository` | Cache-First + Remote Refresh | `darood_party`, `darood_party_member` | Supabase `parties`, `party_members` | Displays cached parties and members; refresh triggers on screen open | Server authoritative; cached partition replaced in `@Transaction` |
| `DaroodRepository` | Cache-First + Pending Buffer | `darood_entry`, `darood_pending_entry` | Supabase `darood_entries` | Counts logged immediately to `darood_pending_entry` and displayed in pending totals | Pending entries synced sequentially by `rowid`; server assigns final ID |
| `StudyRepository` | Cache-First + Pending Buffer | `study_item`, `study_completion`, `study_session`, `study_pending` | Supabase `study_*` tables | Items viewable; completions & study sessions stored in `study_pending` | Server response replaces pending row in `@Transaction` |
| `StudyReminderRepository` | Cache-First + Local Alarms | `study_reminder`, `study_reminder_pending`, `study_reminder_done` | Supabase `study_reminders` | Existing alarms fire on device; new reminders staged in pending table | Last-write-wins by reminder ID |
| `StudyChatRepository` | Cache-First + Outbox Queue | `chat_message`, `chat_message_pending`, `chat_file`, `chat_upload` | Supabase `chat_messages`, Storage bucket | Messages visible offline; outbound messages shown as `PENDING` | Monotonic `createdAt` + server timestamp ordering |

### D. Gaming & AI Domains
| Repository | Strategy | Local Storage | Remote Source | Offline Behavior | Conflict Resolution |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `ChessRepository` | Network-First (Real-time) + Cache | `chess_rating`, `chess_finished_game`, `chess_head_to_head` | Supabase Realtime Channels / WebSocket | Active matches require internet; previous ratings and match histories cached | Server validates every move legality |
| `HabitInsightRepository` | Cache-First + TTL (24h) | `insight_cache` | Gemini AI Proxy | Shows latest cached habit report; network call skipped if data hash matches | New insight replaces oldest once generation completes |
| `LeaderboardRepository` | Network-First with Safe Fallback | In-memory StateFlow | Supabase `leaderboard_view` | Displays cached list or safe offline banner (`SafeErrorCopy.BOARD_OFFLINE`) | Read-only view computed on Supabase |

---

## 3. Worker Sync Trigger Matrix

| Worker | Trigger Rule | Network Constraint | Retry Policy |
| :--- | :--- | :--- | :--- |
| `DaroodSyncWorker` | Scheduled periodic + Network connected broadcast | `NetworkType.CONNECTED` | Exponential backoff (initial 30s) |
| `StudySyncWorker` | App foreground + Connectivity recovery | `NetworkType.CONNECTED` | Exponential backoff (initial 30s) |
| `StudyChatUploaderWorker` | Enqueued on document selection | `NetworkType.CONNECTED` | Exponential backoff with file chunk resumption |
| `ConnectivitySyncObserver` | System connectivity callbacks | `NetworkType.CONNECTED` | Debounced batch trigger |
