package com.pixelquest.app.islamic.ui

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.BuildConfig
import com.pixelquest.app.R
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.islamic.IslamicItem
import com.pixelquest.app.quran.ui.list.SurahListViewModel
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqButtonStyle
import com.pixelquest.app.ui.components.PqCard
import com.pixelquest.app.ui.components.PqTopBar
import com.pixelquest.app.ui.components.dashedBorder
import com.pixelquest.app.ui.screens.namaz.NamazSummary
import com.pixelquest.app.ui.screens.namaz.NamazSummaryViewModel
import com.pixelquest.app.ui.screens.namaz.NamazUiState
import com.pixelquest.app.ui.screens.namaz.rememberPrayerTimeFormatter
import com.pixelquest.app.ui.screens.namaz.untilText
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import com.pixelquest.app.ui.theme.bsp
import com.pixelquest.app.ui.theme.pqShape
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Where the player is in the Quran, for Deen's Quran tile: the surah, its ayah, and how many it has. */
data class DeenQuran(val surahName: String, val ayah: Int, val ayahCount: Int) {
    val progress: Float get() = if (ayahCount == 0) 0f else ayah / ayahCount.toFloat()
}

/**
 * The Deen tab (UI v2, mock 2D; docs/islamic-hub.md): the next prayer and today's five, then a tile
 * for each other item that's on (the Quran with the place last read, Darood Party).
 */
@Composable
fun IslamicHubScreen(
    onOpen: (route: String) -> Unit,
    viewModel: IslamicHubViewModel = hiltViewModel(),
    prayers: NamazSummaryViewModel = hiltViewModel(),
    quran: SurahListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val summary by prayers.summary.collectAsState()
    val reading by quran.state.collectAsState()
    IslamicHubContent(
        state = state,
        onOpen = onOpen,
        showComingSoon = IslamicHubFlags.showComingSoon,
        prayer = summary,
        quran = reading.continueReading?.let { DeenQuran(it.surah.englishName, it.ayah, it.surah.ayahCount) },
    )
}

/** Switches for working on the hub. */
object IslamicHubFlags {
    /**
     * A "More coming soon" card after the items, to see where a new item will sit. Debug builds
     * only: a release never shows it.
     */
    val showComingSoon: Boolean = BuildConfig.DEBUG
}

/** One cell of the hub's grid: an item's tile, or the coming-soon card. */
private sealed interface HubCell {
    data class Item(val item: IslamicItem) : HubCell
    data object ComingSoon : HubCell
}

/**
 * Deen: its title and today's date, the Namaz Tracker's card (the next prayer, while the tracker is
 * on), then the other items as tiles, two to a row; one to a row in Simple Mode and at large text
 * sizes, where two would leave each line a word a line.
 *
 * @param prayer the next prayer and today's marks; null while the tracker is off.
 * @param quran the place last read; null before the player has read anything.
 * @param showComingSoon adds the "More coming soon" card after the items ([IslamicHubFlags]).
 */
