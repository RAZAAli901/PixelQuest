package com.pixelquest.app.ui.screens.namaz

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqButtonStyle
import com.pixelquest.app.ui.components.PqSheet
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.data.local.entity.NamazSettingsEntity
import com.pixelquest.app.domain.namaz.AsrMadhab
import com.pixelquest.app.domain.namaz.NamazMethod
import com.pixelquest.app.domain.namaz.NamazReminderPlanner
import com.pixelquest.app.domain.namaz.NamazSound
import com.pixelquest.app.domain.namaz.remindsFor
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.domain.namaz.adjustmentFor

/**
 * The tracker's settings, from the top bar: the calculation method, when Asr starts, and a
 * minute adjustment per prayer. Each change is saved at once and the screen's times follow it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NamazSettingsSheet(
    settings: NamazSettingsEntity,
    onMethod: (NamazMethod) -> Unit,
    onMadhab: (AsrMadhab) -> Unit,
    onAdjust: (prayer: Prayer, minutes: Int) -> Unit,
    onDismiss: () -> Unit,
    onReminderOffset: (Int) -> Unit = {},
    nudge: Boolean = false,
    onNudge: (Boolean) -> Unit = {},
    onReminder: (Prayer, Boolean) -> Unit = { _, _ -> },
    sound: NamazSound = NamazSound.SYSTEM,
    onSound: (NamazSound) -> Unit = {},
    fajrThroughDnd: Boolean = false,
    onFajrThroughDnd: (Boolean) -> Unit = {},
    onTestReminder: (() -> Unit)? = null,
    onXpEnabled: (Boolean) -> Unit = {},
    isSimpleMode: Boolean = false
) {
    val method = NamazMethod.fromName(settings.method)
    val madhab = AsrMadhab.fromName(settings.asrMadhab)

    PqSheet(onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .testTag(NamazTestTags.SETTINGS),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(text = stringResource(R.string.namaz_settings_title), style = PqType.CardTitle)
            NamazSheetSection(title = stringResource(R.string.namaz_setup_method)) {
                Column(modifier = Modifier.selectableGroup()) {
                    NamazMethod.entries.forEach { option ->
                        NamazChoiceRow(label = stringResource(option.displayName), selected = option == method, onClick = { onMethod(option) })
                    }
                }
            }
            NamazSheetSection(title = stringResource(R.string.namaz_settings_madhab)) {
                Column(modifier = Modifier.selectableGroup()) {
                    AsrMadhab.entries.forEach { option ->
                        NamazChoiceRow(label = stringResource(option.displayName), selected = option == madhab, onClick = { onMadhab(option) })
                    }
                }
            }
            // Simple Mode shows no XP, so it has no XP switch either.
            if (!isSimpleMode) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    NamazToggleRow(
                        label = stringResource(R.string.namaz_settings_xp),
                        checked = settings.xpEnabled,
                        onCheckedChange = onXpEnabled
                    )
                    Text(text = stringResource(R.string.namaz_settings_xp_hint), style = PqType.Meta)
                }
            }
            NamazSheetSection(title = stringResource(R.string.namaz_settings_reminders)) {
                Prayer.entries.forEach { prayer ->
                    NamazToggleRow(
                        label = stringResource(prayer.displayName),
                        checked = settings.remindsFor(prayer),
                        onCheckedChange = { onReminder(prayer, it) }
                    )
                }
            }
            NamazSheetSection(title = stringResource(R.string.namaz_settings_sound)) {
                Column(modifier = Modifier.selectableGroup()) {
                    NamazSound.entries.forEach { option ->
                        NamazChoiceRow(
                            label = stringResource(
                                when (option) {
                                    NamazSound.SYSTEM -> R.string.namaz_sound_system
                                    NamazSound.CHIME -> R.string.namaz_sound_chime
                                    NamazSound.SILENT -> R.string.namaz_sound_silent
                                }
                            ),
                            selected = option == sound,
                            onClick = { onSound(option) }
                        )
                    }
                }
                // Silent reminders make no sound to let through.
                if (sound != NamazSound.SILENT) {
                    NamazToggleRow(
                        label = stringResource(R.string.namaz_settings_fajr_dnd),
                        checked = fajrThroughDnd,
                        onCheckedChange = onFajrThroughDnd
                    )
                    Text(text = stringResource(R.string.namaz_settings_fajr_dnd_hint), style = PqType.Meta)
                }
            }
            // Debug builds only: a real reminder in 10 seconds, to try the whole path on a phone.
            onTestReminder?.let { test ->
                PqButton(
                    text = stringResource(R.string.namaz_settings_test_reminder),
                    onClick = test,
                    style = PqButtonStyle.Secondary,
                    icon = Icons.Rounded.NotificationsActive,
                    modifier = Modifier.fillMaxWidth().testTag(NamazTestTags.TEST_REMINDER)
                )
            }
            NamazSheetSection(title = stringResource(R.string.namaz_settings_reminder_time)) {
                Column(modifier = Modifier.selectableGroup()) {
                    NamazReminderPlanner.OFFSETS.forEach { minutes ->
                        NamazChoiceRow(
                            label = if (minutes == 0) {
                                stringResource(R.string.namaz_offset_at_start)
                            } else {
                                pluralStringResource(R.plurals.namaz_offset_before, minutes, minutes)
                            },
                            selected = settings.reminderOffsetMin == minutes,
                            onClick = { onReminderOffset(minutes) }
                        )
                    }
                }
                NamazToggleRow(label = stringResource(R.string.namaz_settings_nudge), checked = nudge, onCheckedChange = onNudge)
            }
            NamazSheetSection(title = stringResource(R.string.namaz_settings_adjust)) {
                Text(text = stringResource(R.string.namaz_settings_adjust_hint), style = PqType.Meta)
                Prayer.entries.forEach { prayer ->
                    AdjustRow(prayer = prayer, minutes = settings.adjustmentFor(prayer), onAdjust = { onAdjust(prayer, it) })
                }
            }
        }
    }
}

/** One prayer's adjustment: its name, a minute earlier, the minutes now, a minute later. */
@Composable
private fun AdjustRow(prayer: Prayer, minutes: Int, onAdjust: (Int) -> Unit) {
    val name = stringResource(prayer.displayName)
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(NamazTestTags.adjust(prayer)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = name, style = PqType.BodyStrong, modifier = Modifier.weight(1f))
        IconButton(
            onClick = { onAdjust(-1) },
            enabled = minutes > -NamazViewModel.MAX_ADJUST_MINUTES
        ) {
            Icon(Icons.Rounded.Remove, contentDescription = stringResource(R.string.namaz_adjust_earlier, name), tint = PqColors.Ink)
        }
        Text(
            text = stringResource(R.string.namaz_adjust_minutes, minutes),
            style = PqType.Body,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 64.dp)
        )
        IconButton(
            onClick = { onAdjust(1) },
            enabled = minutes < NamazViewModel.MAX_ADJUST_MINUTES
        ) {
            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.namaz_adjust_later, name), tint = PqColors.Ink)
        }
    }
}
