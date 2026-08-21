package com.pixelquest.app.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.local.SeedDataProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        databaseProvider: Provider<AppDatabase>
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "pixelquest.db"
        )
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    CoroutineScope(Dispatchers.IO).launch {
                        val appDb = databaseProvider.get()
                        appDb.userProfileDao().insertProfile(SeedDataProvider.defaultProfile())
                        appDb.difficultySettingsDao().insertSettings(SeedDataProvider.defaultDifficultySettings())
                        appDb.streakDao().insertStreak(SeedDataProvider.defaultStreak())
                        SeedDataProvider.initialTasks().forEach { task ->
                            appDb.taskDao().insertTask(task)
                        }
                        Log.d("PixelQuestSeed", "Database seeded successfully with initial profile, settings, streak, and tasks.")
                    }
                }
            })
            .build()
    }
}