@Composable
fun IslamicHubContent(
    state: IslamicHubUiState,
    onOpen: (route: String) -> Unit,
    modifier: Modifier = Modifier,
    showComingSoon: Boolean = false,
    prayer: NamazSummary? = null,
    quran: DeenQuran? = null,
    today: LocalDate = LocalDate.now(),
) {
    val namaz = state.items.firstOrNull { it.id == NAMAZ }
    val cells = state.items.filter { it.id != NAMAZ }.map { HubCell.Item(it) } +
        if (showComingSoon && state.loaded) listOf(HubCell.ComingSoon) else emptyList()
    val largeText = LocalDensity.current.fontScale >= LARGE_TEXT
    val columns = if (state.isSimpleMode || largeText) 1 else 2
    Column(
        modifier.fillMaxSize().background(PqColors.Ground).testTag(IslamicTestTags.HUB)
            .verticalScroll(rememberScrollState())
            .padding(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 8.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PqTopBar(stringResource(R.string.pq_deen_title)) {
            Text(today.format(DateFormatter), style = PqType.MetaStrong.copy(color = PqColors.Muted, fontSize = 13.bsp))
        }
        com.pixelquest.app.ui.tips.FirstRunTip(com.pixelquest.app.ui.tips.Tip.ISLAMIC_HUB)
        if (namaz != null) {
            if (prayer != null) {
                NextPrayerCard(namaz, prayer, state.isSimpleMode) { onOpen(namaz.route) }
            } else {
                IslamicItemCard(namaz, { onOpen(namaz.route) }, Modifier.fillMaxWidth(), isSimpleMode = state.isSimpleMode)
            }
        }
        cells.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { cell ->
                    val cellModifier = Modifier.weight(1f).fillMaxHeight()
                    when (cell) {
                        is HubCell.Item -> {
                            val reading = quran.takeIf { cell.item.id == QURAN }
                            IslamicItemCard(
                                item = cell.item,
                                onClick = { onOpen(cell.item.route) },
                                modifier = cellModifier,
                                isSimpleMode = state.isSimpleMode,
                                body = reading?.let { stringResource(R.string.pq_deen_quran_continue, it.surahName, it.ayah) },
                                progress = reading?.progress,
                            )
                        }
                        HubCell.ComingSoon -> ComingSoonCard(cellModifier)
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private const val NAMAZ = "namaz"
private const val QURAN = "quran"
private val DateFormatter = DateTimeFormatter.ofPattern("EEE, d MMM")

/** From this font scale the tiles are one to a row. */
internal const val LARGE_TEXT = 1.5f

/**
 * The Namaz Tracker on Deen, while it's on: the next prayer, its time and how long until it, and a
 * box for each of the day's five, ticked once prayed. The card, or its button, opens the tracker.
 */
@Composable
private fun NextPrayerCard(item: IslamicItem, prayer: NamazSummary, isSimpleMode: Boolean, onOpen: () -> Unit) {
    val formatter = rememberPrayerTimeFormatter()
    val name = stringResource(prayer.next.next.prayer.displayName)
    val time = prayer.next.next.time.format(formatter)
    val spoken = stringResource(R.string.namaz_title) + ": " + stringResource(R.string.namaz_home_next, name, time) + ", " +
        pluralStringResource(R.plurals.namaz_progress, prayer.prayed, prayer.prayed, NamazUiState.PRAYERS_PER_DAY)
    PqCard(
        Modifier.fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = spoken; role = Role.Button }
            .testTag(IslamicTestTags.card(item.id)),
        bg = PqColors.Sky, shadow = PqDimens.ShadowLg, radius = 18.dp, onClick = onOpen,
        onClickLabel = stringResource(R.string.islamic_card_open),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.pq_deen_next_prayer), style = PqType.Label, modifier = Modifier.weight(1f))
                // Simple Mode shows no streaks.
                if (!isSimpleMode && prayer.allFiveStreak > 0) {
                    Icon(Icons.Rounded.LocalFireDepartment, null, tint = PqColors.Flame, modifier = Modifier.size(16.dp))
                    Text(pluralStringResource(R.plurals.pq_deen_all_five, prayer.allFiveStreak, prayer.allFiveStreak), style = PqType.MetaStrong)
                }
                if (item.hasBadge) {
                    Spacer(Modifier.size(8.dp))
                    IslamicBadge(item.badgeCount)
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(name.uppercase(), style = PqType.display(56, 52), modifier = Modifier.weight(1f), maxLines = 1)
                Column(horizontalAlignment = Alignment.End) {
                    Text(time, style = PqType.BodyStrong.copy(fontSize = 18.bsp))
                    Text(untilText(prayer.next.remaining), style = PqType.MetaStrong.copy(fontSize = 13.bsp))
                }
            }
            PrayerPips(prayer.prayedPrayers)
            PqButton(
                stringResource(R.string.pq_deen_open_tracker), onOpen, Modifier.fillMaxWidth(),
                style = PqButtonStyle.Secondary, trailingIcon = Icons.Rounded.ArrowForward, height = 44.dp,
            )
        }
    }
}

/** The day's five prayers: a ticked box once prayed, a dashed one until then. A box pops in as it's ticked (M11). */
@Composable
private fun PrayerPips(prayed: Set<Prayer>) {
    val reduced = com.pixelquest.app.ui.motion.reducedMotion()
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Prayer.entries.forEach { p ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val box = Modifier.fillMaxWidth().height(36.dp)
                val on = p in prayed
                val s by androidx.compose.animation.core.animateFloatAsState(
                    if (on) 1f else 0f,
                    when {
                        reduced -> androidx.compose.animation.core.snap()
                        on -> androidx.compose.animation.core.keyframes { durationMillis = com.pixelquest.app.ui.motion.PqMotion.Base; 1.25f at 140 }
                        else -> androidx.compose.animation.core.tween(com.pixelquest.app.ui.motion.PqMotion.Fast)
                    },
                    label = "pip"
                )
                Box(box.dashedBorder(), contentAlignment = Alignment.Center) {
                    if (s > 0.01f) {
                        Box(
                            Modifier.matchParentSize().graphicsLayer { scaleX = s; scaleY = s }
                                .clip(pqShape(9.dp)).background(PqColors.Paper).border(PqDimens.BorderThin, PqColors.Ink, pqShape(9.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Check, null, tint = PqColors.Ink, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                Text(stringResource(p.displayName), style = PqType.NavLabel, maxLines = 1)
            }
        }
    }
}

/** A card that only says more is coming, where the next item will sit. It opens nothing. */
@Composable
private fun ComingSoonCard(modifier: Modifier = Modifier) {
    PqCard(modifier.testTag(IslamicTestTags.COMING_SOON), bg = PqColors.Paper, shadow = 0.dp) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = PqColors.Ink, modifier = Modifier.size(30.dp))
            Text(stringResource(R.string.islamic_coming_soon_title), style = PqType.BodyStrong.copy(fontSize = 16.bsp))
            Text(stringResource(R.string.islamic_coming_soon_body), style = PqType.Meta.copy(color = PqColors.Ink))
        }
    }
}
