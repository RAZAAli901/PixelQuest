package com.pixelquest.app.di

import com.pixelquest.app.data.repository.DifficultySettingsRepositoryImpl
import com.pixelquest.app.data.repository.StreakRepositoryImpl
import com.pixelquest.app.data.repository.TaskCompletionRepositoryImpl
import com.pixelquest.app.data.repository.TaskRepositoryImpl
import com.pixelquest.app.data.repository.UserProfileRepositoryImpl
import com.pixelquest.app.domain.repository.DifficultySettingsRepository
import com.pixelquest.app.domain.repository.StreakRepository
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import com.pixelquest.app.domain.repository.TaskRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

import com.pixelquest.app.data.repository.LevelHistoryRepositoryImpl
import com.pixelquest.app.domain.repository.LevelHistoryRepository

import com.pixelquest.app.data.repository.SettingsRepositoryImpl
import com.pixelquest.app.domain.repository.SettingsRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTaskRepository(
        impl: TaskRepositoryImpl
    ): TaskRepository

    @Binds
    @Singleton
    abstract fun bindStreakRepository(
        impl: StreakRepositoryImpl
    ): StreakRepository

    @Binds
    @Singleton
    abstract fun bindUserProfileRepository(
        impl: UserProfileRepositoryImpl
    ): UserProfileRepository

    @Binds
    @Singleton
    abstract fun bindDifficultySettingsRepository(
        impl: DifficultySettingsRepositoryImpl
    ): DifficultySettingsRepository

    @Binds
    @Singleton
    abstract fun bindTaskCompletionRepository(
        impl: TaskCompletionRepositoryImpl
    ): TaskCompletionRepository

    @Binds
    @Singleton
    abstract fun bindLevelHistoryRepository(
        impl: LevelHistoryRepositoryImpl
    ): LevelHistoryRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindStatsRepository(
        impl: com.pixelquest.app.data.repository.StatsRepositoryImpl
    ): com.pixelquest.app.domain.repository.StatsRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: com.pixelquest.app.auth.AuthRepositoryImpl
    ): com.pixelquest.app.auth.AuthRepository

    @Binds
    @Singleton
    abstract fun bindCloudProfileRepository(
        impl: com.pixelquest.app.data.repository.CloudProfileRepositoryImpl
    ): com.pixelquest.app.data.repository.CloudProfileRepository

    @Binds
    @Singleton
    abstract fun bindSyncScheduler(
        impl: com.pixelquest.app.worker.SyncSchedulerImpl
    ): com.pixelquest.app.worker.SyncScheduler

    @Binds
    @Singleton
    abstract fun bindLeaderboardRepository(
        impl: com.pixelquest.app.data.repository.LeaderboardRepositoryImpl
    ): com.pixelquest.app.data.repository.LeaderboardRepository

    @Binds
    @Singleton
    abstract fun bindInsightCacheRepository(
        impl: com.pixelquest.app.data.repository.InsightCacheRepositoryImpl
    ): com.pixelquest.app.domain.repository.InsightCacheRepository

    @Binds
    @Singleton
    abstract fun bindNamazRepository(
        impl: com.pixelquest.app.data.repository.NamazRepositoryImpl
    ): com.pixelquest.app.domain.repository.NamazRepository

    @Binds
    @Singleton
    abstract fun bindPrayerTimeProvider(
        impl: com.pixelquest.app.domain.namaz.DefaultPrayerTimeProvider
    ): com.pixelquest.app.domain.namaz.PrayerTimeProvider

    @Binds
    @Singleton
    abstract fun bindLocationProvider(
        impl: com.pixelquest.app.namaz.AndroidLocationProvider
    ): com.pixelquest.app.namaz.LocationProvider

    @Binds
    @Singleton
    abstract fun bindNamazNotifier(
        impl: com.pixelquest.app.namaz.AndroidNamazNotifier
    ): com.pixelquest.app.namaz.NamazNotifier

    @Binds
    @Singleton
    abstract fun bindNamazAlarms(
        impl: com.pixelquest.app.namaz.NamazAlarmScheduler
    ): com.pixelquest.app.namaz.NamazAlarms

    @Binds
    @Singleton
    abstract fun bindNamazReminderOptionsStore(
        impl: com.pixelquest.app.namaz.PrefsNamazReminderOptionsStore
    ): com.pixelquest.app.domain.namaz.NamazReminderOptionsStore

    @Binds
    @Singleton
    abstract fun bindNamazDeliveries(
        impl: com.pixelquest.app.namaz.PrefsNamazDeliveries
    ): com.pixelquest.app.namaz.NamazDeliveries

    @Binds
    @Singleton
    abstract fun bindUsernameRepository(
        impl: com.pixelquest.app.darood.data.SupabaseUsernameRepository
    ): com.pixelquest.app.darood.data.UsernameRepository

    @Binds
    @Singleton
    abstract fun bindUsernamePromptStore(
        impl: com.pixelquest.app.darood.data.PrefsUsernamePromptStore
    ): com.pixelquest.app.darood.data.UsernamePromptStore

    @Binds
    @Singleton
    abstract fun bindMilestoneSeenStore(
        impl: com.pixelquest.app.darood.data.PrefsMilestoneSeenStore
    ): com.pixelquest.app.darood.data.MilestoneSeenStore

    @Binds
    @Singleton
    abstract fun bindDaroodReminderSettings(
        impl: com.pixelquest.app.darood.data.PrefsDaroodReminderSettings
    ): com.pixelquest.app.darood.data.DaroodReminderSettings

    @Binds
    @Singleton
    abstract fun bindUsernameCache(
        impl: com.pixelquest.app.darood.data.PrefsUsernameCache
    ): com.pixelquest.app.darood.data.UsernameCache

    // Darood Party (docs/darood-design.md): its server, its account and its repository.

    @Binds
    @Singleton
    abstract fun bindDaroodAccount(
        impl: com.pixelquest.app.darood.data.remote.SupabaseDaroodAccount
    ): com.pixelquest.app.darood.data.remote.DaroodAccount

    @Binds
    @Singleton
    abstract fun bindPartyRemoteDataSource(
        impl: com.pixelquest.app.darood.data.remote.SupabaseDaroodPartyRemoteDataSource
    ): com.pixelquest.app.party.data.remote.PartyRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindPartyRepository(
        impl: com.pixelquest.app.party.data.PartyRepositoryImpl
    ): com.pixelquest.app.party.data.PartyRepository

    @Binds
    @Singleton
    abstract fun bindDaroodPartyRemoteDataSource(
        impl: com.pixelquest.app.darood.data.remote.SupabaseDaroodPartyRemoteDataSource
    ): com.pixelquest.app.darood.data.remote.DaroodPartyRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindDaroodPartyRepository(
        impl: com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
    ): com.pixelquest.app.darood.data.DaroodPartyRepository

    @Binds
    @Singleton
    abstract fun bindDaroodSyncScheduler(
        impl: com.pixelquest.app.darood.sync.WorkManagerDaroodSyncScheduler
    ): com.pixelquest.app.darood.sync.DaroodSyncScheduler

    @Binds
    @Singleton
    abstract fun bindPartyLiveSource(
        impl: com.pixelquest.app.party.sync.SupabasePartyLiveSource
    ): com.pixelquest.app.party.sync.PartyLiveSource

    @Binds
    @Singleton
    abstract fun bindDaroodLiveSource(
        impl: com.pixelquest.app.darood.sync.SupabaseDaroodLiveSource
    ): com.pixelquest.app.darood.sync.DaroodLiveSource

    @Binds
    @Singleton
    abstract fun bindDaroodCacheOwner(
        impl: com.pixelquest.app.darood.sync.PrefsDaroodCacheOwner
    ): com.pixelquest.app.darood.sync.DaroodCacheOwner

    // Study parties (docs/study-party.md): their own server calls; members go through the party core.

    @Binds
    @Singleton
    abstract fun bindStudyRemoteDataSource(
        impl: com.pixelquest.app.study.data.remote.SupabaseStudyRemoteDataSource
    ): com.pixelquest.app.study.data.remote.StudyRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindStudyReminderRemoteDataSource(
        impl: com.pixelquest.app.study.data.remote.SupabaseStudyReminderRemoteDataSource
    ): com.pixelquest.app.study.data.remote.StudyReminderRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindStudyReminderRepository(
        impl: com.pixelquest.app.study.data.StudyReminderRepositoryImpl
    ): com.pixelquest.app.study.data.StudyReminderRepository

    @Binds
    @Singleton
    abstract fun bindReminderSendScheduler(
        impl: com.pixelquest.app.study.sync.WorkManagerReminderSendScheduler
    ): com.pixelquest.app.study.sync.ReminderSendScheduler

    @Binds
    @Singleton
    abstract fun bindStudyReminderMutes(
        impl: com.pixelquest.app.study.alarm.PrefsStudyReminderMutes
    ): com.pixelquest.app.study.alarm.StudyReminderMutes

    @Binds
    @Singleton
    abstract fun bindStudyReminderAlarms(
        impl: com.pixelquest.app.study.alarm.AlarmManagerStudyReminderAlarms
    ): com.pixelquest.app.study.alarm.StudyReminderAlarms

    @Binds
    @Singleton
    abstract fun bindStudyReminderLedger(
        impl: com.pixelquest.app.study.alarm.PrefsStudyReminderLedger
    ): com.pixelquest.app.study.alarm.StudyReminderLedger

    @Binds
    @Singleton
    abstract fun bindStudyReminderNotifier(
        impl: com.pixelquest.app.study.alarm.AndroidStudyReminderNotifier
    ): com.pixelquest.app.study.alarm.StudyReminderNotifier

    @Binds
    @Singleton
    abstract fun bindStudyChatRemoteDataSource(
        impl: com.pixelquest.app.study.chat.data.remote.SupabaseStudyChatRemoteDataSource
    ): com.pixelquest.app.study.chat.data.remote.StudyChatRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindStudyChatRepository(
        impl: com.pixelquest.app.study.chat.data.StudyChatRepositoryImpl
    ): com.pixelquest.app.study.chat.data.StudyChatRepository

    @Binds
    @Singleton
    abstract fun bindChatFilesRemote(
        impl: com.pixelquest.app.study.chat.data.remote.HttpChatFilesRemote
    ): com.pixelquest.app.study.chat.data.remote.ChatFilesRemote

    @Binds
    @Singleton
    abstract fun bindPickedDocuments(
        impl: com.pixelquest.app.study.chat.data.ContentResolverPickedDocuments
    ): com.pixelquest.app.study.chat.data.PickedDocuments

    @Binds
    @Singleton
    abstract fun bindChatUploadScheduler(
        impl: com.pixelquest.app.study.chat.sync.WorkManagerChatUploadScheduler
    ): com.pixelquest.app.study.chat.data.ChatUploadScheduler

    @Binds
    @Singleton
    abstract fun bindPickedFileReader(
        impl: com.pixelquest.app.study.chat.data.ContentResolverPickedFileReader
    ): com.pixelquest.app.study.chat.data.PickedFileReader

    @Binds
    @Singleton
    abstract fun bindChatLiveSource(
        impl: com.pixelquest.app.study.chat.sync.SupabaseChatLiveSource
    ): com.pixelquest.app.study.chat.sync.ChatLiveSource

    @Binds
    @Singleton
    abstract fun bindChatSendScheduler(
        impl: com.pixelquest.app.study.chat.sync.WorkManagerChatSendScheduler
    ): com.pixelquest.app.study.chat.data.ChatSendScheduler

    @Binds
    @Singleton
    abstract fun bindStudyRepository(
        impl: com.pixelquest.app.study.data.StudyRepositoryImpl
    ): com.pixelquest.app.study.data.StudyRepository

    @Binds
    @Singleton
    abstract fun bindStudySyncScheduler(
        impl: com.pixelquest.app.study.sync.WorkManagerStudySyncScheduler
    ): com.pixelquest.app.study.sync.StudySyncScheduler

    @Binds
    @Singleton
    abstract fun bindStudyTimerStore(
        impl: com.pixelquest.app.study.data.PrefsStudyTimerStore
    ): com.pixelquest.app.study.data.StudyTimerStore

    @Binds
    @Singleton
    abstract fun bindWinnerSeenStore(
        impl: com.pixelquest.app.study.data.PrefsWinnerSeenStore
    ): com.pixelquest.app.study.data.WinnerSeenStore

    @Binds
    @Singleton
    abstract fun bindStudyLiveSource(
        impl: com.pixelquest.app.study.sync.SupabaseStudyLiveSource
    ): com.pixelquest.app.study.sync.StudyLiveSource

    @Binds
    @Singleton
    abstract fun bindStudyReminderSettings(
        impl: com.pixelquest.app.study.data.PrefsStudyReminderSettings
    ): com.pixelquest.app.study.data.StudyReminderSettings

    @Binds
    @Singleton
    abstract fun bindStudyAccountData(
        impl: com.pixelquest.app.study.data.PrefsStudyAccountData
    ): com.pixelquest.app.study.data.StudyAccountData

    @Binds
    @Singleton
    abstract fun bindTipsStore(
        impl: com.pixelquest.app.ui.tips.PrefsTipsStore
    ): com.pixelquest.app.ui.tips.TipsStore
}
