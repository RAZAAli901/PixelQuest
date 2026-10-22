# Code map (features touched by this plan)

App code is under `app/src/main/java/com/pixelquest/app/`.

| Area | Where |
|---|---|
| Study reminders: domain | `study/domain/StudyReminder.kt`, `ReminderTime.kt` (occurrences), `ReminderAlerts.kt` (alert times) |
| Reminders: data | `study/data/StudyReminderRepository.kt`, `study/data/local/StudyReminder*`, `study/data/remote/StudyReminder*` |
| Reminders: alarms | `study/alarm/` (scheduler, receiver, actions, notifier, mutes, ledger, alarm sync), `scheduling/AlarmArming.kt`, `scheduling/BootReceiver.kt` |
| Reminders: UI | `study/ui/ReminderForm.kt`, `ReminderSheet.kt`, `ReminderBanner.kt`, `StudyRemindersViewModel.kt` |
| Reminders: sync | `study/sync/StudyReminderSyncWorker.kt` |
| Shared chat UI | `ui/chat/` (ChatScaffold, ChatTopBar, ChatTimeline, ChatLook, Bubbles, DaySeparator, MemberColors) |
| Study chat: domain | `study/chat/domain/ChatModels.kt` (StudyChatConfig), `UploadChecks.kt` |
| Study chat: data | `study/chat/data/` (StudyChatRepository, ChatUploads, ChatFileOpener, PickedFiles, FileWarningSeen), `data/local/Chat*`, `data/remote/` (ChatDtos, StudyChatRemoteDataSource, ChatFilesRemote) |
| Study chat: sync | `study/chat/sync/` (ChatLiveSource, StudyChatSendWorker, StudyChatUploadWorker, StudyChatNotifyWorker) |
| Study chat: UI | `study/chat/ui/` (StudyChatViewModel, StudyChatPane, MessageBubble, Replies, MessageMenu, ChatInputBar, Attachments, UploadBubble, FileCard, FilesTab, OpenInViewer) |
| Study party screen (tabs) | `study/ui/StudyPartyScreen.kt` (wires the reminders and chat view models, passed from `party/ui/PartiesNavigation.kt`) |
| Item screen (materials) | `study/ui/StudyItemScreen.kt` |
| Room DB | `data/local/AppDatabase.kt` (version 23), `app/schemas/`, `di/DaoModule.kt`, `di/RepositoryModule.kt` |
| Notification channels | `notification/NotificationChannels.kt` (STUDY_REMINDERS, STUDY_CHAT) |
| Server SQL | `supabase/migrations/2026101900004[5-9]*`, `...0005*`, `...0006[0-5]*` |
| Edge Functions | `supabase/functions/{request-upload,finalize-upload,get-download-url,cleanup-uploads,_shared}` |
| Server tests | `supabase/tests/local/study_chat.test.ts`, `study_files.test.ts`, `darood_parties.test.ts` (reminders, privacy) |
| Darood weekly | `darood/domain/WeekResults.kt`, `Goals.kt`; `darood/sync/` (WeekResultsRun, DaroodWeekResultsWorker, AndroidWeekResultNotifier, WeekRankingMutes, GoalNotifyRun); `darood/ui/weekly/`; `darood/ui/chat/ExtendGoalSheet.kt` |
| Chess: domain and rules | `chess/domain/` (ChessModels, ChessConfig, ChessClock with ClockSync, Fen), `chess/rules/ChessRules.kt` (hints only) |
| Chess: data | `chess/data/` (ChessRepositories, ChessRepositoryImpl, ChessSettings), `chess/data/remote/` (ChessDtos, ChessRemote), `chess/data/local/ChessCache.kt`, `chess/data/fake/FakeChessBackend.kt` (scripted games for tests and previews) |
| Chess: live | `chess/sync/ChessLiveSource.kt` (row changes plus presence on the private channel) |
| Chess: UI | `chess/ui/` (ChessGameScreen, ChessSessionViewModel, PlayableBoard, ChessBoard, ChessClocks, MoveList, GameActions, GameOverCard, ChessTab, ChessPartyViewModel, ChallengeSheet, IncomingChallengeScreen, ChessChatCards, LiveGameCard, HeadToHeadScreen, ChessBadges, ChessSettingsSection, ChessFeedback, ChessResume, ChessSpeech) |
| Push | `push/` (PushRegistration, PushService, ChessPushNotifier); `supabase/functions/send-push`, `_shared/push.ts` |
| Cloud export | `auth/AuthRepository.kt` (exportMyData), `ui/screens/account/` (Export my cloud data) |
| Chess server | migrations `...00075`–`...00108`; `supabase/functions/chess-*`, `_shared/chess*.ts` |
| Monitoring | `supabase/functions/_shared/log.ts`, `_monitor/function_errors.ts`, `.github/workflows/monitor.yml` |
| Chess tests | `app/src/test/.../chess/`, `integration/LocalSupabaseChessTest.kt`; `supabase/tests/local/chess_*.test.ts`, `security`, `abuse`, `privacy`, `load` |
| Docs | `docs/study-reminders.md`, `docs/study-chat.md`, `docs/darood-weekly.md`, `docs/chess-design.md`, `docs/chess-ui-integration.md`, `docs/ABUSE_REVIEW.md`, `docs/PRIVACY_REVIEW.md`, `docs/MONITORING.md`, `docs/QA_CHECKLIST.md`, `supabase/schema.md` |
