package com.pixelquest.app.quran.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.local.RoomTransactionRunner
import com.pixelquest.app.quran.data.local.QuranBookmarkEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The player's bookmarks: in the Quran's order, one per ayah, and untouched by a reload of the Quran. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class QuranBookmarkDaoTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    private val dao = db.quranBookmarkDao()

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun bookmarks_comeInTheQuransOrder() = runBlocking {
        dao.insert(QuranBookmarkEntity(36, 1, 300))
        dao.insert(QuranBookmarkEntity(2, 255, 100))
        dao.insert(QuranBookmarkEntity(2, 1, 200))

        assertEquals(listOf(2 to 1, 2 to 255, 36 to 1), dao.observeAll().first().map { it.surah to it.ayah })
        assertEquals(listOf(1, 255), dao.observeIn(2).first())
    }

    @Test
    fun bookmarkingTwice_keepsTheFirstTime() = runBlocking {
        dao.insert(QuranBookmarkEntity(2, 255, 100))
        dao.insert(QuranBookmarkEntity(2, 255, 999))

        assertEquals(listOf(100L), dao.observeAll().first().map { it.createdAt })
    }

    @Test
    fun removing_takesOnlyThatOne() = runBlocking {
        dao.insert(QuranBookmarkEntity(2, 255, 100))
        dao.insert(QuranBookmarkEntity(2, 256, 100))

        dao.delete(2, 255)

        assertEquals(listOf(256), dao.observeIn(2).first())
    }

    @Test
    fun reloadingTheQuran_keepsTheBookmarks() = runBlocking {
        dao.insert(QuranBookmarkEntity(2, 255, 100))
        val importer = QuranImporter(AndroidQuranAssetSource(context), db.quranDao(), RoomTransactionRunner(db)) { QuranImporter.TRANSLATION_FILE }

        importer.import()
        importer.import()

        assertEquals(1, dao.count())
    }
}
