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
| Room DB | `data/local/AppDatabase.kt` (version 20), `app/schemas/`, `di/DaoModule.kt`, `di/RepositoryModule.kt` |
| Notification channels | `notification/NotificationChannels.kt` (STUDY_REMINDERS, STUDY_CHAT) |
| Server SQL | `supabase/migrations/2026101900004[5-9]*`, `...0005*`, `...0006[0-5]*` |
| Edge Functions | `supabase/functions/{request-upload,finalize-upload,get-download-url,cleanup-uploads,_shared}` |
| Server tests | `supabase/tests/local/study_chat.test.ts`, `study_files.test.ts`, `darood_parties.test.ts` (reminders, privacy) |
| Docs | `docs/study-reminders.md`, `docs/study-chat.md`, `supabase/schema.md` |
