package com.pixelquest.app.domain.ai

/**
 * Step 8: Formats user-friendly rate limit and cooldown messages for AI insights.
 */
object AiRateLimitFormatter {

    /**
     * Formats a clear in-app guidance message for rate-limited insight requests.
     *
     * @param remainingSeconds Seconds remaining in the cooldown window.
     * @return Human-readable guidance string (e.g. "Check back in 6 hours for a fresh insight.")
     */
    fun formatCooldownMessage(remainingSeconds: Long): String {
        return when {
            remainingSeconds <= 0L -> "You can generate a fresh insight now."
            remainingSeconds >= 7200L -> {
                val hours = (remainingSeconds + 3599) / 3600
                "Check back in $hours hours for a fresh insight."
            }
            remainingSeconds >= 3600L -> "Check back in 1 hour for a fresh insight."
            remainingSeconds >= 60L -> {
                val minutes = (remainingSeconds + 59) / 60
                "Check back in $minutes minutes for a fresh insight."
            }
            else -> "Check back in $remainingSeconds seconds for a fresh insight."
        }
    }
}
