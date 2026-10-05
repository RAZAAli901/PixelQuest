package com.pixelquest.app.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import org.junit.After
import org.junit.Before

/** Room needs an Android context; Robolectric provides one on the JVM (subclasses inherit the runner). */
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34], application = android.app.Application::class)
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
