package com.pixelquest.app.domain.ai

import com.pixelquest.app.data.local.prefs.EncouragementPack
import java.time.LocalDate

/**
 * Uses the day's AI-written lines when there is a fresh pack in the right tone, otherwise the
 * built-in bank. A pack from yesterday still counts, so a late-night reminder isn't left out.
 */
class AiEncouragementProvider(
    private val packSource: () -> EncouragementPack?,
    private val fallback: EncouragementMessageProvider = StaticEncouragementBank
) : EncouragementMessageProvider {

    override fun messageFor(date: LocalDate, taskId: Long, isSimpleMode: Boolean): String {
        val wantedTone = if (isSimpleMode) HabitInsightTone.SIMPLE_MINIMALIST else HabitInsightTone.GAMIFIED_HEROIC
        val pack = packSource()
            ?.takeIf { it.tone == wantedTone && isFresh(it.generatedOn, date) }
        val lines = pack?.messages?.let { EncouragementSanitizer.clean(it, isSimpleMode) }.orEmpty()
        return if (lines.isNotEmpty()) {
            StaticEncouragementBank.pick(lines, date, taskId)
        } else {
            fallback.messageFor(date, taskId, isSimpleMode)
        }
    }

    internal fun isFresh(generatedOn: LocalDate, today: LocalDate): Boolean =
        !generatedOn.isAfter(today) && !generatedOn.isBefore(today.minusDays(1))
}
