package com.pixelquest.app.domain.namaz

/**
 * What marking one prayer earns (docs/namaz-design.md, XP).
 *
 * @param prayer the prayer's own XP: on time or late, within the day's cap.
 * @param allFiveBonus when this prayer made five on its day.
 * @param weekBonus when that day made a run of all-five days a multiple of seven.
 */
data class NamazXpAward(val prayer: Int, val allFiveBonus: Int = 0, val weekBonus: Int = 0) {
    /** The bonuses this prayer unlocked: taken back with it if it's undone. */
    val bonus: Int get() = allFiveBonus + weekBonus

    val total: Int get() = prayer + bonus

    companion object {
        val NONE = NamazXpAward(0)
    }
}

/** Namaz XP by the rules in [NamazXpConfig]. Pure: the caller says what the day already holds. */
object NamazXpCalculator {

    /**
     * @param onTime marked before the next prayer started (Isha: before the next Fajr).
     * @param prayedBefore the prayer day's prayers already marked prayed, not counting this one.
     * @param earnedToday the day's XP so far from its prayers and its all-five bonus, which the
     *   daily cap counts (the week's bonus isn't counted).
     * @param runIfComplete the run of all-five days, ending with this prayer's day, that the day
     *   would make if this prayer completes it.
     */
    fun award(onTime: Boolean, prayedBefore: Int, earnedToday: Int, runIfComplete: Int): NamazXpAward {
        val base = if (onTime) NamazXpConfig.PER_PRAYER else NamazXpConfig.PER_PRAYER_LATE
        val completes = prayedBefore + 1 == NamazStreak.PRAYERS_PER_DAY
        val allFive = if (completes) NamazXpConfig.ALL_FIVE_BONUS else 0
        // The cap holds the prayers and the all-five bonus together; the prayer's own XP comes first.
        val room = (NamazXpConfig.DAILY_CAP - earnedToday).coerceAtLeast(0)
        val prayerXp = minOf(base, room)
        val allFiveXp = minOf(allFive, room - prayerXp)
        val week = if (completes && runIfComplete > 0 && runIfComplete % NamazXpConfig.WEEK_STREAK_DAYS == 0) {
            NamazXpConfig.WEEK_STREAK_BONUS
        } else {
            0
        }
        return NamazXpAward(prayerXp, allFiveXp, week)
    }
}
