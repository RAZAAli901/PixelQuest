package com.pixelquest.app.ui.components

import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/** The full day name TalkBack reads for a day chip that shows only "T" or "S". */
fun dayChipName(day: DayOfWeek): String = day.getDisplayName(TextStyle.FULL, Locale.ENGLISH)

/**
 * A weekday chip in a multi-select row: TalkBack reads "Tuesday, checked" rather than a lone "T"
 * that Thursday shares. Hide the chip's own letter from TalkBack (clearAndSetSemantics) so it isn't
 * read as well.
 */
fun Modifier.dayChip(day: DayOfWeek, selected: Boolean, onToggle: () -> Unit): Modifier =
    this
        .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onToggle() })
        .semantics { contentDescription = dayChipName(day) }

/** One option of a single-choice chip row (recurrence, category): TalkBack says which is selected. */
fun Modifier.choiceChip(selected: Boolean, onSelect: () -> Unit): Modifier =
    this.selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
