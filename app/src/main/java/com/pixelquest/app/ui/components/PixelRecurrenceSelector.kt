package com.pixelquest.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelquest.app.domain.model.RecurrenceType

/** How often a quest repeats: Daily, Weekly or One-time, one chosen, as v2's choice chips. */
@Composable
fun PixelRecurrenceSelector(
    selectedType: RecurrenceType,
    onTypeSelected: (RecurrenceType) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Repeats"
) {
    val options = listOf(
        RecurrenceType.DAILY to "Daily",
        RecurrenceType.WEEKLY to "Weekly",
        RecurrenceType.ONE_TIME to "One-time"
    )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PqFieldLabel(label)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.selectableGroup()) {
            options.forEach { (type, name) -> PqChoiceChip(name, selectedType == type, { onTypeSelected(type) }) }
        }
    }
}
