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

@Module
@InstallIn(SingletonComponent::class)
object DaoModule {

    @Provides
    @Singleton
    fun provideTaskDao(database: AppDatabase): TaskDao = database.taskDao()

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
}
