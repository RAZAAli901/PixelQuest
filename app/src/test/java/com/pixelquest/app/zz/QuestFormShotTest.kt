package com.pixelquest.app.zz

import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.ui.screens.tasks.TaskFormContent
import com.pixelquest.app.ui.screens.tasks.TaskFormState
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.PqSkin
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

class QuestFormShotTest : ScratchShot() {
    private val state = TaskFormState(
        name = "Read 15 pages", isEditMode = true, category = TaskCategory.LEARNING, recurrenceType = RecurrenceType.WEEKLY,
        selectedDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), scheduledTime = LocalTime.of(21, 0)
    )

    @Test fun comic() = shot("questform", PqSkin.Comic) { PixelQuestTheme { TaskFormContent(state) } }
    @Test fun pixel() = shot("questform", PqSkin.Pixel) { PixelQuestTheme { TaskFormContent(state) } }
}
