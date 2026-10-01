package com.pixelquest.app.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import com.pixelquest.app.domain.ai.AiUsagePolicy
import com.pixelquest.app.domain.ai.AiUsageTracker
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Step 33: Lightweight local SharedPreferences-backed usage counter for tracking
 * daily and monthly Gemini calls toward hard cost safeguard caps without server round-trips.
 */
@Singleton
class PreferencesAiUsageTracker @Inject constructor(
    private val prefs: SharedPreferences,
    private val maxDaily: Int = AiUsagePolicy.MAX_CALLS_PER_DAY,
    private val maxMonthly: Int = AiUsagePolicy.MAX_CALLS_PER_MONTH
) : AiUsageTracker {

    constructor(
        context: Context,
        maxDaily: Int = AiUsagePolicy.MAX_CALLS_PER_DAY,
        maxMonthly: Int = AiUsagePolicy.MAX_CALLS_PER_MONTH
    ) : this(
        prefs = context.getSharedPreferences("pixelquest_ai_usage", Context.MODE_PRIVATE),
        maxDaily = maxDaily,
        maxMonthly = maxMonthly
    )

    companion object {
        private const val KEY_LAST_DATE = "key_ai_usage_last_date"
        private const val KEY_DAILY_COUNT = "key_ai_usage_daily_count"
        private const val KEY_MONTHLY_COUNT = "key_ai_usage_monthly_count"
    }

    @Synchronized
    private fun synchronize(date: LocalDate): Pair<Int, Int> {
        val storedDateStr = prefs.getString(KEY_LAST_DATE, null)
        val storedDate = storedDateStr?.let {
            try { LocalDate.parse(it) } catch (_: Exception) { null }
        }

        var daily = prefs.getInt(KEY_DAILY_COUNT, 0)
        var monthly = prefs.getInt(KEY_MONTHLY_COUNT, 0)

        if (storedDate == null) {
            daily = 0
            monthly = 0
        } else if (storedDate.year != date.year || storedDate.month != date.month) {
            daily = 0
            monthly = 0
            prefs.edit()
                .putString(KEY_LAST_DATE, date.toString())
                .putInt(KEY_DAILY_COUNT, 0)
                .putInt(KEY_MONTHLY_COUNT, 0)
                .apply()
        } else if (storedDate != date) {
            daily = 0
            prefs.edit()
                .putString(KEY_LAST_DATE, date.toString())
                .putInt(KEY_DAILY_COUNT, 0)
                .apply()
        }

        return Pair(daily, monthly)
    }

    override fun canMakeCall(date: LocalDate): Boolean {
        val (daily, monthly) = synchronize(date)
        return daily < maxDaily && monthly < maxMonthly
    }

    @Synchronized
    override fun recordCall(date: LocalDate) {
        val (daily, monthly) = synchronize(date)
        val newDaily = daily + 1
        val newMonthly = monthly + 1
        prefs.edit()
            .putString(KEY_LAST_DATE, date.toString())
            .putInt(KEY_DAILY_COUNT, newDaily)
            .putInt(KEY_MONTHLY_COUNT, newMonthly)
            .apply()
    }

    override fun getDailyCallsRemaining(date: LocalDate): Int {
        val (daily, _) = synchronize(date)
        return (maxDaily - daily).coerceAtLeast(0)
    }

    override fun getMonthlyCallsRemaining(date: LocalDate): Int {
        val (_, monthly) = synchronize(date)
        return (maxMonthly - monthly).coerceAtLeast(0)
    }

    override fun isDailyCapReached(date: LocalDate): Boolean {
        val (daily, _) = synchronize(date)
        return daily >= maxDaily
    }

    override fun isMonthlyCapReached(date: LocalDate): Boolean {
        val (_, monthly) = synchronize(date)
        return monthly >= maxMonthly
    }

    override fun getDailyCallsCount(date: LocalDate): Int {
        val (daily, _) = synchronize(date)
        return daily
    }

    override fun getMonthlyCallsCount(date: LocalDate): Int {
        val (_, monthly) = synchronize(date)
        return monthly
    }
}
