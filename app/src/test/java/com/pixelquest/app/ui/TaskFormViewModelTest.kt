package com.pixelquest.app.ui

import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.repository.TaskRepository
import com.pixelquest.app.ui.screens.tasks.TaskFormViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class FakeTaskRepository : TaskRepository {
    private val tasks = MutableStateFlow<List<TaskEntity>>(emptyList())

    override fun getAllTasks(): Flow<List<TaskEntity>> = tasks
    override fun getTaskById(id: Long): Flow<TaskEntity?> = tasks.map { list -> list.find { it.id == id } }
    override fun getTasksForDay(day: LocalDate): Flow<List<TaskEntity>> = tasks.map { list -> list.filter { it.scheduledDay == day } }

    override suspend fun insertTask(task: TaskEntity): Long {
        val newId = (tasks.value.maxOfOrNull { it.id } ?: 0L) + 1L
        val newTask = task.copy(id = newId)
        tasks.value = tasks.value + newTask
        return newId
    }

    override suspend fun updateTask(task: TaskEntity) {
        tasks.value = tasks.value.map { if (it.id == task.id) task else it }
    }

    override suspend fun deleteTask(task: TaskEntity) {
        tasks.value = tasks.value.filterNot { it.id == task.id }
    }
}

class TaskFormViewModelTest {

    private lateinit var fakeRepository: FakeTaskRepository
    private lateinit var viewModel: TaskFormViewModel

    @Before
    fun setUp() {
        fakeRepository = FakeTaskRepository()
        viewModel = TaskFormViewModel(fakeRepository)
    }

    @Test
    fun validForm_returnsTrue() {
        viewModel.onNameChanged("Daily Workout")
        viewModel.onTimeSelected(LocalTime.of(8, 0))

        assertTrue(viewModel.formState.value.isValid)
        assertTrue(viewModel.validateForm())
        assertNull(viewModel.formState.value.nameError)
        assertNull(viewModel.formState.value.timeError)
    }

    @Test
    fun missingName_failsValidation() {
        viewModel.onNameChanged("   ")
        viewModel.onTimeSelected(LocalTime.of(8, 0))

        assertFalse(viewModel.formState.value.isValid)
        assertFalse(viewModel.validateForm())
        assertNotNull(viewModel.formState.value.nameError)
        assertEquals("Quest title is required!", viewModel.formState.value.nameError)
    }

    @Test
    fun missingTime_failsValidation() {
        viewModel.onNameChanged("Read Book")
        viewModel.onTimeSelected(null)

        assertFalse(viewModel.formState.value.isValid)
        assertFalse(viewModel.validateForm())
        assertNotNull(viewModel.formState.value.timeError)
        assertEquals("Time is required!", viewModel.formState.value.timeError)
    }

    @Test
    fun noDaysSelectedForWeeklyRecurrence_failsValidation() {
        viewModel.onNameChanged("Weekly Quest")
        viewModel.onTimeSelected(LocalTime.of(10, 0))
        viewModel.onRecurrenceSelected(RecurrenceType.WEEKLY)
        DayOfWeek.values().forEach { day ->
            if (viewModel.formState.value.selectedDays.contains(day)) {
                viewModel.onDayToggled(day)
            }
        }

        assertFalse(viewModel.formState.value.isValid)
        assertFalse(viewModel.validateForm())
        assertNotNull(viewModel.formState.value.daysError)
        assertEquals("Select at least one day!", viewModel.formState.value.daysError)
    }
}
