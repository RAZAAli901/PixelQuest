package com.pixelquest.app.domain.ai

import java.time.LocalDate

/**
 * Step 32 & 33: Contract for tracking and enforcing hard daily and monthly
 * Gemini API usage caps per user without requiring server round-trips.
 */
interface AiUsageTracker {
    /**
     * Returns true if both daily and monthly usage caps permit dispatching a live Gemini call.
     */
    fun canMakeCall(date: LocalDate = LocalDate.now()): Boolean

    /**
     * Records a completed live Gemini API call in both daily and monthly buckets.
     */
    fun recordCall(date: LocalDate = LocalDate.now())

    /**
     * Returns the remaining number of allowed calls for today.
     */
    fun getDailyCallsRemaining(date: LocalDate = LocalDate.now()): Int

    /**
     * Returns the remaining number of allowed calls for the current calendar month.
     */
    fun getMonthlyCallsRemaining(date: LocalDate = LocalDate.now()): Int

    /**
     * Returns true if the daily cap has been reached for the given date.
     */
    fun isDailyCapReached(date: LocalDate = LocalDate.now()): Boolean

    /**
     * Returns true if the monthly cap has been reached for the given date.
     */
    fun isMonthlyCapReached(date: LocalDate = LocalDate.now()): Boolean

    /**
     * Returns the current daily call count for the given date.
     */
    fun getDailyCallsCount(date: LocalDate = LocalDate.now()): Int

    /**
     * Returns the current monthly call count for the given date.
     */
    fun getMonthlyCallsCount(date: LocalDate = LocalDate.now()): Int
}

/**
 * In-memory reference implementation of [AiUsageTracker] used for unit testing
 * and rapid verification of cap enforcement across boundary conditions.
 */
class InMemoryAiUsageTracker(
    private val maxDaily: Int = AiUsagePolicy.MAX_CALLS_PER_DAY,
    private val maxMonthly: Int = AiUsagePolicy.MAX_CALLS_PER_MONTH
) : AiUsageTracker {

    private var lastRecordedDate: LocalDate? = null
    private var dailyCount: Int = 0
    private var monthlyCount: Int = 0

    override fun canMakeCall(date: LocalDate): Boolean {
        synchronizeBuckets(date)
        return dailyCount < maxDaily && monthlyCount < maxMonthly
    }

    override fun recordCall(date: LocalDate) {
        synchronizeBuckets(date)
        dailyCount++
        monthlyCount++
        lastRecordedDate = date
    }

    override fun getDailyCallsRemaining(date: LocalDate): Int {
        synchronizeBuckets(date)
        return (maxDaily - dailyCount).coerceAtLeast(0)
    }

    override fun getMonthlyCallsRemaining(date: LocalDate): Int {
        synchronizeBuckets(date)
        return (maxMonthly - monthlyCount).coerceAtLeast(0)
    }

    override fun isDailyCapReached(date: LocalDate): Boolean {
        synchronizeBuckets(date)
        return dailyCount >= maxDaily
    }

    override fun isMonthlyCapReached(date: LocalDate): Boolean {
        synchronizeBuckets(date)
        return monthlyCount >= maxMonthly
    }

    override fun getDailyCallsCount(date: LocalDate): Int {
        synchronizeBuckets(date)
        return dailyCount
    }

    override fun getMonthlyCallsCount(date: LocalDate): Int {
        synchronizeBuckets(date)
        return monthlyCount
    }

    private fun synchronizeBuckets(date: LocalDate) {
        val last = lastRecordedDate ?: return
        if (last.year != date.year || last.month != date.month) {
            monthlyCount = 0
            dailyCount = 0
        } else if (last != date) {
            dailyCount = 0
        }
    }
}
