package com.pixelquest.app.darood.ui.invite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.R
import com.pixelquest.app.darood.domain.UsernameProblem
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.components.PixelTextField
import com.pixelquest.app.ui.components.PixelTopAppBar
import com.pixelquest.app.ui.components.SymbolIcon
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.inkOnPanel

/** Adding people to a party by their exact username. */
@Composable
fun AddPeopleScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddPeopleViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    AddPeopleContent(state = state, onQueryChange = viewModel::onQueryChange, onNavigateBack = onNavigateBack)
}

@Composable
fun AddPeopleContent(
    state: AddPeopleUiState,
    onQueryChange: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onInvite: (String) -> Unit = {}
) {
    val colors = PixelTheme.colors
    Scaffold(
        containerColor = colors.background,
        topBar = {
            PixelTopAppBar(
                title = stringResource(R.string.darood_add_title),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        SymbolIcon(
                            symbol = "◀",
                            contentDescription = stringResource(R.string.navigate_back),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.primaryText
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag(DaroodTestTags.ADD_SCREEN),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = stringResource(R.string.darood_add_intro), style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
            PixelTextField(
                value = state.query,
                onValueChange = onQueryChange,
                label = stringResource(R.string.darood_add_search),
                placeholder = stringResource(R.string.darood_username_placeholder),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrect = false,
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Search
                ),
                modifier = Modifier.fillMaxWidth().testTag(DaroodTestTags.ADD_SEARCH)
            )
            LookupResult(state.lookup, onInvite)
        }
    }
}

/** What the search found: a row to invite from, or a line saying why not. */
@Composable
private fun LookupResult(lookup: Lookup, onInvite: (String) -> Unit) {
    val colors = PixelTheme.colors
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
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            modifier = Modifier.testTag(DaroodTestTags.ADD_STATUS).semantics { liveRegion = LiveRegionMode.Polite }
        )
    }
    if (lookup is Lookup.Found) {
        PixelCard(
            variant = PixelPanelVariant.BORDER,
            contentPadding = 16.dp,
            modifier = Modifier.fillMaxWidth().testTag(DaroodTestTags.ADD_RESULT)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.darood_username_handle, lookup.username.value),
                    style = MaterialTheme.typography.titleMedium,
                    color = inkOnPanel(colors.primaryText),
                    modifier = Modifier.weight(1f)
                )
                PixelButton(
                    text = stringResource(R.string.darood_add_invite),
                    onClick = { onInvite(lookup.username.value) },
                    modifier = Modifier.testTag(DaroodTestTags.ADD_INVITE)
                )
            }
        }
    }
}
