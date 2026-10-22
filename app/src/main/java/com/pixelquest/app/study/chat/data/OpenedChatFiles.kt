package com.pixelquest.app.study.chat.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The chat files the player opened, kept in the app's cache for the viewer app (ChatFileOpener):
 * other people's documents. Signing out, or deleting the account, removes them at once (S036),
 * rather than when Android next clears the cache.
 */
@Singleton
class OpenedChatFiles internal constructor(private val folder: File) {

    @Inject
    constructor(@ApplicationContext context: Context) : this(File(context.cacheDir, FOLDER))

    fun clear() {
        folder.deleteRecursively()
    }

    companion object {
        const val FOLDER = "study-files"
    }
}
