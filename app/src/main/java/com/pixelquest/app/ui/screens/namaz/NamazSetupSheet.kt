package com.pixelquest.app.ui.screens.namaz

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MyLocation
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqButtonStyle
import com.pixelquest.app.ui.components.PqFieldLabel
import com.pixelquest.app.ui.components.PqRadio
import com.pixelquest.app.ui.components.PqSheet
import com.pixelquest.app.ui.components.PqSwitch
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.data.local.entity.NamazSettingsEntity
import com.pixelquest.app.domain.namaz.NamazMethod
import com.pixelquest.app.domain.namaz.NamazPlace

/**
 * The first-time setup, shown once when the switch is first turned on: where the times are for,
 * the calculation method, and whether to be reminded at each prayer. Every choice has a default
 * (Lahore, Karachi's method, reminders on), so closing it straight away is fine.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NamazSetupSheet(
    settings: NamazSettingsEntity,
    place: NamazPlace,
    locationLookup: LocationLookup,
    onUseMyLocation: () -> Unit,
    onDone: (method: NamazMethod, remind: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var method by rememberSaveable { mutableStateOf(NamazMethod.fromName(settings.method)) }
    var remind by rememberSaveable { mutableStateOf(settings.remindFajr) }

    PqSheet(onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .testTag(NamazTestTags.SETUP),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(text = stringResource(R.string.namaz_setup_title), style = PqType.CardTitle)
            NamazSheetSection(title = stringResource(R.string.namaz_setup_location)) {
                NamazPlaceLine(place = place, locationLookup = locationLookup)
                PqButton(
                    text = stringResource(R.string.namaz_use_my_location),
                    onClick = onUseMyLocation,
                    style = PqButtonStyle.Secondary,
                    icon = Icons.Rounded.MyLocation,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            NamazSheetSection(title = stringResource(R.string.namaz_setup_method)) {
                Column(modifier = Modifier.selectableGroup()) {
                    NamazMethod.entries.forEach { option ->
                        NamazChoiceRow(
                            label = stringResource(option.displayName),
                            selected = option == method,
                            onClick = { method = option }
                        )
                    }
                }
            }
            NamazToggleRow(
                label = stringResource(R.string.namaz_setup_reminders),
                checked = remind,
                onCheckedChange = { remind = it }
            )
            PqButton(
                text = stringResource(R.string.namaz_setup_done),
                onClick = { onDone(method, remind) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** A titled group in a Namaz sheet. */
@Composable
internal fun NamazSheetSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PqFieldLabel(title)
        content()
    }
}

/** Where the times are for, and how Use my location is going. */
@Composable
internal fun NamazPlaceLine(place: NamazPlace, locationLookup: LocationLookup) {
    val placeName = place.name.ifBlank { stringResource(R.string.namaz_place_here) }
    Text(text = stringResource(R.string.namaz_place_times_for, placeName), style = PqType.BodyStrong)
    when (locationLookup) {
        LocationLookup.LOOKING -> Text(text = stringResource(R.string.namaz_location_finding), style = PqType.Meta)
        LocationLookup.UNAVAILABLE -> Text(text = stringResource(R.string.namaz_location_unavailable, placeName), style = PqType.Meta)
        LocationLookup.IDLE -> Unit
    }
}

/** One choice of several: the whole row selects it, and TalkBack reads it as a radio button. */
@Composable
internal fun NamazChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PqRadio(selected)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, style = PqType.Body)
    }
}

/** An on/off choice in a Namaz sheet: the whole row toggles it. */
@Composable
internal fun NamazToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = PqType.Body, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(12.dp))
        PqSwitch(checked = checked, onCheckedChange = null)
    }
}
