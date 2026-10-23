package com.pixelquest.app.darood.sync

import android.content.Context
import com.pixelquest.app.R
import com.pixelquest.app.darood.domain.WeekResult
import com.pixelquest.app.darood.domain.WeekTone
import com.pixelquest.app.darood.ui.formatCount

/**
 * What a closed week's notification says (docs/darood-weekly.md, The notification): the player's
 * own place on one line, and every ranked member's place and total when it's expanded.
 */
object WeekRankingText {

    /** "2nd": the place's number with its ending (11th, 12th and 13th, whatever their last digit). */
    fun ordinal(context: Context, position: Int): String {
        val id = when {
            position % 100 in 11..13 -> R.string.darood_ordinal_th
            position % 10 == 1 -> R.string.darood_ordinal_st
            position % 10 == 2 -> R.string.darood_ordinal_nd
            position % 10 == 3 -> R.string.darood_ordinal_rd
            else -> R.string.darood_ordinal_th
        }
        return context.getString(id, formatCount(position))
    }

    /** "Your position this week: 2nd of 8 with 3,200 Darood". Null if [me] wasn't ranked. */
    fun line(context: Context, week: WeekResult, me: String): String? {
        val mine = week.placeOf(me) ?: return null
        return context.getString(R.string.darood_week_notif_place, ordinal(context, mine.position), formatCount(week.members), formatCount(mine.weekTotal))
    }

    /** Every ranked member, in order: "2nd  bram  3,200". The player's own is marked as theirs. */
    fun lines(context: Context, week: WeekResult, me: String, names: Map<String, String?>): List<String> =
        week.places.map { place ->
            val who = if (place.userId.equals(me, ignoreCase = true)) context.getString(R.string.darood_week_you) else names[place.userId] ?: names.entries.firstOrNull { it.key.equals(place.userId, ignoreCase = true) }?.value ?: context.getString(R.string.darood_someone)
            context.getString(R.string.darood_week_notif_row, ordinal(context, place.position), who, formatCount(place.weekTotal))
        }

    /** The kind line for the player's place (WeekTone): null between first and last. */
    fun tone(context: Context, week: WeekResult, me: String): String? = when (WeekTone.of(week, me)) {
        WeekTone.TOP -> context.getString(R.string.darood_week_tone_top)
        WeekTone.ENCOURAGE -> context.getString(R.string.darood_week_tone_encourage)
        WeekTone.NONE, null -> null
    }

    /** The expanded text: the player's line and its kind line, then everyone's. */
    fun expanded(context: Context, week: WeekResult, me: String, names: Map<String, String?>): String =
        (listOfNotNull(line(context, week, me), tone(context, week, me)) + lines(context, week, me, names)).joinToString("\n")
}
