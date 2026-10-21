package com.pixelquest.app.ui.screens.namaz

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.MyLocation
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqButtonStyle
import com.pixelquest.app.ui.components.PqCard
import com.pixelquest.app.ui.components.PqSheet
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import com.pixelquest.app.domain.namaz.NamazCities
import com.pixelquest.app.domain.namaz.NamazPlace
import com.pixelquest.app.ui.components.PixelTextField

/**
 * Where the times are for, with the two ways to change it: the phone's rough location (after the
 * location explanation), or a city picked from the list.
 */
@Composable
fun NamazLocationCard(
    place: NamazPlace,
    locationLookup: LocationLookup,
    onUseMyLocation: () -> Unit,
    onPickCity: () -> Unit,
    modifier: Modifier = Modifier
) {
    PqCard(modifier.fillMaxWidth().testTag(NamazTestTags.LOCATION)) {
        // One button above the other: side by side, the labels wrapped onto two lines.
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            NamazPlaceLine(place = place, locationLookup = locationLookup)
            PqButton(
                text = stringResource(R.string.namaz_use_my_location),
                onClick = onUseMyLocation,
                style = PqButtonStyle.Secondary,
                icon = Icons.Rounded.MyLocation,
                modifier = Modifier.fillMaxWidth()
            )
            PqButton(
                text = stringResource(R.string.namaz_pick_city),
                onClick = onPickCity,
                style = PqButtonStyle.Secondary,
                icon = Icons.Rounded.LocationCity,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Enter a city: type to narrow the built-in list (accents and case don't matter), then pick one.
 * The list is the cities the app knows; anywhere else, Use my location gives the exact place.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NamazCityPicker(
    onPick: (NamazPlace) -> Unit,
    onDismiss: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val cities = NamazCities.search(query)

    PqSheet(onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .testTag(NamazTestTags.CITY_PICKER),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = stringResource(R.string.namaz_pick_city_title), style = PqType.CardTitle)
            PixelTextField(
                value = query,
                onValueChange = { query = it },
                label = stringResource(R.string.namaz_city_search),
                modifier = Modifier.fillMaxWidth()
            )
            if (cities.isEmpty()) {
                Text(text = stringResource(R.string.namaz_city_none), style = PqType.Body.copy(color = PqColors.Ink2))
            }
            cities.forEach { city ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable(role = Role.Button) { onPick(city) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = city.name, style = PqType.BodyStrong)
                }
            }
        }
    }
}
