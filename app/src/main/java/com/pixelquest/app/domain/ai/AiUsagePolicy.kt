package com.pixelquest.app.domain.ai

/**
 * Step 30: Hard cost and usage safeguards beyond the 6-hour per-request rate limit.
 * Establishes strict daily and monthly caps per user to prevent runaway API usage
 * and guarantee predictable cloud operational costs.
 */
object AiUsagePolicy {
    /**
     * Hard daily cap: Maximum 4 live Gemini calls per calendar day.
     * Aligns with the 6-hour minimum throttle interval (24 hours / 6 hours = 4 requests).
     */
    const val MAX_CALLS_PER_DAY = 4

    /**
     * Hard monthly cap: Maximum 60 live Gemini calls per calendar month.
     * Guarantees average consumption does not exceed 2 calls/day, capping monthly
     * token expenditures per user at negligible cost (< $0.005/month).
     */
    const val MAX_CALLS_PER_MONTH = 60

    fun formatDailyLimitMessage(remaining: Int = 0): String {
        return "You've reached today's insight limit ($MAX_CALLS_PER_DAY/day). Check back tomorrow for a fresh debrief."
    }

    fun formatMonthlyLimitMessage(): String {
        return "You've reached this month's insight limit ($MAX_CALLS_PER_MONTH/month). Check back next month for fresh coaching."
    }
}
