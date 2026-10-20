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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import android.content.Intent
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.R
import com.pixelquest.app.quran.domain.Ayah
import com.pixelquest.app.ui.components.PixelTopAppBar
import com.pixelquest.app.ui.components.SymbolIcon
import com.pixelquest.app.ui.theme.PixelTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/** One surah in the reader, with its state from [SurahReaderViewModel]. */
@Composable
fun SurahReaderRoute(
    onNavigateBack: () -> Unit,
    onOpenSurah: (Int) -> Unit = {},
    onOpenTranslations: () -> Unit = {},
    viewModel: SurahReaderViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val surahName = state.surah?.englishName.orEmpty()
    val referenceFormat = stringResource(R.string.quran_share_reference)
    val translationCredit = stringResource(R.string.quran_share_translation_credit, state.translator)
    val textCredit = stringResource(R.string.quran_share_text_credit)
    SurahReaderScreen(
        state = state,
        onNavigateBack = onNavigateBack,
        onLoadMore = viewModel::loadMore,
        onScrolled = viewModel::onScrolled,
        onTargetShown = viewModel::onTargetShown,
        onFlashShown = viewModel::onFlashShown,
        onDisplayMode = viewModel::setDisplayMode,
        onSettings = viewModel::updateSettings,
        onOpenSurah = onOpenSurah,
        onOpenTranslations = onOpenTranslations,
        onAyahAction = { ayah, action ->
            val credits = ShareCredits(
                reference = String.format(referenceFormat, surahName, ayah.surah, ayah.number),
                translation = translationCredit,
                text = textCredit
            )
            when (action) {
                AyahAction.COPY_ARABIC -> clipboard.setText(AnnotatedString(AyahShareText.arabic(ayah, credits)))
                AyahAction.COPY_TRANSLATION -> clipboard.setText(AnnotatedString(AyahShareText.translation(ayah, credits)))
                AyahAction.SHARE -> context.startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, AyahShareText.both(ayah, credits)),
                        null
                    )
                )
                AyahAction.BOOKMARK -> viewModel.toggleBookmark(ayah.number)
            }
        }
    )
}

/**
 * The reader: a top bar with the surah's name, what it means and its ayah count, the way back, and
 * the reading settings; then its ayahs, loading more as the player nears the end. All on the page
 * the player chose ([ReaderPaper]): the look's own, or sepia.
 *
 * @param onScrolled the ayah first in view, as the player scrolls.
 * @param onTargetShown [ReaderUiState.target] has been brought into view.
 * @param onFlashShown [ReaderUiState.flash] has been marked for long enough: it fades.
 */
@Composable
fun SurahReaderScreen(
    state: ReaderUiState,
    onNavigateBack: () -> Unit,
    onLoadMore: () -> Unit = {},
    onScrolled: (ayah: Int) -> Unit = {},
    onTargetShown: () -> Unit = {},
    onFlashShown: () -> Unit = {},
    onDisplayMode: (DisplayMode) -> Unit = {},
    onSettings: (ReadingSettings) -> Unit = {},
    onOpenSurah: (Int) -> Unit = {},
    onOpenTranslations: () -> Unit = {},
    onAyahAction: (Ayah, AyahAction) -> Unit = { _, _ -> },
    listState: LazyListState = rememberLazyListState()
) {
    KeepScreenOn(state.settings.keepScreenOn)
    ReaderPage(state.settings.paper) {
        val colors = PixelTheme.colors
        val surah = state.surah
        var showingSettings by rememberSaveable { mutableStateOf(false) }
        // The ayah whose sheet is open, by its number in the surah.
        var acting by rememberSaveable { mutableStateOf<Int?>(null) }
        val snackbars = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val copied = stringResource(R.string.quran_copied)
        state.ayahs.firstOrNull { it.number == acting }?.let { ayah ->
            AyahActionsSheet(
                ayah = ayah,
                surahName = surah?.englishName.orEmpty(),
                bookmarked = ayah.number in state.bookmarked,
                onAction = { action ->
                    acting = null
                    onAyahAction(ayah, action)
                    if (action == AyahAction.COPY_ARABIC || action == AyahAction.COPY_TRANSLATION) {
                        scope.launch { snackbars.showSnackbar(copied) }
                    }
                },
                onDismiss = { acting = null }
            )
        }
        if (showingSettings) {
            ReadingSettingsSheet(
                settings = state.settings,
                preview = state.ayahs.firstOrNull()?.let { it.arabic to it.translation },
                onChange = onSettings,
                onDismiss = { showingSettings = false },
                translation = state.translationName,
                onOpenTranslations = {
                    showingSettings = false
                    onOpenTranslations()
                }
            )
        }
        Scaffold(
            containerColor = colors.background,
            snackbarHost = { SnackbarHost(snackbars) },
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
                        IconButton(onClick = { showingSettings = true }, modifier = Modifier.testTag(ReaderTags.SETTINGS)) {
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
            ReaderEffects(state, listState, onLoadMore, onScrolled, onTargetShown, onFlashShown)
            val typography = state.settings.typography(colors.onBackground)
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
                        item(key = "bismillah", contentType = "bismillah") { BismillahHeader(bismillah = bismillah, typography = typography) }
                    }
                    // One content type for every ayah, so a row scrolled away is reused for the next.
                    items(state.ayahs, key = { it.id }, contentType = { "ayah" }) { ayah ->
                        AyahItem(
                            ayah = ayah,
                            mode = state.displayMode,
                            typography = typography,
                            bookmarked = ayah.number in state.bookmarked,
                            flash = ayah.number == state.flash && state.target == null,
                            onClick = { acting = ayah.number }
                        )
                        HorizontalDivider(modifier = Modifier.padding(top = 16.dp), color = colors.onSurface.copy(alpha = 0.12f))
                    }
                    // The surah's end: on to the next, or back to the one before.
                    if (!state.hasMore && state.ayahs.isNotEmpty()) {
                        item(key = "neighbours", contentType = "neighbours") { SurahNeighbours(previous = state.previous, next = state.next, onOpenSurah = onOpenSurah) }
                    }
                }
            }
        }
    }
}

