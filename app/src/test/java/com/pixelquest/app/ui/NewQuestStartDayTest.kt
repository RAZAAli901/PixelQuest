package com.pixelquest.app.ui

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.domain.QuestStart
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import com.pixelquest.app.testing.FakeTaskRepository
import com.pixelquest.app.testing.FixedClock
import com.pixelquest.app.ui.screens.tasks.TaskFormViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * A quest created after its time of day used to start today anyway, so its first occurrence was
 * already past and got recorded as missed. It now starts tomorrow, and the form says so.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class NewQuestStartDayTest {

    private val today = LocalDate.of(2026, 10, 6)
    private val threePm = LocalDateTime.of(today, LocalTime.of(15, 0))

    @Test
    fun theRule() {
        assertEquals(today, QuestStart.firstDay(threePm, LocalTime.of(18, 0)))
        assertEquals(today.plusDays(1), QuestStart.firstDay(threePm, LocalTime.of(9, 0)))
        // A quest due this very minute has already started: tomorrow.
        assertEquals(today.plusDays(1), QuestStart.firstDay(threePm, LocalTime.of(15, 0)))
    }

    private fun form(repository: FakeTaskRepository = FakeTaskRepository()) =
        TaskFormViewModel(repository, TaskAlarmScheduler(ApplicationProvider.getApplicationContext()), FixedClock(threePm))

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    @Test
    fun aNewQuestWhoseTimeHasPassed_isSavedFromTomorrow_andTheFormSaysSo() {
        val repository = FakeTaskRepository()
        val viewModel = form(repository)
        viewModel.onNameChanged("Morning Run")
        viewModel.onTimeSelected(LocalTime.of(9, 0))

        assertTrue(viewModel.formState.value.startsTomorrow)
        viewModel.saveTask {}
        idle()

        assertEquals(today.plusDays(1), repository.tasks.value.single().scheduledDay)
    }

    @Test
    fun aNewQuestLaterToday_startsToday() {
        val repository = FakeTaskRepository()
        val viewModel = form(repository)
        viewModel.onNameChanged("Evening Read")
        viewModel.onTimeSelected(LocalTime.of(21, 0))

        assertFalse(viewModel.formState.value.startsTomorrow)
        viewModel.saveTask {}
        idle()

        assertEquals(today, repository.tasks.value.single().scheduledDay)
    }

    @Test
    fun editingAQuest_keepsItsStartDay() {
        val started = today.minusDays(20)
        val repository = FakeTaskRepository(
            listOf(
                com.pixelquest.app.data.local.entity.TaskEntity(
                    id = 3, name = "Stretch", description = "", scheduledDay = started, scheduledTime = LocalTime.of(8, 0),
                    recurrenceType = com.pixelquest.app.domain.model.RecurrenceType.DAILY,
                    category = com.pixelquest.app.domain.model.TaskCategory.HEALTH
                )
            )
        )
        val viewModel = form(repository)
        viewModel.loadTask(3)
        idle()
        viewModel.onTimeSelected(LocalTime.of(7, 0)) // earlier than 3 pm, but it's an edit

        assertFalse(viewModel.formState.value.startsTomorrow)
        viewModel.saveTask {}
        idle()

        assertEquals(started, repository.tasks.value.single().scheduledDay)
    }
}
