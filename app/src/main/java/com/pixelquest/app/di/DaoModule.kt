package com.pixelquest.app.di

import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.local.dao.DifficultySettingsDao
import com.pixelquest.app.data.local.dao.StreakDao
import com.pixelquest.app.data.local.dao.TaskCompletionLogDao
import com.pixelquest.app.data.local.dao.TaskDao
import com.pixelquest.app.data.local.dao.UserProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

import com.pixelquest.app.data.local.dao.InsightCacheDao
import com.pixelquest.app.data.local.dao.LevelHistoryDao

@Module
@InstallIn(SingletonComponent::class)
object DaoModule {

    @Provides
    @Singleton
    fun provideTaskDao(database: AppDatabase): TaskDao = database.taskDao()

    @Provides
    @Singleton
    fun provideTransactionRunner(database: AppDatabase): com.pixelquest.app.data.local.TransactionRunner =
        com.pixelquest.app.data.local.RoomTransactionRunner(database)

    @Provides
    @Singleton
    fun provideStreakDao(database: AppDatabase): StreakDao = database.streakDao()

    @Provides
    @Singleton
    fun provideUserProfileDao(database: AppDatabase): UserProfileDao = database.userProfileDao()

    @Provides
    @Singleton
    fun provideDifficultySettingsDao(database: AppDatabase): DifficultySettingsDao = database.difficultySettingsDao()

    @Provides
    @Singleton
    fun provideTaskCompletionLogDao(database: AppDatabase): TaskCompletionLogDao = database.taskCompletionLogDao()

    @Provides
    @Singleton
    fun provideLevelHistoryDao(database: AppDatabase): LevelHistoryDao = database.levelHistoryDao()

    @Provides
    @Singleton
    fun provideInsightCacheDao(database: AppDatabase): InsightCacheDao = database.insightCacheDao()

    // The Namaz Tracker (docs/namaz-design.md).

    @Provides
    @Singleton
    fun provideNamazLogDao(database: AppDatabase): com.pixelquest.app.data.local.dao.NamazLogDao = database.namazLogDao()

    @Provides
    @Singleton
    fun provideNamazSettingsDao(database: AppDatabase): com.pixelquest.app.data.local.dao.NamazSettingsDao = database.namazSettingsDao()

    // Darood Party's cache (docs/darood-design.md).

    @Provides
    @Singleton
    fun provideDaroodDao(database: AppDatabase): com.pixelquest.app.darood.data.local.DaroodDao = database.daroodDao()

    // Every party's cache (docs/party-core.md): the same tables Darood Party's DAO reads.

    @Provides
    @Singleton
    fun providePartyDao(database: AppDatabase): com.pixelquest.app.party.data.local.PartyDao = database.partyDao()

    // The Quran on the phone (docs/quran-data.md).

    @Provides
    @Singleton
    fun provideQuranDao(database: AppDatabase): com.pixelquest.app.quran.data.local.QuranDao = database.quranDao()

    @Provides
    @Singleton
    fun provideQuranBookmarkDao(database: AppDatabase): com.pixelquest.app.quran.data.local.QuranBookmarkDao = database.quranBookmarkDao()
}
