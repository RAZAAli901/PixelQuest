package com.pixelquest.app.data.local

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** Before an update changes the saved game's layout, the old file is copied, so a migration mistake can be undone. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PreUpdateCopyTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val root = File(context.filesDir, "pre-update-test")
    private val dbFile = File(root, "game.db")
    private val dir = File(root, PreUpdateCopy.DIR_NAME)

    @Before
    fun setUp() {
        root.deleteRecursively()
        root.mkdirs()
    }

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    private fun saveGame(version: Int, hero: String) {
        listOf("", "-wal", "-shm", "-journal").forEach { File(dbFile.path + it).delete() }
        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { db ->
            db.execSQL("CREATE TABLE user_profile (username TEXT)")
            db.execSQL("INSERT INTO user_profile VALUES (?)", arrayOf(hero))
            db.version = version
        }
    }

    private fun heroIn(file: File): String =
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("SELECT username FROM user_profile", null).use { it.moveToFirst(); it.getString(0) }
        }

    @Test
    fun anOlderLayout_isCopied_withItsData() {
        saveGame(3, "Aria")

        assertEquals(3, PreUpdateCopy.keepIfOlder(dbFile, dir, currentVersion = 7))

        val copy = File(dir, "pixelquest-v3.db")
        assertEquals(3, PreUpdateCopy.versionOf(copy))
        assertEquals("Aria", heroIn(copy))
    }

    @Test
    fun theCurrentLayout_isntCopied() {
        saveGame(7, "Aria")

        assertNull(PreUpdateCopy.keepIfOlder(dbFile, dir, currentVersion = 7))
        assertFalse(dir.exists())
    }

    @Test
    fun theNextUpdate_replacesTheEarlierCopy() {
        saveGame(3, "Aria")
        PreUpdateCopy.keepIfOlder(dbFile, dir, currentVersion = 7)
        saveGame(7, "Bram")

        PreUpdateCopy.keepIfOlder(dbFile, dir, currentVersion = 8)

        assertEquals(listOf("pixelquest-v7.db"), dir.list()!!.filter { it.endsWith(".db") })
        assertEquals("Bram", heroIn(File(dir, "pixelquest-v7.db")))
    }

    @Test
    fun noFile_orNotADatabase_isLeftAlone() {
        assertNull(PreUpdateCopy.keepIfOlder(dbFile, dir, currentVersion = 7))
        dbFile.writeText("not a database".repeat(20))
        assertNull(PreUpdateCopy.keepIfOlder(dbFile, dir, currentVersion = 7))
        assertFalse(dir.exists())
    }
}
