package com.pixelquest.app.data.backup

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What the phone had just before the last restore, kept in the app's private storage so the restore
 * can be undone. Restoring the wrong file (someone else's, or an old one) used to replace the
 * player's progress for good. The copy stays until it's used or the next restore replaces it, and
 * isn't part of Android's backup (only the database and settings are; res/xml/backup_rules.xml).
 */
@Singleton
class RestoreSafetyCopy internal constructor(private val file: File) {

    @Inject
    constructor(@ApplicationContext context: Context) : this(File(context.filesDir, FILE_NAME))

    fun exists(): Boolean = file.exists()

    fun save(json: String) {
        val tmp = File(file.parentFile, "$FILE_NAME.tmp")
        tmp.writeText(json)
        if (!tmp.renameTo(file)) {
            file.writeText(json)
            tmp.delete()
        }
    }

    fun read(): String? = file.takeIf { it.exists() }?.readText()

    fun clear() {
        file.delete()
    }

    companion object {
        const val FILE_NAME = "before-restore.json"
    }
}
