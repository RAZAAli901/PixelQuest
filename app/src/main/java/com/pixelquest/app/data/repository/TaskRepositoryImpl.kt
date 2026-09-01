package com.pixelquest.app.data.repository

import com.pixelquest.app.data.local.dao.TaskDao
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao
) : TaskRepository {
    override fun getAllTasks(): Flow<List<TaskEntity>> = taskDao.getAllTasks()
    override fun getTaskById(id: Long): Flow<TaskEntity?> = taskDao.getTaskById(id)
    override fun getTasksForDay(day: LocalDate): Flow<List<TaskEntity>> = taskDao.getTasksForDay(day)
    override suspend fun insertTask(task: TaskEntity): Long = com.pixelquest.app.util.safeDatabaseCall(-1L) { taskDao.insertTask(task) }
    override suspend fun updateTask(task: TaskEntity) = com.pixelquest.app.util.safeDatabaseCall(Unit) { taskDao.updateTask(task) }
    override suspend fun deleteTask(task: TaskEntity) = com.pixelquest.app.util.safeDatabaseCall(Unit) { taskDao.deleteTask(task) }
}

