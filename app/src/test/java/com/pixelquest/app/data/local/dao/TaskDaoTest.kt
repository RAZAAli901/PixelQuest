package com.pixelquest.app.data.local.dao

import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class TaskDaoTest : BaseDaoTest() {

    private lateinit var taskDao: TaskDao

    @Before
    override fun createDb() {
        super.createDb()
        taskDao = database.taskDao()
    }

    @Test
    fun insertAndQueryTaskById() = runBlocking {
        val today = LocalDate.now()
        val task = TaskEntity(
            id = 10,
            name = "Morning Quest",
            description = "Complete morning workout",
            scheduledDay = today,
            scheduledTime = LocalTime.of(7, 30),
            recurrenceType = RecurrenceType.DAILY,
            category = TaskCategory.FITNESS
        )

        taskDao.insertTask(task)
        val fetched = taskDao.getTaskById(10).first()

        assertNotNull(fetched)
        assertEquals("Morning Quest", fetched?.name)
        assertEquals(TaskCategory.FITNESS, fetched?.category)
    }

    @Test
    fun queryTasksForDay() = runBlocking {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        val task1 = TaskEntity(
            id = 1,
            name = "Task Today 1",
            description = "Desc",
            scheduledDay = today,
            scheduledTime = LocalTime.of(9, 0),
            recurrenceType = RecurrenceType.DAILY,
            category = TaskCategory.PRODUCTIVITY
        )
        val task2 = TaskEntity(
            id = 2,
            name = "Task Tomorrow",
            description = "Desc",
            scheduledDay = tomorrow,
            scheduledTime = LocalTime.of(10, 0),
            recurrenceType = RecurrenceType.ONE_TIME,
            category = TaskCategory.PRODUCTIVITY
        )

        taskDao.insertTask(task1)
        taskDao.insertTask(task2)

        val todayTasks = taskDao.getTasksForDay(today).first()
        assertEquals(1, todayTasks.size)
        assertEquals("Task Today 1", todayTasks[0].name)
    }

    @Test
    fun deleteTask() = runBlocking {
        val today = LocalDate.now()
        val task = TaskEntity(
            id = 5,
            name = "To be deleted",
            description = "Desc",
            scheduledDay = today,
            scheduledTime = LocalTime.of(12, 0),
            recurrenceType = RecurrenceType.ONE_TIME,
            category = TaskCategory.HEALTH
        )

        taskDao.insertTask(task)
        val inserted = taskDao.getTaskById(5).first()
        assertNotNull(inserted)

        taskDao.deleteTask(task)
        val deleted = taskDao.getTaskById(5).first()
        assertNull(deleted)
    }
}
