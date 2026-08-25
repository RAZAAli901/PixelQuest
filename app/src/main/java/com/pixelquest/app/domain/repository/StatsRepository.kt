package com.pixelquest.app.domain.repository

import com.pixelquest.app.domain.model.DailyStatus
import com.pixelquest.app.domain.model.PerTaskStats
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface StatsRepository {
    fun getCompletionRateOverRange(startDate: LocalDate, endDate: LocalDate): Flow<Float>
    fun getDailyStatusForRange(startDate: LocalDate, endDate: LocalDate): Flow<Map<LocalDate, DailyStatus>>
    fun getPerTaskStats(taskId: Long): Flow<PerTaskStats>
}
