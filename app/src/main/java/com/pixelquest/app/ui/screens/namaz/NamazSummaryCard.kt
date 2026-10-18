package com.pixelquest.app.ui.screens.namaz

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.R
import com.pixelquest.app.domain.namaz.NextPrayer
import com.pixelquest.app.domain.namaz.NextPrayerUseCase
import com.pixelquest.app.domain.namaz.PrayerStatus
import com.pixelquest.app.domain.repository.NamazRepository
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.inkOnPanel
import com.pixelquest.app.util.AppClock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.ZoneId
import javax.inject.Inject

/** Home's glance at the Namaz Tracker: the next prayer, and how many of today's five are prayed. */
data class NamazSummary(val next: NextPrayer, val prayed: Int)

/**
 * The Home card's state: null while the tracker is off, so the card isn't there at all. Lighter
 * than the tracker's own ViewModel: no week, streak or missed marks.
 */
@HiltViewModel
class NamazSummaryViewModel(
    private val repository: NamazRepository,
    private val nextPrayer: NextPrayerUseCase,
    private val clock: AppClock,
    private val zone: () -> ZoneId
) : ViewModel() {

    @Inject
    constructor(repository: NamazRepository, nextPrayer: NextPrayerUseCase, clock: AppClock) :
        this(repository, nextPrayer, clock, { ZoneId.systemDefault() })

    @OptIn(ExperimentalCoroutinesApi::class)
    val summary: StateFlow<NamazSummary?> = combine(clock.minuteTicks(), repository.observeSettings()) { _, settings ->
        settings.enabled
    }.flatMapLatest { enabled ->
        if (!enabled) {
            flowOf(null)
        } else {
            val next = nextPrayer(clock.now().atZone(zone()))
            repository.observeDay(next.prayerDay).map { logs ->
                NamazSummary(next, logs.count { it.status == PrayerStatus.PRAYED })
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** Home's Namaz card, shown only while the tracker is on. */
@Composable
fun HomeNamazSection(
    onOpen: () -> Unit,
    viewModel: NamazSummaryViewModel = hiltViewModel()
) {
    val summary by viewModel.summary.collectAsState()
    summary?.let { NamazSummaryCard(summary = it, onClick = onOpen) }
}

/** The next prayer and today's progress at a glance; a tap opens the tracker. */
@Composable
fun NamazSummaryCard(summary: NamazSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = PixelTheme.colors
    val formatter = rememberPrayerTimeFormatter()
    val nextLine = stringResource(
        R.string.namaz_home_next,
        stringResource(summary.next.next.prayer.displayName),
        summary.next.next.time.format(formatter)
    )
    val progressLine = pluralStringResource(R.plurals.namaz_progress, summary.prayed, summary.prayed, NamazUiState.PRAYERS_PER_DAY)
    val title = stringResource(R.string.namaz_title)
    PixelCard(
        variant = PixelPanelVariant.BORDER,
        contentPadding = 14.dp,
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.namaz_home_open), onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = "$title: $nextLine, $progressLine" }
            .testTag(NamazTestTags.HOME_CARD)
    ) {
        Row(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "🕌", fontSize = 24.sp, modifier = Modifier.clearAndSetSemantics {})
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = nextLine, style = MaterialTheme.typography.titleSmall, color = inkOnPanel(colors.primaryText))
                Text(text = progressLine, style = MaterialTheme.typography.bodySmall, color = colors.onSurface)
            }
            Text(
                text = "›",
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurfaceVariant,
                modifier = Modifier.clearAndSetSemantics {}
            )
        }
    }
}
