package com.pixelquest.app.darood.ui.create

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
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
import com.pixelquest.app.darood.ui.formatCount
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.party.ui.components.MemberLimitStepper
import com.pixelquest.app.party.ui.icon
import com.pixelquest.app.party.ui.shortName
import com.pixelquest.app.party.ui.tint
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqButtonStyle
import com.pixelquest.app.ui.components.PqChoiceChip
import com.pixelquest.app.ui.components.PqFieldLabel
import com.pixelquest.app.ui.components.PqTextField
import com.pixelquest.app.ui.components.PqTopBar
import com.pixelquest.app.ui.components.comic
import com.pixelquest.app.ui.theme.PqCircle
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType

/**
 * Starting a party (UI v2, mock 4D): its type first, then its name, its size and, for Darood, its
 * milestone if wanted. Once it's made, [onCreated] opens it; [onChooseUsername] goes to the
 * username screen when one is needed first.
 */
@Composable
fun CreatePartyScreen(
    onNavigateBack: () -> Unit,
    onCreated: (partyId: String, type: PartyType) -> Unit = { _, _ -> },
    onChooseUsername: () -> Unit = {},
    viewModel: CreatePartyViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    androidx.compose.runtime.LaunchedEffect(state.created) { state.created?.let { onCreated(it, state.type) } }
    CreatePartyContent(
        onCreate = viewModel::create,
        onChooseUsername = onChooseUsername,
        state = state,
        onNameChange = viewModel::onNameChange,
        onFewer = viewModel::fewerMembers,
        onMore = viewModel::moreMembers,
        onMilestoneChange = viewModel::onMilestoneChange,
        onType = viewModel::setType,
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
    onChooseUsername: () -> Unit = {},
    onType: (PartyType) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PqColors.Ground)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 8.dp, bottom = 24.dp)
            .testTag(DaroodTestTags.CREATE_SCREEN),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PqTopBar(stringResource(R.string.darood_create_title), onBack = onNavigateBack)
        // The kind first, when there's more than one to start.
        if (state.types.size > 1) {
            PqFieldLabel(stringResource(R.string.pq_new_party_type))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.selectableGroup()) {
                state.types.forEach { type -> TypeChoice(type, type == state.type, { onType(type) }, Modifier.weight(1f)) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            PqFieldLabel(stringResource(R.string.darood_create_name), Modifier.weight(1f))
            // The live counter: how much of the 40 the name uses.
            Text(
                text = stringResource(R.string.darood_create_name_counter, state.nameLength, DaroodRules.NAME_MAX),
                style = PqType.MetaStrong.copy(color = if (state.nameLength > DaroodRules.NAME_MAX) PqColors.RedText else PqColors.Muted),
                modifier = Modifier.testTag(DaroodTestTags.CREATE_NAME_COUNTER).semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
        PqTextField(
            state.name, onNameChange, stringResource(R.string.darood_create_name_placeholder),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
            modifier = Modifier.testTag(DaroodTestTags.CREATE_NAME)
        )
        when (state.nameProblem) {
            PartyNameProblem.TOO_SHORT -> Problem(stringResource(R.string.darood_create_name_too_short))
            PartyNameProblem.TOO_LONG -> Problem(stringResource(R.string.darood_create_name_too_long))
            null -> Unit
        }
        MemberLimitStepper(count = state.maxMembers, canFewer = state.canHaveFewer, canMore = state.canHaveMore, onFewer = onFewer, onMore = onMore)
        // The milestone, for a kind that has one: optional now, and the leader can set it later.
        if (state.asksMilestone) {
            PqFieldLabel(stringResource(R.string.darood_create_milestone))
            PqTextField(
                state.milestoneText, onMilestoneChange, stringResource(R.string.darood_create_milestone_placeholder),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                modifier = Modifier.testTag(DaroodTestTags.CREATE_MILESTONE)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MILESTONE_CHOICES.forEach { m ->
                    PqChoiceChip(formatCount(m), state.milestone == m, { onMilestoneChange(m.toString()) })
                }
            }
            if (state.milestoneIsZero) {
                Problem(stringResource(R.string.darood_create_milestone_zero))
            } else {
                Text(
                    text = state.milestone?.takeIf { it > 0 }?.let { stringResource(R.string.darood_create_milestone_read, formatCount(it)) }
                        ?: stringResource(R.string.darood_create_milestone_hint),
                    style = PqType.Meta
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
                style = PqType.MetaStrong.copy(color = PqColors.RedText),
                modifier = Modifier.testTag(DaroodTestTags.CREATE_ERROR).semantics { liveRegion = LiveRegionMode.Polite }
            )
            if (error == CreatePartyError.USERNAME_REQUIRED) {
                PqButton(stringResource(R.string.darood_create_choose_username), onChooseUsername, Modifier.fillMaxWidth(), style = PqButtonStyle.Secondary)
            }
        }
        Spacer(Modifier.height(8.dp))
        PqButton(
            stringResource(if (state.creating) R.string.darood_create_creating else R.string.darood_create_button), onCreate,
            Modifier.fillMaxWidth().testTag(DaroodTestTags.CREATE_BUTTON), enabled = state.canCreate, height = 52.dp
        )
    }
}

/** Ready-made milestones, a tap away. */
private val MILESTONE_CHOICES = listOf(10_000L, 50_000L, 100_000L)

@Composable
private fun Problem(text: String) {
    Text(text, style = PqType.MetaStrong.copy(color = PqColors.RedText))
}

/** One kind of party to start: its icon and name, the chosen one in its colour with a tick. */
@Composable
private fun TypeChoice(type: PartyType, selected: Boolean, onSelect: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier) {
        Row(
            Modifier.fillMaxWidth().height(48.dp)
                .comic(bg = if (selected) type.tint else PqColors.Paper, shadow = if (selected) PqDimens.Shadow else 0.dp, tappable = true)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
                .testTag(DaroodTestTags.createType(type.wire))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(type.icon, null, tint = PqColors.Ink)
            Text(stringResource(type.shortName()), style = PqType.BodyStrong)
        }
        if (selected) Box(
            Modifier.align(Alignment.TopEnd).size(24.dp).clip(PqCircle).background(PqColors.Ink),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Rounded.Check, null, tint = PqColors.Paper, modifier = Modifier.size(16.dp)) }
    }
}
