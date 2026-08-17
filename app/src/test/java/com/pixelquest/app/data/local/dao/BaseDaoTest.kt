package com.pixelquest.app.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import org.junit.After
import org.junit.Before

abstract class BaseDaoTest {
    protected lateinit var database: AppDatabase

    @Before
    open fun createDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    open fun closeDb() {
        if (::database.isInitialized) {
            database.close()
        }
    }
}
