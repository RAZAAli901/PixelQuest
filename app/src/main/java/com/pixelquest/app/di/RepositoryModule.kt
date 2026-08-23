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
}
