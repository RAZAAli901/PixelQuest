package com.pixelquest.app.domain.repository

import com.pixelquest.app.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TaskRepository {
    fun getAllTasks(): Flow<List<TaskEntity>>
    fun getTaskById(id: Long): Flow<TaskEntity?>
    fun getTasksForDay(day: LocalDate): Flow<List<TaskEntity>>
    suspend fun insertTask(task: TaskEntity): Long
    suspend fun updateTask(task: TaskEntity)
    suspend fun deleteTask(task: TaskEntity)
}
