package com.pixelquest.app.darood.ui.create

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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.R
import com.pixelquest.app.darood.domain.DaroodRules
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelTextField
import com.pixelquest.app.ui.components.PixelTopAppBar
import com.pixelquest.app.ui.components.SymbolIcon
import com.pixelquest.app.ui.theme.PixelTheme

/**
 * Starting a party: its name, its size and, if wanted, its milestone. Once it's made, [onCreated]
 * opens it; [onChooseUsername] goes to the username screen when one is needed first.
 */
@Composable
fun CreatePartyScreen(
    onNavigateBack: () -> Unit,
    onCreated: (String) -> Unit = {},
    onChooseUsername: () -> Unit = {},
    viewModel: CreatePartyViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    androidx.compose.runtime.LaunchedEffect(state.created) { state.created?.let(onCreated) }
    CreatePartyContent(
        onCreate = viewModel::create,
        onChooseUsername = onChooseUsername,
        state = state,
        onNameChange = viewModel::onNameChange,
        onFewer = viewModel::fewerMembers,
        onMore = viewModel::moreMembers,
        onMilestoneChange = viewModel::onMilestoneChange,
        onNavigateBack = onNavigateBack
    )
}

@Composable
fun CreatePartyContent(
    state: CreatePartyUiState,
    onNameChange: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onFewer: () -> Unit = {},
    onMore: () -> Unit = {},
    onMilestoneChange: (String) -> Unit = {},
    onCreate: () -> Unit = {},
    onChooseUsername: () -> Unit = {}
) {
    val colors = PixelTheme.colors
    Scaffold(
        containerColor = colors.background,
        topBar = {
            PixelTopAppBar(
                title = stringResource(R.string.darood_create_title),
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
                .testTag(DaroodTestTags.CREATE_SCREEN),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PixelTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = stringResource(R.string.darood_create_name),
                    placeholder = stringResource(R.string.darood_create_name_placeholder),
                    errorText = when (state.nameProblem) {
                        PartyNameProblem.TOO_SHORT -> stringResource(R.string.darood_create_name_too_short)
                        PartyNameProblem.TOO_LONG -> stringResource(R.string.darood_create_name_too_long)
                        null -> null
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth().testTag(DaroodTestTags.CREATE_NAME)
                )
                // The live counter: how much of the 40 the name uses.
                Text(
                    text = stringResource(R.string.darood_create_name_counter, state.nameLength, DaroodRules.NAME_MAX),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (state.nameLength > DaroodRules.NAME_MAX) colors.error else colors.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.End)
                        .testTag(DaroodTestTags.CREATE_NAME_COUNTER)
                        .semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
            MemberLimitStepper(count = state.maxMembers, canFewer = state.canHaveFewer, canMore = state.canHaveMore, onFewer = onFewer, onMore = onMore)
            // The milestone, for a kind that has one: optional now, and the leader can set it later.
            if (state.asksMilestone) {
                // The milestone: optional now, and the leader can set it later.
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PixelTextField(
                        value = state.milestoneText,
                        onValueChange = onMilestoneChange,
                        label = stringResource(R.string.darood_create_milestone),
                        placeholder = stringResource(R.string.darood_create_milestone_placeholder),
                        errorText = if (state.milestoneIsZero) stringResource(R.string.darood_create_milestone_zero) else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth().testTag(DaroodTestTags.CREATE_MILESTONE)
                    )
                    Text(
                        text = state.milestone?.takeIf { it > 0 }?.let { stringResource(R.string.darood_create_milestone_read, com.pixelquest.app.darood.ui.formatCount(it)) }
                            ?: stringResource(R.string.darood_create_milestone_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }
            state.error?.let { error ->
                Text(
                    text = stringResource(
                        when (error) {
                            CreatePartyError.USERNAME_REQUIRED -> R.string.darood_create_needs_username
                            CreatePartyError.OFFLINE -> R.string.darood_create_offline
                            CreatePartyError.FAILED -> R.string.darood_create_failed
                        }
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.error,
                    modifier = Modifier.testTag(DaroodTestTags.CREATE_ERROR).semantics { liveRegion = LiveRegionMode.Polite }
                )
                if (error == CreatePartyError.USERNAME_REQUIRED) {
                    PixelButton(
                        text = stringResource(R.string.darood_create_choose_username),
                        onClick = onChooseUsername,
                        variant = com.pixelquest.app.ui.components.PixelButtonVariant.BLUE,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            PixelButton(
                text = stringResource(if (state.creating) R.string.darood_create_creating else R.string.darood_create_button),
                onClick = onCreate,
                enabled = state.canCreate,
                modifier = Modifier.fillMaxWidth().testTag(DaroodTestTags.CREATE_BUTTON)
            )
        }
    }
}

/** How many the party holds, 2 to 30, the leader included: − and + around the number. */
@Composable
private fun MemberLimitStepper(count: Int, canFewer: Boolean, canMore: Boolean, onFewer: () -> Unit, onMore: () -> Unit) {
    val colors = PixelTheme.colors
    val description = pluralStringResource(R.plurals.darood_create_limit_value, count, count)
    // TalkBack says what the buttons do, not their signs.
    val fewer = stringResource(R.string.darood_create_fewer)
    val more = stringResource(R.string.darood_create_more)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = stringResource(R.string.darood_create_limit), style = MaterialTheme.typography.labelLarge, color = colors.primaryText)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PixelButton(
                text = "−",
                onClick = onFewer,
                enabled = canFewer,
                modifier = Modifier.testTag(DaroodTestTags.CREATE_FEWER).semantics { contentDescription = fewer }
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.onSurface,
                modifier = Modifier
                    .testTag(DaroodTestTags.CREATE_LIMIT)
                    .semantics { stateDescription = description }
            )
            PixelButton(
                text = "+",
                onClick = onMore,
                enabled = canMore,
                modifier = Modifier.testTag(DaroodTestTags.CREATE_MORE).semantics { contentDescription = more }
            )
        }
        Text(text = stringResource(R.string.darood_create_limit_hint), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    }
}
