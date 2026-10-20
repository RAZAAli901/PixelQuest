package com.pixelquest.app.quran.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Toggling a bookmark on the real table: on, then off, each surah's own. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class BookmarkRepositoryTest {

    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
        .allowMainThreadQueries().build()
    private val repository = RoomBookmarkRepository(db.quranBookmarkDao()) { 1_000L }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun aToggle_bookmarks_thenRemovesIt() = runBlocking {
        assertTrue(repository.toggle(2, 255))
        assertEquals(setOf(255), repository.bookmarkedIn(2).first())
        assertTrue("another surah's are its own", repository.bookmarkedIn(3).first().isEmpty())

        assertFalse(repository.toggle(2, 255))
        assertTrue(repository.bookmarkedIn(2).first().isEmpty())
    }

    @Test
    fun all_inTheQuransOrder() = runBlocking {
        repository.toggle(36, 1)
        repository.toggle(2, 255)

        assertEquals(listOf(2 to 255, 36 to 1), repository.all().first())
    }
}
