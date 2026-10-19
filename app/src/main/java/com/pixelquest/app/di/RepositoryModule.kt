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
    abstract fun bindDaroodLiveSource(
        impl: com.pixelquest.app.darood.sync.SupabaseDaroodLiveSource
    ): com.pixelquest.app.darood.sync.DaroodLiveSource

    @Binds
    @Singleton
    abstract fun bindDaroodCacheOwner(
        impl: com.pixelquest.app.darood.sync.PrefsDaroodCacheOwner
    ): com.pixelquest.app.darood.sync.DaroodCacheOwner
}
