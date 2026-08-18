package com.pixelquest.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.ui.theme.PixelBackgroundDark
import com.pixelquest.app.ui.theme.PixelQuestTheme
import java.time.DayOfWeek
import java.time.LocalTime

@Preview(showBackground = true)
@Composable
fun PixelFormComponentsPreview() {
    var textValue by remember { mutableStateOf("Complete 20 Pushups") }
    var selectedDays by remember { mutableStateOf(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)) }
    var selectedTime by remember { mutableStateOf<LocalTime?>(LocalTime.of(8, 30)) }
    var recurrence by remember { mutableStateOf(RecurrenceType.DAILY) }
    var category by remember { mutableStateOf(TaskCategory.FITNESS) }

    PixelQuestTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PixelBackgroundDark)
                .padding(16.dp)
        ) {
            PixelTextField(
                value = textValue,
                onValueChange = { textValue = it },
                label = "QUEST NAME",
                placeholder = "Enter quest description..."
            )
            Spacer(modifier = Modifier.height(16.dp))

            PixelDaySelector(
                selectedDays = selectedDays,
                onDayToggled = { day ->
                    selectedDays = if (selectedDays.contains(day)) {
                        selectedDays - day
                    } else {
                        selectedDays + day
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))

            PixelTimePicker(
                selectedTime = selectedTime,
                onTimeSelected = { selectedTime = it }
            )
            Spacer(modifier = Modifier.height(16.dp))

            PixelRecurrenceSelector(
                selectedType = recurrence,
                onTypeSelected = { recurrence = it }
            )
            Spacer(modifier = Modifier.height(16.dp))

            PixelCategorySelector(
                selectedCategory = category,
                onCategorySelected = { category = it }
            )
        }
    }
}
