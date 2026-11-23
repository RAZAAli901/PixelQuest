package com.pixelquest.app.tune.di

import com.pixelquest.app.tune.data.TuneRepository
import com.pixelquest.app.tune.data.TuneRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TuneModule {

    @Binds
    @Singleton
    abstract fun bindTuneRepository(
        impl: TuneRepositoryImpl,
    ): TuneRepository
}
