package com.pixelquest.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.local.SeedDataProvider
import com.pixelquest.app.util.AppLog
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

    const val DATABASE_NAME = "pixelquest.db"

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        databaseProvider: Provider<AppDatabase>,
    ): AppDatabase {
        // Before the migrations below change the saved game, a copy of the old file (PreUpdateCopy).
        // A copy that fails (a full disk, say) mustn't stop the game from opening.
        try {
            com.pixelquest.app.data.local.PreUpdateCopy.keepIfOlder(
                context.getDatabasePath(DATABASE_NAME),
                java.io.File(context.filesDir, com.pixelquest.app.data.local.PreUpdateCopy.DIR_NAME),
                AppDatabase.ALL_MIGRATIONS.maxOf { it.endVersion },
            )
        } catch (e: Exception) {
            AppLog.w("PixelQuestDb", "No copy kept before the update", e)
        }
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            DATABASE_NAME,
        )
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            // Only a downgrade may start over. An update with no migration path used to wipe the
            // player's quests, streak and level silently; every path is now tested
            // (DatabaseUpgradeTest), and a missing one fails at start, data intact, until a fixed
            // update ships.
            .fallbackToDestructiveMigrationOnDowngrade()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO).launch {
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
                        AppLog.d("PixelQuestSeed", "Database seeded successfully with initial profile, settings, streak, and tasks.")
                    }
                }
            })
            .build()
    }
}
