package com.pixelquest.app.data.local

import java.io.File
import java.io.RandomAccessFile

/**
 * Keeps a copy of the saved game's file before an update changes its layout. Room migrates the
 * database in place (AppDatabase.ALL_MIGRATIONS), so the player's only copy is the one being
 * changed; every path is tested (DatabaseUpgradeTest), but if one still had a mistake, the old file
 * is kept in the app's private storage until the next update that changes the layout, and a fixed
 * update can get the data back from it.
 */
object PreUpdateCopy {

    const val DIR_NAME = "pre-update"

    /** The layout version a database file was saved with (its header's user_version); null if it isn't one. */
    fun versionOf(dbFile: File): Int? {
        if (!dbFile.isFile || dbFile.length() < 100) return null
        RandomAccessFile(dbFile, "r").use { file ->
            val magic = ByteArray(16).also { file.readFully(it) }
            if (String(magic, Charsets.US_ASCII) != "SQLite format 3\u0000") return null
            file.seek(60)
            return file.readInt()
        }
    }

    /**
     * Copies [dbFile] into [dir] when it was saved with a layout older than [currentVersion], with
     * its -wal and -shm files (recent changes can still be in them), replacing an earlier copy.
     * Returns the version copied, or null when nothing needed copying. A file whose header is behind
     * its -wal (an update stopped before SQLite tidied up) is copied once more; that's harmless.
     */
    fun keepIfOlder(dbFile: File, dir: File, currentVersion: Int): Int? {
        val version = versionOf(dbFile) ?: return null
        if (version <= 0 || version >= currentVersion) return null
        dir.deleteRecursively()
        dir.mkdirs()
        for (suffix in listOf("", "-wal", "-shm")) {
            val source = File(dbFile.path + suffix)
            if (source.exists()) source.copyTo(File(dir, "pixelquest-v$version.db$suffix"), overwrite = true)
        }
        return version
    }
}
