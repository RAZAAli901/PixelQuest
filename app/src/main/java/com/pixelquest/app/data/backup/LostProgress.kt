package com.pixelquest.app.data.backup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A game found empty while the phone kept an automatic backup with progress. Android deletes a
 * corrupt database and the game starts over, but the onboarding flag lives in the settings, which
 * survive: the app opened an empty dashboard rather than the welcome screen that offers the backup
 * (seen on the emulator), so the backups sat unnoticed in Settings.
 */
@Singleton
class LostProgress @Inject constructor(
    private val backupRestorer: BackupRestorer,
    private val autoBackups: AutoBackups
) {
    data class Offer(val day: LocalDate, val payload: BackupPayload)

    /** The newest automatic backup with progress, when the game has none and it wasn't declined. */
    suspend fun check(): Offer? = withContext(Dispatchers.IO) {
        if (backupRestorer.snapshot().hasProgress) return@withContext null
        autoBackups.newestWorthOffering()?.let { (day, payload) -> Offer(day, payload) }
    }

    suspend fun restore(offer: Offer) = backupRestorer.restore(offer.payload)

    /** Not now: it isn't offered again, and stays in Settings > Data & Backup. */
    fun decline(offer: Offer) = autoBackups.decline(offer.day)
}
