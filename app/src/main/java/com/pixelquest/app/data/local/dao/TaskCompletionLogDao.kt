package com.pixelquest.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface TaskCompletionLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: TaskCompletionLogEntity): Long

    @Query("SELECT * FROM task_completion_logs WHERE completedDate = :date")
    fun getLogsForDate(date: LocalDate): Flow<List<TaskCompletionLogEntity>>

    @Query("SELECT * FROM task_completion_logs WHERE taskId = :taskId ORDER BY completedDate DESC")
    fun getLogsForTask(taskId: Long): Flow<List<TaskCompletionLogEntity>>

    @Query("SELECT * FROM task_completion_logs WHERE completedDate BETWEEN :startDate AND :endDate ORDER BY completedDate ASC")
    fun getCompletionHistory(startDate: LocalDate, endDate: LocalDate): Flow<List<TaskCompletionLogEntity>>
}
