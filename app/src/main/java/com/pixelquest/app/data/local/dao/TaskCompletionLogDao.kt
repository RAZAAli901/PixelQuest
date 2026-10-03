package com.pixelquest.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface TaskCompletionLogDao {
    /** Returns -1 when the task already has a result for that day (see TaskResultRecorder). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLog(log: TaskCompletionLogEntity): Long

    @Update
    suspend fun updateLog(log: TaskCompletionLogEntity)

    @Query("DELETE FROM task_completion_logs")
    suspend fun deleteAll()

    @Query("SELECT * FROM task_completion_logs WHERE taskId = :taskId AND completedDate = :date LIMIT 1")
    suspend fun getLogForTaskOnDate(taskId: Long, date: LocalDate): TaskCompletionLogEntity?

    @Query("SELECT * FROM task_completion_logs WHERE completedDate = :date")
    fun getLogsForDate(date: LocalDate): Flow<List<TaskCompletionLogEntity>>

    @Query("SELECT * FROM task_completion_logs WHERE taskId = :taskId ORDER BY completedDate DESC")
    fun getLogsForTask(taskId: Long): Flow<List<TaskCompletionLogEntity>>

    @Query("SELECT * FROM task_completion_logs WHERE completedDate BETWEEN :startDate AND :endDate ORDER BY completedDate ASC")
    fun getCompletionHistory(startDate: LocalDate, endDate: LocalDate): Flow<List<TaskCompletionLogEntity>>

    @Query("SELECT * FROM task_completion_logs ORDER BY completedDate ASC")
    fun getAllLogs(): Flow<List<TaskCompletionLogEntity>>
}
