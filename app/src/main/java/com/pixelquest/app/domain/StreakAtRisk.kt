package com.pixelquest.app.domain

/**
 * The evening "streak at risk" nudge: whether today still threatens a live streak, and what to say.
 * A day keeps the streak when it reaches the difficulty's perfect-day threshold (StreakCalculator).
 */
object StreakAtRisk {

    /**
     * How many more of today's quests keep the streak, or null when there's nothing to warn about:
     * no streak to lose, nothing due today, or the threshold is already reached.
     */
    fun questsStillNeeded(currentStreak: Int, dueToday: Int, doneToday: Int, threshold: Float): Int? {
        if (currentStreak <= 0 || dueToday <= 0) return null
        if (StreakCalculator.isPerfectDay(doneToday, dueToday, threshold)) return null
        // The smallest number of completions that reaches the threshold.
        val needed = (1..dueToday).first { StreakCalculator.isPerfectDay(it, dueToday, threshold) }
        return (needed - doneToday).coerceAtLeast(1)
    }

    /** Title and text of the notification. */
    fun copy(currentStreak: Int, stillNeeded: Int, isSimpleMode: Boolean): Pair<String, String> {
        val days = if (currentStreak == 1) "1-day" else "$currentStreak-day"
        return if (isSimpleMode) {
            val tasks = if (stillNeeded == 1) "1 more task" else "$stillNeeded more tasks"
            "Your $days streak ends tonight" to "Complete $tasks today to keep it going."
        } else {
            val quests = if (stillNeeded == 1) "1 more quest" else "$stillNeeded more quests"
            "🔥 Your $days streak is at risk!" to "Clear $quests before midnight to keep the flame alive."
        }
    }
}
