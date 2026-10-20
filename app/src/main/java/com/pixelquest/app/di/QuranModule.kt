package com.pixelquest.app.di

import com.pixelquest.app.quran.data.AndroidQuranAssetSource
import com.pixelquest.app.quran.data.QuranAssetSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** The Quran reader's pieces (docs/quran-data.md). Its DAO is in DaoModule, with the others. */
@Module
@InstallIn(SingletonComponent::class)
abstract class QuranModule {

    /** The bundled files in the APK's assets/quran. */
    @Binds
    @Singleton
    abstract fun bindQuranAssetSource(source: AndroidQuranAssetSource): QuranAssetSource
}
