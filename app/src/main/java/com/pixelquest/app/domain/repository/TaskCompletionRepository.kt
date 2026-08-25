package com.pixelquest.app.domain.repository

import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TaskCompletionRepository {
    suspend fun insertLog(log: TaskCompletionLogEntity): Long
    fun getLogsForDate(date: LocalDate): Flow<List<TaskCompletionLogEntity>>
    fun getLogsForTask(taskId: Long): Flow<List<TaskCompletionLogEntity>>
    fun getCompletionHistory(startDate: LocalDate, endDate: LocalDate): Flow<List<TaskCompletionLogEntity>>
    fun getAllLogs(): Flow<List<TaskCompletionLogEntity>>
}
