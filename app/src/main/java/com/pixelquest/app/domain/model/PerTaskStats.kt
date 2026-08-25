package com.pixelquest.app.domain.model

import java.time.LocalDate

data class PerTaskStats(
    val taskId: Long,
    val completionCount: Int,
    val totalScheduledCount: Int,
    val completionRate: Float,
    val currentStreak: Int,
    val longestStreak: Int,
    val recentHistory: List<Pair<LocalDate, Boolean>> = emptyList()
)
