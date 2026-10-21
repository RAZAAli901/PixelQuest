package com.pixelquest.app.party.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.darood.domain.UsernameProblem
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.darood.ui.invite.Lookup
import com.pixelquest.app.ui.components.InitialAvatar
import com.pixelquest.app.ui.components.PixelTextField
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqCard
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqType

// Finding someone to invite by username: shared by every kind of party (docs/party-core.md).

/** The username to invite, typed: no capitals or corrections, and Search on the keyboard. */
@Composable
fun InviteSearch(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    PixelTextField(
        value = query,
        onValueChange = onQueryChange,
        label = stringResource(R.string.darood_add_search),
        placeholder = stringResource(R.string.darood_username_placeholder),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrect = false,
            keyboardType = KeyboardType.Ascii,
            imeAction = ImeAction.Search
        ),
        modifier = modifier.fillMaxWidth().testTag(DaroodTestTags.ADD_SEARCH)
    )
}


/** What the search found: a row to invite from, or a line saying why not. */
@Composable
fun LookupResult(lookup: Lookup, enabled: Boolean, onInvite: (String) -> Unit) {
    val line = when (lookup) {
        Lookup.Idle -> null
        Lookup.Checking -> stringResource(R.string.darood_username_checking)
        is Lookup.NotFound -> stringResource(R.string.darood_add_not_found, lookup.username.value)
        Lookup.Yours -> stringResource(R.string.darood_add_yours)
        is Lookup.Invalid -> stringResource(
            when (lookup.problem) {
                UsernameProblem.TOO_SHORT -> R.string.darood_username_too_short
                UsernameProblem.TOO_LONG -> R.string.darood_username_too_long
                UsernameProblem.RESERVED -> R.string.darood_add_not_found_reserved
                UsernameProblem.EMPTY, UsernameProblem.INVALID_CHARACTERS -> R.string.darood_username_invalid_characters
            }
        )
        Lookup.Unknown -> stringResource(R.string.darood_username_unknown)
        is Lookup.Found -> null
    }
    if (line != null) {
        Text(
            text = line,
            style = PqType.Body.copy(color = PqColors.Ink2),
            modifier = Modifier.testTag(DaroodTestTags.ADD_STATUS).semantics { liveRegion = LiveRegionMode.Polite }
        )
    }
    if (lookup is Lookup.Found) {
        PqCard(Modifier.fillMaxWidth().testTag(DaroodTestTags.ADD_RESULT)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            ) {
                InitialAvatar(lookup.username.value)
                Text(
                    text = stringResource(R.string.darood_username_handle, lookup.username.value),
                    style = PqType.BodyStrong,
                    modifier = Modifier.weight(1f)
                )
                PqButton(
                    text = stringResource(R.string.darood_add_invite),
                    onClick = { onInvite(lookup.username.value) },
                    enabled = enabled,
                    modifier = Modifier.testTag(DaroodTestTags.ADD_INVITE)
                )
            }
        }
    }
}
