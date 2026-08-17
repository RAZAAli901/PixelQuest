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
}
