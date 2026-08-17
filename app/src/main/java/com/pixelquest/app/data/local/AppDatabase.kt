package com.pixelquest.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pixelquest.app.data.local.dao.StreakDao
import com.pixelquest.app.data.local.dao.TaskDao
import com.pixelquest.app.data.local.dao.UserProfileDao
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity

@Database(
    entities = [
        TaskEntity::class,
        StreakEntity::class,
        UserProfileEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun streakDao(): StreakDao
    abstract fun userProfileDao(): UserProfileDao
}
