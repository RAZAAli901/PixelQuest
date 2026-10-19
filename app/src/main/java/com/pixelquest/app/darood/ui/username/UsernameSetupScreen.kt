package com.pixelquest.app.darood.ui.username

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.runtime.collectAsState
import com.pixelquest.app.R
import com.pixelquest.app.darood.domain.UsernameProblem
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelTextField
import com.pixelquest.app.ui.components.PixelTopAppBar
import com.pixelquest.app.ui.components.SymbolIcon
import com.pixelquest.app.ui.theme.PixelTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Claiming or changing the Darood Party username. [onDone] runs once it's saved; [onNavigateBack]
 * leaves without saving (null hides the back button).
 */
@Composable
fun UsernameSetupScreen(
    onDone: () -> Unit,
    onNavigateBack: (() -> Unit)?,
    viewModel: UsernameSetupViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(state.saved) { if (state.saved != null) onDone() }
    UsernameSetupContent(
        state = state,
        onInputChange = viewModel::onInputChange,
        onSave = viewModel::save,
        onNavigateBack = onNavigateBack
    )
}

@Composable
fun UsernameSetupContent(
    state: UsernameSetupUiState,
    onInputChange: (String) -> Unit,
    onSave: () -> Unit,
    onNavigateBack: (() -> Unit)?
) {
    val colors = PixelTheme.colors
    Scaffold(
        containerColor = colors.background,
        topBar = {
            PixelTopAppBar(
                title = stringResource(R.string.darood_username_title),
                navigationIcon = onNavigateBack?.let { back ->
                    {
                        IconButton(onClick = back) {
                            SymbolIcon(
                                symbol = "◀",
                                contentDescription = stringResource(R.string.navigate_back),
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.primaryText
                            )
                        }
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
                .testTag(DaroodTestTags.USERNAME_SCREEN),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.darood_username_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface
            )
            state.current?.let { current ->
                Text(
                    text = stringResource(R.string.darood_username_current, current.username.value) +
                        if (current.changeableAt.isAfter(Instant.now())) {
                            " " + stringResource(R.string.darood_username_changeable_at, formatDay(current.changeableAt))
                        } else {
                            ""
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.primaryText
                )
            }
            PixelTextField(
                value = state.input,
                onValueChange = onInputChange,
                label = stringResource(R.string.darood_username_label),
                placeholder = stringResource(R.string.darood_username_placeholder),
                errorText = errorText(state),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrect = false,
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onSave() }),
                modifier = Modifier.fillMaxWidth().testTag(DaroodTestTags.USERNAME_FIELD)
            )
            statusText(state)?.let { status ->
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.primaryText,
                    modifier = Modifier
                        .testTag(DaroodTestTags.USERNAME_STATUS)
                        .semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
            Text(
                text = stringResource(R.string.darood_username_rules),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
            PixelButton(
                text = stringResource(if (state.saving) R.string.darood_username_saving else R.string.darood_username_save),
                onClick = onSave,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth().testTag(DaroodTestTags.USERNAME_SAVE)
            )
        }
    }
}

/** What's wrong with the name, or the save: shown under the field. */
@Composable
private fun errorText(state: UsernameSetupUiState): String? {
    state.saveError?.let { error ->
        return when (error) {
            UsernameSaveError.Taken -> stringResource(R.string.darood_username_error_taken)
            UsernameSaveError.Invalid -> stringResource(R.string.darood_username_error_invalid)
            is UsernameSaveError.TooSoon -> stringResource(R.string.darood_username_error_too_soon, formatDay(error.changeableAt))
            UsernameSaveError.Offline -> stringResource(R.string.darood_username_error_offline)
        }
    }
    return when (state.problem) {
        null -> if (state.availability == UsernameAvailability.TAKEN) stringResource(R.string.darood_username_taken, state.input) else null
        UsernameProblem.EMPTY -> null
        UsernameProblem.TOO_SHORT -> stringResource(R.string.darood_username_too_short)
        UsernameProblem.TOO_LONG -> stringResource(R.string.darood_username_too_long)
        UsernameProblem.INVALID_CHARACTERS -> stringResource(R.string.darood_username_invalid_characters)
        UsernameProblem.RESERVED -> stringResource(R.string.darood_username_reserved)
    }
}

/** The check's progress or good news, under the field; problems go in [errorText]. */
@Composable
private fun statusText(state: UsernameSetupUiState): String? = when {
    state.saveError != null || state.problem != null || state.input.isEmpty() -> null
    state.isCurrent -> stringResource(R.string.darood_username_yours)
    state.availability == UsernameAvailability.CHECKING -> stringResource(R.string.darood_username_checking)
    state.availability == UsernameAvailability.AVAILABLE -> stringResource(R.string.darood_username_available, state.input)
    state.availability == UsernameAvailability.UNKNOWN -> stringResource(R.string.darood_username_unknown)
    else -> null
}

private fun formatDay(instant: Instant): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault()).format(instant)
