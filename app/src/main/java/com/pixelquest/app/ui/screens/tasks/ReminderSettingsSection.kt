package com.pixelquest.app.ui.screens.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pixelquest.app.domain.model.ReminderStyle
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelButtonVariant
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.theme.PixelTheme

/**
 * Per-task reminder settings on the Create/Edit task form: on/off, how early, and how loud.
 * Option chips are PixelCards, so they pick up the Pixel, Light or Comic treatment automatically.
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
        PixelButton(
            text = if (reminderEnabled) "🔔 REMINDER: ON" else "🔕 REMINDER: OFF",
            onClick = { onReminderEnabledChanged(!reminderEnabled) },
            variant = PixelButtonVariant.BLUE,
            modifier = Modifier.fillMaxWidth()
        )
        if (reminderEnabled) {
            OptionRow(
                label = "REMIND ME",
                options = REMINDER_LEAD_OPTIONS,
                selected = leadMinutes,
                optionLabel = ::leadLabel,
                onSelected = onLeadSelected
            )
            OptionRow(
                label = "ALERT STYLE",
                options = ReminderStyle.values().toList(),
                selected = style,
                optionLabel = ::styleLabel,
                onSelected = onStyleSelected
            )
            Text(
                text = styleHint(style),
                style = MaterialTheme.typography.bodySmall,
                color = PixelTheme.colors.onSurfaceVariant
            )
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
    val colors = PixelTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = colors.primaryText)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                val isSelected = option == selected
                PixelCard(
                    variant = if (isSelected) PixelPanelVariant.BLUE else PixelPanelVariant.BORDER,
                    contentPadding = 8.dp,
                    modifier = Modifier
                        .widthIn(min = 88.dp)
                        .selectable(selected = isSelected, role = Role.RadioButton) { onSelected(option) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = optionLabel(option),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) colors.onSurface else colors.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
