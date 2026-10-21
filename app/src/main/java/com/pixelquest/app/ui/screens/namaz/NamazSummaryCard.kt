package com.pixelquest.app.ui.screens.namaz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
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
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.R
import com.pixelquest.app.ui.components.CategoryTile
import com.pixelquest.app.ui.components.PqCard
import com.pixelquest.app.ui.components.prayerIcon
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqType
import com.pixelquest.app.ui.theme.bsp
import com.pixelquest.app.domain.namaz.NextPrayer
import com.pixelquest.app.domain.namaz.NextPrayerUseCase
import com.pixelquest.app.domain.namaz.PrayerStatus
import com.pixelquest.app.domain.repository.NamazRepository
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
    private val zone: () -> ZoneId,
    hubShown: kotlinx.coroutines.flow.Flow<Boolean> = flowOf(true)
) : ViewModel() {

    @Inject
    constructor(repository: NamazRepository, nextPrayer: NextPrayerUseCase, clock: AppClock, settings: com.pixelquest.app.domain.repository.SettingsRepository) :
        this(repository, nextPrayer, clock, { ZoneId.systemDefault() }, settings.islamicHubShown)

    /** The card while the tracker is on, and the Islamic section shows (it's hidden in Settings otherwise). */
    @OptIn(ExperimentalCoroutinesApi::class)
    val summary: StateFlow<NamazSummary?> = combine(clock.minuteTicks(), repository.observeSettings(), hubShown) { _, settings, shown ->
        settings.enabled && shown
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

/**
 * The next prayer and today's progress at a glance (UI v2, mock 2A): "NEXT PRAYER · 4/5 TODAY",
 * the prayer and its time, and how long until it. A tap opens the tracker.
 */
@Composable
fun NamazSummaryCard(summary: NamazSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val formatter = rememberPrayerTimeFormatter()
    val prayer = stringResource(summary.next.next.prayer.displayName)
    val time = summary.next.next.time.format(formatter)
    val nextLine = stringResource(R.string.namaz_home_next, prayer, time)
    val progressLine = pluralStringResource(R.plurals.namaz_progress, summary.prayed, summary.prayed, NamazUiState.PRAYERS_PER_DAY)
    val title = stringResource(R.string.namaz_title)
    val key = summary.next.next.prayer.name.lowercase()
    PqCard(
        modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = "$title: $nextLine, $progressLine"; role = Role.Button }
            .testTag(NamazTestTags.HOME_CARD),
        bg = PqColors.Sky,
        onClick = onClick,
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CategoryTile(prayerIcon(key), PqColors.Paper)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.namaz_home_label, summary.prayed, NamazUiState.PRAYERS_PER_DAY), style = PqType.Label)
                Text(stringResource(R.string.namaz_home_prayer_time, prayer, time), style = PqType.BodyStrong.copy(fontSize = 16.bsp))
                Text(untilText(summary.next.remaining), style = PqType.MetaStrong)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = PqColors.Ink)
        }
    }
}

/** How long until a prayer, in words: "in 36 min", "in 2 h 5 min". */
@Composable
fun untilText(remaining: java.time.Duration): String {
    val minutes = remaining.toMinutes().coerceAtLeast(0).toInt()
    return if (minutes < 60) {
        pluralStringResource(R.plurals.namaz_in_minutes, minutes, minutes)
    } else {
        stringResource(R.string.namaz_in_hours_minutes, minutes / 60, minutes % 60)
    }
}
