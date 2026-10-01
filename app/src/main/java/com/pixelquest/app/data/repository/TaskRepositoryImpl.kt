package com.pixelquest.app.data.repository

import com.pixelquest.app.data.local.dao.TaskDao
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.TaskOccurrence
import com.pixelquest.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao
) : TaskRepository {
    override fun getAllTasks(): Flow<List<TaskEntity>> = taskDao.getAllTasks()
    override fun getTaskById(id: Long): Flow<TaskEntity?> = taskDao.getTaskById(id)
    // Recurring tasks keep their first date, so match every task that is due on [day].
    override fun getTasksForDay(day: LocalDate): Flow<List<TaskEntity>> =
        taskDao.getActiveTasksStartedBy(day).map { tasks ->
            tasks.filter { TaskOccurrence.occursOn(it.scheduledDay, it.recurrenceType, day) }
        }
    override suspend fun insertTask(task: TaskEntity): Long = com.pixelquest.app.util.safeDatabaseCall(-1L) { taskDao.insertTask(task) }
    override suspend fun updateTask(task: TaskEntity) = com.pixelquest.app.util.safeDatabaseCall(Unit) { taskDao.updateTask(task) }
    override suspend fun deleteTask(task: TaskEntity) = com.pixelquest.app.util.safeDatabaseCall(Unit) { taskDao.deleteTask(task) }
}

