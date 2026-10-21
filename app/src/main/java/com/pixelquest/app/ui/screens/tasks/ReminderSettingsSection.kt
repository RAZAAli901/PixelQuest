package com.pixelquest.app.ui.screens.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pixelquest.app.domain.model.ReminderStyle
import com.pixelquest.app.ui.components.PqChoiceChip
import com.pixelquest.app.ui.components.PqFieldLabel
import com.pixelquest.app.ui.components.PqSwitch
import com.pixelquest.app.ui.components.comic
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType

/**
 * Per-quest reminder settings on the quest form: on or off (the whole row is the switch), how early,
 * and how loud, as v2's choice chips.
 */
@Composable
fun ReminderSettingsSection(
    reminderEnabled: Boolean,
    leadMinutes: Int,
    style: ReminderStyle,
    onReminderEnabledChanged: (Boolean) -> Unit,
    onLeadSelected: (Int) -> Unit,
    onStyleSelected: (ReminderStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .comic(radius = PqDimens.Radius, border = PqDimens.BorderThin, shadow = 0.dp)
                .toggleable(value = reminderEnabled, role = Role.Switch, onValueChange = onReminderEnabledChanged)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = PqColors.Ink)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Remind me", style = PqType.BodyStrong)
                Text(text = if (reminderEnabled) leadLabel(leadMinutes) else "No reminder", style = PqType.Meta)
            }
            PqSwitch(checked = reminderEnabled, onCheckedChange = null)
        }
        if (reminderEnabled) {
            OptionRow(
                label = "When",
                options = REMINDER_LEAD_OPTIONS,
                selected = leadMinutes,
                optionLabel = ::leadLabel,
                onSelected = onLeadSelected
            )
            OptionRow(
                label = "Alert style",
                options = ReminderStyle.values().toList(),
                selected = style,
                optionLabel = ::styleLabel,
                onSelected = onStyleSelected
            )
            Text(text = styleHint(style), style = PqType.Meta)
        }
    }
}

internal fun leadLabel(minutes: Int): String = when {
    minutes <= 0 -> "At time"
    minutes < 60 -> "$minutes min early"
    else -> "${minutes / 60} hr early"
}

internal fun styleLabel(style: ReminderStyle): String = when (style) {
    ReminderStyle.STANDARD -> "Standard"
    ReminderStyle.SILENT -> "Silent"
    ReminderStyle.PROMPT -> "Full screen"
}

internal fun styleHint(style: ReminderStyle): String = when (style) {
    ReminderStyle.STANDARD -> "A normal notification, with sound if reminder sound is on."
    ReminderStyle.SILENT -> "Appears in your notifications without sound or vibration."
    ReminderStyle.PROMPT -> "Opens the check-in screen, even on the lock screen where Android allows it."
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> OptionRow(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PqFieldLabel(label)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.selectableGroup()
        ) {
            options.forEach { option -> PqChoiceChip(optionLabel(option), option == selected, { onSelected(option) }) }
        }
    }
}
