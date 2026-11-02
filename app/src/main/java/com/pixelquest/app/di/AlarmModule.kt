package com.pixelquest.app.di

import com.pixelquest.app.scheduling.AlarmScheduler
import com.pixelquest.app.scheduling.AndroidAlarmScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AlarmModule {

    @Binds
    @Singleton
    abstract fun bindAlarmScheduler(
        impl: AndroidAlarmScheduler,
    ): AlarmScheduler
}
