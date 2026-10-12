package com.pixelquest.app.data.backup

/**
 * What a backup holds, in a line for the restore dialog, which used to warn without saying what the
 * file had: "Aria, level 5, a 4-day streak and 12 quests, with settings. Saved by PixelQuest 1.1.0."
 */
object BackupSummary {

    fun describe(payload: BackupPayload): String {
        val parts = mutableListOf<String>()
        payload.userProfile?.let { parts += "${it.username}, level ${it.level}" }
        payload.streak?.currentStreak?.takeIf { it > 0 }?.let { parts += "a $it-day streak" }
        val quests = payload.tasks.size
        parts += if (quests == 1) "1 quest" else "$quests quests"

        val holds = when (parts.size) {
            1 -> parts[0]
            else -> parts.dropLast(1).joinToString(", ") + " and " + parts.last()
        }
        val withSettings = if (payload.settings != null) ", with settings" else ""
        val source = when {
            payload.appVersion != null -> " Saved by PixelQuest ${payload.appVersion}."
            payload.formatVersion == 0 -> " Saved by an earlier PixelQuest."
            else -> ""
        }
        return "$holds$withSettings.$source"
    }
}
