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
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7)
            .fallbackToDestructiveMigration()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    CoroutineScope(Dispatchers.IO).launch {
                        val appDb = databaseProvider.get()
                        // Only rows that don't exist yet: on a first launch with no network, onboarding's
                        // save is what opens (and creates) the database, and the seed used to REPLACE the
                        // name, avatar and difficulty the player had just chosen.
                        appDb.userProfileDao().insertProfileIfAbsent(SeedDataProvider.defaultProfile())
                        appDb.difficultySettingsDao().insertSettingsIfAbsent(SeedDataProvider.defaultDifficultySettings())
                        appDb.streakDao().insertStreakIfAbsent(SeedDataProvider.defaultStreak())
                        SeedDataProvider.initialTasks().forEach { task ->
                            appDb.taskDao().insertTaskIfAbsent(task)
                        }
                        Log.d("PixelQuestSeed", "Database seeded successfully with initial profile, settings, streak, and tasks.")
                    }
                }
            })
            .build()
    }
}
