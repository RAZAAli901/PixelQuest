package com.pixelquest.app.data.backup

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Daily automatic backups kept on the phone (AutoBackupWorker), the last [KEEP] days, in the app's
 * private storage. A corrupt database file is deleted and started over by Android (SQLite's default
 * handling), and a player without an exported backup lost everything; Settings can restore one of
 * these instead. They're the same files as an export (DataExportImport).
 */
@Singleton
class AutoBackups internal constructor(private val dir: File) {

    @Inject
    constructor(@ApplicationContext context: Context) : this(File(context.filesDir, DIR_NAME))

    /** The days with a backup, newest first. */
    fun days(): List<LocalDate> =
        dir.listFiles().orEmpty()
            .mapNotNull { file -> NAME.matchEntire(file.name)?.let { runCatching { LocalDate.parse(it.groupValues[1]) }.getOrNull() } }
            .sortedDescending()

    fun read(day: LocalDate): String? = fileFor(day).takeIf { it.exists() }?.readText()

    /** Saves [day]'s backup (replacing one already saved that day) and drops all but the newest [KEEP]. */
    fun save(day: LocalDate, json: String) {
        dir.mkdirs()
        val tmp = File(dir, "${fileFor(day).name}.tmp")
        tmp.writeText(json)
        val target = fileFor(day)
        if (!tmp.renameTo(target)) {
            target.delete()
            if (!tmp.renameTo(target)) {
                target.writeText(json)
                tmp.delete()
            }
        }
        days().drop(KEEP).forEach { fileFor(it).delete() }
    }

    /**
     * Backups up to this day aren't offered again when the game is found empty (LostProgress): the
     * player said not now, started a new hero instead, or reset their progress on purpose.
     */
    fun declinedThrough(): LocalDate? =
        File(dir, DECLINED).takeIf { it.exists() }?.readText()?.trim()?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    fun decline(through: LocalDate) {
        dir.mkdirs()
        File(dir, DECLINED).writeText(through.toString())
    }

    /** Declines every backup kept so far (RESET ALL PROGRESS); they stay listed in Settings. */
    fun declineAll() {
        days().firstOrNull()?.let { decline(it) }
    }

    /** The days that may still be offered when the game is found empty, newest first. */
    fun offerable(): List<LocalDate> {
        val declined = declinedThrough()
        return days().filter { declined == null || it > declined }
    }

    /**
     * The newest offerable backup that reads back and has progress (BackupPayload.hasProgress), for
     * the welcome screen and the dashboard alike: a file cut short, or an empty game saved before
     * Step 29, is skipped for an older one rather than offered and then failing.
     */
    fun newestWorthOffering(): Pair<LocalDate, BackupPayload>? =
        offerable().firstNotNullOfOrNull { day ->
            read(day)
                ?.let { runCatching { DataExportImport.importFromJson(it) }.getOrNull() }
                ?.takeIf { it.isUsable && it.hasProgress }
                ?.let { day to it }
        }

    private fun fileFor(day: LocalDate) = File(dir, "pixelquest-auto-$day.json")

    companion object {
        const val DIR_NAME = "auto-backups"
        const val KEEP = 3
        private const val DECLINED = "declined-through.txt"
        private val NAME = Regex("""pixelquest-auto-(\d{4}-\d{2}-\d{2})\.json""")
    }
}
