package com.pixelquest.app.ui

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import com.pixelquest.app.testing.FakeTaskRepository
import com.pixelquest.app.ui.screens.tasks.TaskFormViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

/**
 * Rotating the device recreates the screen but keeps its ViewModel. The edit screen asks it to load
 * the task again then, which used to copy the stored task back over the player's unsaved edits.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class TaskFormRotationTest {

    private val stored = TaskEntity(
        id = 7, name = "Morning Run", description = "", scheduledDay = LocalDate.of(2026, 9, 1),
        scheduledTime = LocalTime.of(7, 0), recurrenceType = RecurrenceType.DAILY, category = TaskCategory.FITNESS
    )
    private val repository = FakeTaskRepository(listOf(stored))
    private val viewModel = TaskFormViewModel(repository, TaskAlarmScheduler(ApplicationProvider.getApplicationContext()))

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    @Test
    fun loadingAgain_afterRotation_keepsUnsavedEdits() {
        viewModel.loadTask(7)
        idle()
        assertEquals("Morning Run", viewModel.formState.value.name)
        assertTrue(viewModel.formState.value.isEditMode)

        viewModel.onNameChanged("Evening Run")
        viewModel.onTimeSelected(LocalTime.of(19, 30))
        viewModel.loadTask(7) // the screen's LaunchedEffect runs again after rotation
        idle()

        assertEquals("Evening Run", viewModel.formState.value.name)
        assertEquals(LocalTime.of(19, 30), viewModel.formState.value.scheduledTime)
    }

    @Test
    fun aLaterChangeToTheStoredTask_doesNotOverwriteEdits() {
        viewModel.loadTask(7)
        idle()
        viewModel.onNameChanged("Evening Run")

        // E.g. a reminder or completion rewrites the row while the form is open.
        runBlocking { repository.updateTask(stored.copy(name = "Morning Run (updated)")) }
        idle()

        assertEquals("Evening Run", viewModel.formState.value.name)
    }
}