/**
 * What the reader does as it's scrolled: loads more near the end, says which ayah is first in view,
 * brings the target ayah into view once it's loaded, and lets its flash fade after a moment.
 */
@Composable
private fun ReaderEffects(
    state: ReaderUiState,
    listState: LazyListState,
    onLoadMore: () -> Unit,
    onScrolled: (Int) -> Unit,
    onTargetShown: () -> Unit,
    onFlashShown: () -> Unit
) {
    val current by rememberUpdatedState(state)
    // The list's items are the Bismillah (where the surah has one), then the ayahs.
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .collect { last ->
                val ayahIndex = last?.minus(headerCount(current.surah))
                // Only once there are ayahs: the Bismillah alone isn't the list's end.
                if (ayahIndex != null && current.ayahs.isNotEmpty() && current.hasMore && ayahIndex >= current.ayahs.size - LOAD_AHEAD) onLoadMore()
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
            // The first ayah with the Bismillah above it, as the surah opens; any other at the top.
            listState.scrollToItem(if (index == 0) 0 else index + headerCount(state.surah))
            onTargetShown()
        }
    }
    LaunchedEffect(state.flash, state.target) {
        if (state.flash != null && state.target == null) {
            delay(FLASH_MILLIS)
            onFlashShown()
        }
    }
}

/** How long the ayah the reader was opened at stays marked before it fades. */
internal const val FLASH_MILLIS = 1_500L

/** While [enabled], the screen doesn't dim and sleep: only while the reader is open. */
@Composable
internal fun KeepScreenOn(enabled: Boolean) {
    if (!enabled) return
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

/** How many ayahs before the end of those loaded the next page is asked for. */
private const val LOAD_AHEAD = 10

/** The reader's test tags. */
object ReaderTags {
    const val SCREEN = "quran_reader"
    const val BISMILLAH = "quran_bismillah"
    const val PREVIOUS = "quran_previous_surah"
    const val ACTIONS = "quran_ayah_actions"
    const val RIBBON = "quran_bookmark_ribbon"
    const val NEXT = "quran_next_surah"
    const val LIST = "quran_reader_list"
    const val SETTINGS = "quran_reader_settings"
    const val SETTINGS_SHEET = "quran_reader_settings_sheet"
    const val ARABIC_SIZE = "quran_setting_arabic_size"
    const val TRANSLATION_SIZE = "quran_setting_translation_size"
    const val LINE_SPACING = "quran_setting_line_spacing"
    const val KEEP_SCREEN_ON = "quran_setting_keep_screen_on"

    fun ayah(number: Int) = "quran_ayah_$number"

    fun arabic(number: Int) = "quran_ayah_arabic_$number"

    fun translation(number: Int) = "quran_ayah_translation_$number"

    fun mode(mode: DisplayMode) = "quran_mode_${mode.name.lowercase()}"

    fun paper(paper: ReaderPaper) = "quran_paper_${paper.name.lowercase()}"

    fun action(action: AyahAction) = "quran_action_${action.name.lowercase()}"
}
