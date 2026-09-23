package com.pixelquest.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.PixelTypography
import com.pixelquest.app.ui.theme.ThemeMode
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Step 27: Compose Preview comparing form inputs across all three themes:
 * Pixel (Retro 8-bit dark), Light (Daylight amber), and Comic (Paper/Ink Vector with Bangers & drop shadow).
 */
@Composable
private fun FormComponentsShowcaseColumn(themeTitle: String) {
    var textValue by remember { mutableStateOf("Slay the Procrastination Beast") }
    var selectedDays by remember { mutableStateOf(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)) }
    var selectedTime by remember { mutableStateOf<LocalTime?>(LocalTime.of(8, 30)) }
    var recurrence by remember { mutableStateOf(RecurrenceType.DAILY) }
    var category by remember { mutableStateOf(TaskCategory.FITNESS) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PixelTheme.colors.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = themeTitle,
            style = PixelTypography.titleMedium,
            color = PixelTheme.colors.primary,
            fontSize = 12.sp
        )

        // 1. Text Field
        PixelTextField(
            value = textValue,
            onValueChange = { textValue = it },
            label = "QUEST NAME",
            placeholder = "Enter quest description..."
        )

        // 2. Day Selector
        PixelDaySelector(
            selectedDays = selectedDays,
            onDayToggled = { day ->
                selectedDays = if (selectedDays.contains(day)) selectedDays - day else selectedDays + day
            }
        )

        // 3. Time Picker
        PixelTimePicker(
            selectedTime = selectedTime,
            onTimeSelected = { selectedTime = it }
        )

        // 4. Recurrence Selector
        PixelRecurrenceSelector(
            selectedType = recurrence,
            onTypeSelected = { recurrence = it }
        )

        // 5. Category Selector
        PixelCategorySelector(
            selectedCategory = category,
            onCategorySelected = { category = it }
        )
    }
}

@Preview(name = "Form 3-Theme Comparison Preview", widthDp = 1200, heightDp = 800)
@Composable
fun ThemeFormComponentsComparisonPreview() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                FormComponentsShowcaseColumn(themeTitle = "PIXEL (DARK)")
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            PixelQuestTheme(themeMode = ThemeMode.Light) {
                FormComponentsShowcaseColumn(themeTitle = "LIGHT (DAYLIGHT)")
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            PixelQuestTheme(themeMode = ThemeMode.Comic) {
                FormComponentsShowcaseColumn(themeTitle = "COMIC (INK/VECTOR)")
            }
        }
    }
}
