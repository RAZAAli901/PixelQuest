package com.pixelquest.app.study.chat.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A document the player picked to share, as the system's picker gave it: its address (read with
 * the permission the picker grants for it, never a path), and its name, type and size as the
 * system says them. Only claims, until the server checks the bytes (docs/study-chat.md).
 */
data class PickedFile(val uri: String, val name: String, val mime: String, val sizeBytes: Long)

/** Reads what a picked document says it is: Android's ContentResolver in the app, a fake in tests. */
fun interface PickedFileReader {
    /** The picked document's name, type and size; null if it can't be read (it went, or isn't openable). */
    suspend fun describe(uri: String): PickedFile?
}

@Singleton
class ContentResolverPickedFileReader @Inject constructor(
    @ApplicationContext private val context: Context
) : PickedFileReader {

    override suspend fun describe(uri: String): PickedFile? = withContext(Dispatchers.IO) {
        val parsed = Uri.parse(uri)
        // Only documents the picker handed over: a content:// address, never a file path.
        if (parsed.scheme != "content") return@withContext null
        val resolver = context.contentResolver
        runCatching {
            resolver.query(parsed, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val name = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)).orEmpty()
                val sizeColumn = cursor.getColumnIndexOrThrow(OpenableColumns.SIZE)
                val size = if (cursor.isNull(sizeColumn)) -1L else cursor.getLong(sizeColumn)
                PickedFile(uri, name, resolver.getType(parsed).orEmpty(), size)
            }
        }.getOrNull()
    }
}
