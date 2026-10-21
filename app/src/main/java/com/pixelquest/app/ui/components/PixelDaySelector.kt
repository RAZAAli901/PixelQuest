package com.pixelquest.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.PqCircle
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import java.time.DayOfWeek

/**
 * A weekly quest's days, Monday to Sunday: a circle a day, filled with ink when chosen. TalkBack hears
 * each as a checkbox named with the whole day ([dayChip]), never the single letter.
 */
@Composable
fun PixelDaySelector(
    selectedDays: Set<DayOfWeek>,
    onDayToggled: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Days",
    errorText: String? = null
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PqFieldLabel(label)
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            DayOfWeek.entries.forEach { day ->
                val on = day in selectedDays
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(PqDimens.TouchMin)
                        .clip(PqCircle)
                        .dayChip(day, on) { onDayToggled(day) }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(PqCircle)
                            .background(if (on) PqColors.Ink else PqColors.Paper)
                            .border(PqDimens.BorderThin, PqColors.Ink, PqCircle)
                    ) {
                        Text(
                            text = day.name.take(1),
                            style = PqType.Button.copy(color = if (on) PqColors.Paper else PqColors.Ink),
                            modifier = Modifier.clearAndSetSemantics { }
                        )
                    }
                }
            }
        }
        if (errorText != null) Text(text = errorText, style = PqType.Meta.copy(color = PqColors.Error))
    }
}
