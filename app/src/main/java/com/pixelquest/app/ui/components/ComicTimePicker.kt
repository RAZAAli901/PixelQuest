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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.ui.theme.BangersFontFamily
import com.pixelquest.app.ui.theme.ComicShapeTokens
import com.pixelquest.app.ui.theme.ComicTokens
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Step 24: ComicTimePicker matching PixelTimePicker's role and behavior.
 * Uses the same underlying platform TimePickerDialog with a comic-styled shell
 * featuring Bangers label, ComicPanel input trigger, and ink borders.
 */
@Composable
fun ComicTimePicker(
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

    val hasError = errorText != null
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            fontFamily = BangersFontFamily,
            fontSize = 15.sp,
            letterSpacing = 0.5.sp,
            color = ComicTokens.SolidBlack,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        ComicPanel(
            variant = ComicPanelVariant.SURFACE,
            borderColor = if (hasError) ComicTokens.CoralRed else ComicTokens.SolidBlack,
            borderWidth = if (hasError) ComicShapeTokens.BorderWidthThick else ComicShapeTokens.BorderWidthDefault,
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
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = selectedTime?.format(timeFormatter) ?: "--:--",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedTime != null) ComicTokens.SolidBlack else Color(0xFF757575)
                )
            }
        }

        if (errorText != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorText,
                fontFamily = FontFamily.SansSerif,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ComicTokens.CoralRed,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
