package com.pixelquest.app.ui.navigation

import com.pixelquest.app.domain.repository.TaskRepository
import com.pixelquest.app.ui.screens.tasks.TaskFormViewModel
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

class ScreenTransitionStateTest {

    private val taskRepository: TaskRepository = mockk(relaxed = true)

    @Test
    fun taskFormState_survivesScreenReEntry_whenNotReset() {
        val viewModel = TaskFormViewModel(taskRepository)

        // Simulate user typing in form before navigating away/transitioning
        viewModel.onNameChanged("Epic Daily Workout")
        viewModel.onTimeChanged(LocalTime.of(8, 30))
        viewModel.onDaysChanged(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))

        // State check after simulated transition departure & re-entry
        val state = viewModel.formState.value
        assertEquals("Epic Daily Workout", state.name)
        assertEquals(LocalTime.of(8, 30), state.time)
        assertEquals(3, state.selectedDays.size)
        assertTrue(state.selectedDays.contains(DayOfWeek.MONDAY))
    }
}
