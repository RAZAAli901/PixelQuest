package com.pixelquest.app.data.local

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer

/**
 * Keeps a copy of the saved game's file before an update changes its layout. Room migrates the
 * database in place (AppDatabase.ALL_MIGRATIONS), so the player's only copy is the one being
 * changed; every path is tested (DatabaseUpgradeTest), but if one still had a mistake, the old file
 * is kept in the app's private storage until the next update that changes the layout, and a fixed
 * update can get the data back from it.
 */
object PreUpdateCopy {

    const val DIR_NAME = "pre-update"

    private const val USER_VERSION_OFFSET = 60L
    private const val WAL_HEADER_SIZE = 32
    private const val WAL_FRAME_HEADER_SIZE = 24
    private const val WAL_MAGIC_LE = 0x377f0682
    private const val WAL_MAGIC_BE = 0x377f0683

    /**
     * The layout version a database file was saved with (its user_version); null if it isn't one.
     * Room keeps the game open in WAL mode, where changes, the version among them, wait in the -wal
     * file until a checkpoint, so the newest version there counts over the file's own header (which
     * says 0 for a game never checkpointed, or the old version just after an update).
     */
    fun versionOf(dbFile: File): Int? {
        val header = headerVersionOf(dbFile) ?: return null
        return walVersionOf(File(dbFile.path + "-wal")) ?: header
    }

    /** The user_version in the file's own header, without the -wal; null if it isn't a database. */
    private fun headerVersionOf(dbFile: File): Int? {
        if (!dbFile.isFile || dbFile.length() < 100) return null
        return RandomAccessFile(dbFile, "r").use { file ->
            val magic = ByteArray(16).also { file.readFully(it) }
            if (String(magic, Charsets.US_ASCII) != "SQLite format 3\u0000") return null
            file.seek(USER_VERSION_OFFSET)
            file.readInt()
        }
    }

    /**
     * The user_version in the newest committed copy of page 1 in a -wal file (sqlite.org/fileformat2.html,
     * "WAL File Format"), or null when it holds none. Frames from before the -wal was last reset carry
     * other salts, and frames after the last commit belong to an unfinished write; neither counts.
     */
    internal fun walVersionOf(wal: File): Int? {
        if (!wal.isFile || wal.length() < WAL_HEADER_SIZE) return null
        RandomAccessFile(wal, "r").use { file ->
            val header = ByteBuffer.wrap(ByteArray(WAL_HEADER_SIZE).also { file.readFully(it) })
            if (header.getInt(0) != WAL_MAGIC_LE && header.getInt(0) != WAL_MAGIC_BE) return null
            val pageSize = header.getInt(8).let { if (it == 1) 65_536 else it }
            if (pageSize < 512) return null
            val salts = header.getInt(16) to header.getInt(20)
            val frame = ByteArray(WAL_FRAME_HEADER_SIZE)
            var committed: Int? = null
            var pending: Int? = null
            var offset = WAL_HEADER_SIZE.toLong()
            while (offset + WAL_FRAME_HEADER_SIZE + pageSize <= file.length()) {
                file.seek(offset)
                file.readFully(frame)
                val frameHeader = ByteBuffer.wrap(frame)
                if ((frameHeader.getInt(8) to frameHeader.getInt(12)) != salts) break
                if (frameHeader.getInt(0) == 1) {
                    file.seek(offset + WAL_FRAME_HEADER_SIZE + USER_VERSION_OFFSET)
                    pending = file.readInt()
                }
                // A frame giving the database's size ends a transaction: its pages count from here on.
                if (frameHeader.getInt(4) != 0) {
                    if (pending != null) committed = pending
                    pending = null
                }
                offset += WAL_FRAME_HEADER_SIZE + pageSize
            }
            return committed
        }
    }

    /**
     * Copies [dbFile] into [dir] when it was saved with a layout older than [currentVersion], with
     * its -wal and -shm files (recent changes can still be in them), replacing an earlier copy.
     * Returns the version copied, or null when nothing needed copying. The version counts the -wal
     * ([versionOf]), so the launch after an update, with the migration still in the -wal, doesn't
     * copy the migrated game over the copy taken before it.
     */
    fun keepIfOlder(dbFile: File, dir: File, currentVersion: Int): Int? {
        // A version never goes down, so a header already at the current one settles it without
        // reading the -wal frame by frame (this runs at every launch, before the first screen).
        if ((headerVersionOf(dbFile) ?: return null) >= currentVersion) return null
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
