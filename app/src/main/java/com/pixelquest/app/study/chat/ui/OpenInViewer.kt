package com.pixelquest.app.study.chat.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.pixelquest.app.R
import com.pixelquest.app.study.chat.data.OpenedFile

/** The FileProvider's authority for the study chat's opened files (the manifest's, and study_file_paths). */
fun studyFilesAuthority(context: Context) = context.packageName + ".studyfiles"

/**
 * Hands a fetched file to the system viewer the player chooses, with leave to read that one file
 * only, for as long as the viewer has it open. False when no app on the phone opens its kind.
 */
fun openInViewer(context: Context, saved: OpenedFile.Saved): Boolean {
    val uri = FileProvider.getUriForFile(context, studyFilesAuthority(context), saved.file)
    val view = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, saved.mime)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    return try {
        context.startActivity(Intent.createChooser(view, context.getString(R.string.study_file_open_with)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
