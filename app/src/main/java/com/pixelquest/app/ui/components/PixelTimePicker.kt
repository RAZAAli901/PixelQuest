package com.pixelquest.app.ui.components

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqType
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * A quest's time: an inked field showing it (or --:--), which opens Android's own time picker. A
 * problem with it ([errorText]) is said under it.
 */
@Composable
fun PixelTimePicker(
    selectedTime: LocalTime?,
    onTimeSelected: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Time",
    errorText: String? = null
) {
    val context = LocalContext.current
    val formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PqFieldLabel(label)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .comicPressable(
                    {
                        TimePickerDialog(
                            context,
                            { _, hour, minute -> onTimeSelected(LocalTime.of(hour, minute)) },
                            selectedTime?.hour ?: 9,
                            selectedTime?.minute ?: 0,
                            android.text.format.DateFormat.is24HourFormat(context)
                        ).show()
                    },
                    radius = 12.dp,
                    shadow = 2.dp,
                    onClickLabel = label,
                    role = Role.Button
                )
                .padding(horizontal = 14.dp)
        ) {
            Icon(Icons.Rounded.Schedule, contentDescription = null, tint = PqColors.Ink)
            Text(
                text = selectedTime?.format(formatter) ?: "--:--",
                style = PqType.BodyStrong.copy(color = if (selectedTime != null) PqColors.Ink else PqColors.Muted)
            )
        }
        if (errorText != null) Text(text = errorText, style = PqType.Meta.copy(color = PqColors.Error))
    }
}
