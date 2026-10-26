package com.pixelquest.app.study.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.ChevronRight
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.R
import com.pixelquest.app.party.data.PartyRepository
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.study.data.StudyRepository
import com.pixelquest.app.ui.components.CategoryTile
import com.pixelquest.app.ui.components.PqCard
import com.pixelquest.app.ui.navigation.TabSharing
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import com.pixelquest.app.ui.theme.pqShape
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Duration
import java.time.ZoneId
import javax.inject.Inject

/** One item on the Home card: what, where, and how close. */
data class HomeDueLine(
    val partyId: String,
    val partyName: String,
    val itemId: String,
    val title: String,
    val due: DueState,
    val dueAt: java.time.Instant?,
)

/**
 * The Home card's content: shown while the player is in a study party. [due] is what they haven't
 * finished, soonest first, overdue ones too, at most three, due within the next week.
 */
data class HomeStudy(val due: List<HomeDueLine>)

/** The study parties' Home card, from the phone's own copy: no call to the server of its own. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeStudyViewModel @Inject constructor(parties: PartyRepository, private val study: StudyRepository) : ViewModel() {
    val home: StateFlow<HomeStudy?> = parties.observeMyParties(PartyType.STUDY).flatMapLatest { all ->
        val mine = all.filter { it.myStatus == MemberStatus.ACTIVE }.associateBy { it.id }
        study.observeDueSoon(study.now().plus(Duration.ofDays(7)), LIMIT).map { items ->
            if (mine.isEmpty()) {
                null
            } else {
                val now = study.now()
                HomeStudy(
                    items.filter { it.partyId in mine }.map {
                        HomeDueLine(
                            it.partyId,
                            mine.getValue(it.partyId).name,
                            it.id,
                            it.title,
                            dueStateOf(it.dueAt, now, study.zone()),
                            it.dueAt,
                        )
                    },
                )
            }
        }
    }.stateIn(viewModelScope, TabSharing, null)

    private companion object {
        const val LIMIT = 3
    }
}

/** On Home, while the player is in a study party: what's due soon, each line opening its party. */
@Composable
fun HomeStudySection(onOpenParty: (String) -> Unit, onOpenParties: () -> Unit, viewModel: HomeStudyViewModel = hiltViewModel()) {
    val home by viewModel.home.collectAsState()
    home?.let { HomeStudyCard(it, onOpenParty, onOpenParties) }
}

@Composable
fun HomeStudyCard(home: HomeStudy, onOpenParty: (String) -> Unit, onOpenParties: () -> Unit, zone: ZoneId = ZoneId.systemDefault()) {
    PqCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(StudyTags.HOME_CARD)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.study_home_open), onClick = onOpenParties),
        bg = PqColors.Lilac,
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button, onClickLabel = stringResource(R.string.study_home_open), onClick = onOpenParties),
            ) {
                CategoryTile(Icons.AutoMirrored.Rounded.MenuBook, PqColors.Paper)
                Text(text = stringResource(R.string.study_home_title).uppercase(), style = PqType.Label, modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.ChevronRight, null, tint = PqColors.Ink)
            }
            if (home.due.isEmpty()) {
                Text(text = stringResource(R.string.study_home_nothing), style = PqType.Body.copy(color = PqColors.Ink2))
            }
            home.due.forEach { line ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(pqShape(12.dp))
                        .background(PqColors.Paper)
                        .border(PqDimens.BorderThin, PqColors.Ink, pqShape(12.dp))
                        .clickable(role = Role.Button, onClickLabel = stringResource(R.string.study_home_open_party)) {
                            onOpenParties()
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag(StudyTags.homeLine(line.itemId)),
                ) {
                    Text(text = line.title, style = PqType.BodyStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = stringResource(R.string.study_home_where, line.partyName, dueText(line.due, line.dueAt, zone)),
                        style = if (line.due == DueState.OVERDUE) PqType.MetaStrong.copy(color = PqColors.RedText) else PqType.Meta,
                    )
                }
            }
        }
    }
}
