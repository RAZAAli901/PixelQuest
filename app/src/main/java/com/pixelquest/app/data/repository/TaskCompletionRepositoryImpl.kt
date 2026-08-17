package com.pixelquest.app.data.repository

import com.pixelquest.app.data.local.dao.TaskCompletionLogDao
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

class TaskCompletionRepositoryImpl @Inject constructor(
    private val taskCompletionLogDao: TaskCompletionLogDao
) : TaskCompletionRepository {
    override suspend fun insertLog(log: TaskCompletionLogEntity): Long = taskCompletionLogDao.insertLog(log)
    override fun getLogsForDate(date: LocalDate): Flow<List<TaskCompletionLogEntity>> = taskCompletionLogDao.getLogsForDate(date)
    override fun getLogsForTask(taskId: Long): Flow<List<TaskCompletionLogEntity>> = taskCompletionLogDao.getLogsForTask(taskId)
    override fun getCompletionHistory(startDate: LocalDate, endDate: LocalDate): Flow<List<TaskCompletionLogEntity>> =
        taskCompletionLogDao.getCompletionHistory(startDate, endDate)
}
