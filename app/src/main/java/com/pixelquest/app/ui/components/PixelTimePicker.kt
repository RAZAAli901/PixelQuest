package com.pixelquest.app.ui.components

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelRed
import com.pixelquest.app.ui.theme.PixelTextMuted
import com.pixelquest.app.ui.theme.PixelTextWhite
import com.pixelquest.app.ui.theme.PixelTypography
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun PixelTimePicker(
    selectedTime: LocalTime?,
    onTimeSelected: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "SET TIME",
    errorText: String? = null
) {
    val context = LocalContext.current
    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

    val initialHour = selectedTime?.hour ?: 9
    val initialMinute = selectedTime?.minute ?: 0

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            onTimeSelected(LocalTime.of(hourOfDay, minute))
        },
        initialHour,
        initialMinute,
        false
    )

    Column(modifier = modifier) {
        Text(
            text = label,
            style = PixelTypography.labelLarge,
            color = PixelGold,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        PixelCard(
            variant = PixelPanelVariant.BORDER,
            contentPadding = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { timePickerDialog.show() }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "⏰",
                    style = PixelTypography.bodyLarge
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = selectedTime?.format(timeFormatter) ?: "--:--",
                    style = PixelTypography.bodyMedium,
                    color = if (selectedTime != null) PixelTextWhite else PixelTextMuted
                )
            }
        }
        if (errorText != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorText,
                style = PixelTypography.bodySmall,
                color = PixelRed,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
