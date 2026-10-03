package com.pixelquest.app.domain.repository

import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TaskCompletionRepository {
    /** Returns -1 when the task already has a result for that day; use TaskResultRecorder to record one. */
    suspend fun insertLog(log: TaskCompletionLogEntity): Long
    suspend fun updateLog(log: TaskCompletionLogEntity)
    suspend fun getLogForTaskOnDate(taskId: Long, date: LocalDate): TaskCompletionLogEntity?
    fun getLogsForDate(date: LocalDate): Flow<List<TaskCompletionLogEntity>>
    fun getLogsForTask(taskId: Long): Flow<List<TaskCompletionLogEntity>>
    fun getCompletionHistory(startDate: LocalDate, endDate: LocalDate): Flow<List<TaskCompletionLogEntity>>
    fun getAllLogs(): Flow<List<TaskCompletionLogEntity>>
}
