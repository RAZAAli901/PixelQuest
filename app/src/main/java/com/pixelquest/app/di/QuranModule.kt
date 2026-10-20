package com.pixelquest.app.di

import com.pixelquest.app.quran.data.AndroidQuranAssetSource
import com.pixelquest.app.quran.data.QuranAssetSource
import com.pixelquest.app.quran.data.QuranRepository
import com.pixelquest.app.quran.data.QuranRepositoryImpl
import dagger.Binds
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** The Quran reader's pieces (docs/quran-data.md). Its DAO is in DaoModule, with the others. */
@Module
@InstallIn(SingletonComponent::class)
abstract class QuranModule {

    /** The player's bookmarks. */
    @Binds
    @Singleton
    abstract fun bindBookmarkRepository(repository: com.pixelquest.app.quran.data.RoomBookmarkRepository): com.pixelquest.app.quran.data.BookmarkRepository

    /** How the player reads, kept between visits. */
    @Binds
    @Singleton
    abstract fun bindReadingSettingsStore(store: com.pixelquest.app.quran.data.DataStoreReadingSettingsStore): com.pixelquest.app.quran.data.ReadingSettingsStore

    companion object {
        /** The reading settings' DataStore: one per process, as DataStore requires. */
        @Provides
        @Singleton
        fun provideReadingDataStore(
            @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context
        ): androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> =
            androidx.datastore.preferences.core.PreferenceDataStoreFactory.create {
                context.preferencesDataStoreFile(com.pixelquest.app.quran.data.DataStoreReadingSettingsStore.NAME)
            }
    }

    /** The bundled files in the APK's assets/quran. */
    @Binds
    @Singleton
    abstract fun bindQuranAssetSource(source: AndroidQuranAssetSource): QuranAssetSource

    /** Which translation the player reads. */
    @Binds
    @Singleton
    abstract fun bindTranslationChoiceStore(store: com.pixelquest.app.quran.data.PrefsTranslationChoiceStore): com.pixelquest.app.quran.data.TranslationChoiceStore

    /** The Quran on the phone, for the reader. Its loader, QuranImporter, is injected as itself. */
    @Binds
    @Singleton
    abstract fun bindQuranRepository(repository: QuranRepositoryImpl): QuranRepository
}
