package com.pixelquest.app.domain

import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity

object StreakCalculator {

    /**
     * Calculates the completion percentage for a given date.
     * If totalTaskCount is 0, returns 1.0f (100%) to handle rest days / days with no scheduled tasks.
     */
    fun calculateCompletionPercentage(completedTaskCount: Int, totalTaskCount: Int): Float {
        if (totalTaskCount <= 0) return 1.0f
        return (completedTaskCount.toFloat() / totalTaskCount.toFloat()).coerceIn(0.0f, 1.0f)
    }

    /**
     * Calculates completion percentage from a list of completion logs for the day and total scheduled tasks.
     */
    fun calculateCompletionPercentage(logs: List<TaskCompletionLogEntity>, totalTaskCount: Int): Float {
        val completedCount = logs.count { it.wasCompleted }
        return calculateCompletionPercentage(completedCount, totalTaskCount)
    }

    /**
     * Determines whether a date counts as a "perfect day" by comparing completion percentage against a threshold.
     */
    fun isPerfectDay(completionPercentage: Float, threshold: Float): Boolean {
        return completionPercentage >= threshold
    }

    fun isPerfectDay(completedTaskCount: Int, totalTaskCount: Int, threshold: Float): Boolean {
        val pct = calculateCompletionPercentage(completedTaskCount, totalTaskCount)
        return isPerfectDay(pct, threshold)
    }

    fun isPerfectDay(logs: List<TaskCompletionLogEntity>, totalTaskCount: Int, threshold: Float): Boolean {
        val pct = calculateCompletionPercentage(logs, totalTaskCount)
        return isPerfectDay(pct, threshold)
    }
}

