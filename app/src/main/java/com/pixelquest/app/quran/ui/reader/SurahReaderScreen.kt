package com.pixelquest.app.quran.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.R
import com.pixelquest.app.quran.ui.QuranTypography
import com.pixelquest.app.ui.components.PixelTopAppBar
import com.pixelquest.app.ui.components.SymbolIcon
import com.pixelquest.app.ui.theme.PixelTheme
import kotlinx.coroutines.flow.distinctUntilChanged

/** One surah in the reader, with its state from [SurahReaderViewModel]. */
@Composable
fun SurahReaderRoute(onNavigateBack: () -> Unit, viewModel: SurahReaderViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    SurahReaderScreen(
        state = state,
        onNavigateBack = onNavigateBack,
        onLoadMore = viewModel::loadMore,
        onScrolled = viewModel::onScrolled,
        onTargetShown = viewModel::onTargetShown,
        onDisplayMode = viewModel::setDisplayMode
    )
}

/**
 * The reader: a top bar with the surah's name, what it means and its ayah count, the way back, and
 * the reading settings; then its ayahs, loading more as the player nears the end.
 *
 * @param onScrolled the ayah first in view, as the player scrolls.
 * @param onTargetShown [ReaderUiState.target] has been brought into view.
 */
@Composable
fun SurahReaderScreen(
    state: ReaderUiState,
    onNavigateBack: () -> Unit,
    onLoadMore: () -> Unit = {},
    onScrolled: (ayah: Int) -> Unit = {},
    onTargetShown: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onDisplayMode: (DisplayMode) -> Unit = {},
    listState: LazyListState = rememberLazyListState()
) {
    val colors = PixelTheme.colors
    val surah = state.surah
    Scaffold(
        containerColor = colors.background,
        topBar = {
            PixelTopAppBar(
                title = surah?.englishName.orEmpty(),
                subtitle = surah?.let {
                    "${it.englishMeaning} · ${pluralStringResource(R.plurals.quran_ayah_count, it.ayahCount, it.ayahCount)}"
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        SymbolIcon(
                            symbol = "◀",
                            contentDescription = stringResource(R.string.navigate_back),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.primaryText
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings, modifier = Modifier.testTag(ReaderTags.SETTINGS)) {
                        SymbolIcon(
                            symbol = "⚙️",
                            contentDescription = stringResource(R.string.quran_reader_settings),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.primaryText
                        )
                    }
                }
            )
        },
        modifier = Modifier.testTag(ReaderTags.SCREEN)
    ) { padding ->
        ReaderEffects(state, listState, onLoadMore, onScrolled, onTargetShown)
        val typography = QuranTypography.of(color = colors.onBackground)
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            DisplayModeSwitch(
                selected = state.displayMode,
                onSelect = onDisplayMode,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize().testTag(ReaderTags.LIST)
            ) {
                surah?.bismillah?.let { bismillah ->
                    item(key = "bismillah") { BismillahHeader(bismillah = bismillah, typography = typography) }
                }
                items(state.ayahs, key = { it.id }) { ayah ->
                    AyahItem(ayah = ayah, mode = state.displayMode, typography = typography)
                    HorizontalDivider(modifier = Modifier.padding(top = 16.dp), color = colors.onSurface.copy(alpha = 0.12f))
                }
            }
        }
    }
}

/**
 * What the reader does as it's scrolled: loads more near the end, says which ayah is first in view,
 * and brings the target ayah into view once it's loaded.
 */
@Composable
private fun ReaderEffects(
    state: ReaderUiState,
    listState: LazyListState,
    onLoadMore: () -> Unit,
    onScrolled: (Int) -> Unit,
    onTargetShown: () -> Unit
) {
    val current by rememberUpdatedState(state)
    // The list's items are the Bismillah (where the surah has one), then the ayahs.
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .collect { last ->
                val ayahIndex = last?.minus(headerCount(current.surah))
                if (ayahIndex != null && current.hasMore && ayahIndex >= current.ayahs.size - LOAD_AHEAD) onLoadMore()
            }
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                val ayahIndex = (index - headerCount(current.surah)).coerceAtLeast(0)
                current.ayahs.getOrNull(ayahIndex)?.let { onScrolled(it.number) }
            }
    }
    LaunchedEffect(state.target, state.ayahs.size) {
        val target = state.target ?: return@LaunchedEffect
        val index = state.ayahs.indexOfFirst { it.number == target }
        if (index >= 0) {
            listState.scrollToItem(index + headerCount(state.surah))
            onTargetShown()
        }
    }
}

/** How many ayahs before the end of those loaded the next page is asked for. */
private const val LOAD_AHEAD = 10

/** The reader's test tags. */
object ReaderTags {
    const val SCREEN = "quran_reader"
    const val BISMILLAH = "quran_bismillah"
    const val LIST = "quran_reader_list"
    const val SETTINGS = "quran_reader_settings"

    fun ayah(number: Int) = "quran_ayah_$number"

    fun arabic(number: Int) = "quran_ayah_arabic_$number"

    fun translation(number: Int) = "quran_ayah_translation_$number"

    fun mode(mode: DisplayMode) = "quran_mode_${mode.name.lowercase()}"
}
