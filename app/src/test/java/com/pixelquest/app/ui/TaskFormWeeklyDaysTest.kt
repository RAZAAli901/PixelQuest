package com.pixelquest.app.ui

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.backup.BackupPayload
import com.pixelquest.app.data.backup.DataExportImport
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import com.pixelquest.app.ui.screens.tasks.TaskFormViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * The days picked for a weekly quest are saved, shown again when the quest is edited, and kept in
 * backups. Before Day 27 the picker's choice was dropped on save.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class TaskFormWeeklyDaysTest {

    private val repository = FakeTaskRepository()
    private fun newViewModel() = TaskFormViewModel(repository, TaskAlarmScheduler(ApplicationProvider.getApplicationContext()))
    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    @Test
    fun newWeeklyQuest_startsOnTodaysWeekday() {
        assertEquals(setOf(LocalDate.now().dayOfWeek), newViewModel().formState.value.selectedDays)
    }

    @Test
    fun savedWeeklyQuest_keepsTheChosenDays_andEditShowsThem() = runBlocking {
        val form = newViewModel()
        form.onNameChanged("Swim")
        form.onTimeSelected(LocalTime.of(7, 0))
        form.onRecurrenceSelected(RecurrenceType.WEEKLY)
        // Pick exactly Tuesday and Thursday.
        DayOfWeek.values().forEach { day ->
            val wanted = day == DayOfWeek.TUESDAY || day == DayOfWeek.THURSDAY
            if (form.formState.value.selectedDays.contains(day) != wanted) form.onDayToggled(day)
        }
        form.saveTask {}
        idle()

        val saved = repository.getAllTasks().first().single()
        assertEquals(setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), saved.weeklyDays)

        val edit = newViewModel()
        edit.loadTask(saved.id)
        idle()
        assertEquals(setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), edit.formState.value.selectedDays)
    }

    @Test
    fun dailyQuest_savesNoWeeklyDays() = runBlocking {
        val form = newViewModel()
        form.onNameChanged("Push-ups")
        form.onTimeSelected(LocalTime.of(8, 0))
        form.saveTask {}
        idle()

        assertEquals(emptySet<DayOfWeek>(), repository.getAllTasks().first().single().weeklyDays)
    }

    @Test
    fun editingAnOlderWeeklyQuest_showsItsFirstDatesWeekday() = runBlocking {
        val id = repository.insertTask(
            TaskEntity(
                name = "Old weekly", description = "", scheduledDay = LocalDate.of(2026, 9, 7), // Monday
                scheduledTime = LocalTime.of(8, 0), recurrenceType = RecurrenceType.WEEKLY, category = TaskCategory.OTHER
            )
        )
        val edit = newViewModel()
        edit.loadTask(id)
        idle()

        assertEquals(setOf(DayOfWeek.MONDAY), edit.formState.value.selectedDays)
    }

    @Test
    fun backup_roundTripsWeeklyDays_andOldBackupsStillLoad() {
        val task = TaskEntity(
            id = 1, name = "Swim", description = "", scheduledDay = LocalDate.of(2026, 9, 7),
            scheduledTime = LocalTime.of(7, 0), recurrenceType = RecurrenceType.WEEKLY, category = TaskCategory.FITNESS,
            weeklyDays = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY)
        )
        val json = DataExportImport.exportToJson(BackupPayload(null, null, null, listOf(task)))
        assertEquals(task.weeklyDays, DataExportImport.importFromJson(json).tasks.single().weeklyDays)

        // weeklyDays is the last field of each task, so drop it together with the comma before it.
        val oldBackup = json.replace(Regex(",\\s*\"weeklyDays\"\\s*:\\s*\\[[^\\]]*\\]"), "")
        assertEquals(emptySet<DayOfWeek>(), DataExportImport.importFromJson(oldBackup).tasks.single().weeklyDays)
    }
}
