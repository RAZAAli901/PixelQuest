package com.pixelquest.app.quran.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.R
import com.pixelquest.app.quran.ui.about.SourcesTags
import com.pixelquest.app.quran.ui.list.ContinueReadingCard
import com.pixelquest.app.quran.ui.list.GoToAyahDialog
import com.pixelquest.app.quran.ui.list.GoToTags
import com.pixelquest.app.quran.ui.list.JuzList
import com.pixelquest.app.quran.ui.list.QuranTab
import com.pixelquest.app.quran.ui.list.QuranTabs
import com.pixelquest.app.quran.ui.list.SurahList
import com.pixelquest.app.quran.ui.list.SurahListUiState
import com.pixelquest.app.quran.ui.list.SurahListViewModel
import com.pixelquest.app.quran.ui.search.SearchTags
import com.pixelquest.app.ui.components.PqIconButton
import com.pixelquest.app.ui.components.PqProgressBar
import com.pixelquest.app.ui.components.PqTopBar
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType

/** The Quran's screen, with its state from [QuranViewModel] and [SurahListViewModel]; its Saved tab from the bookmarks'. */
@Composable
fun QuranRoute(
    onNavigateBack: () -> Unit,
    onOpenSurah: (surah: Int) -> Unit = {},
    onOpenAyah: (surah: Int, ayah: Int) -> Unit = { surah, _ -> onOpenSurah(surah) },
    onOpenSearch: () -> Unit = {},
    onOpenSources: () -> Unit = {},
    viewModel: QuranViewModel = hiltViewModel(),
    listViewModel: SurahListViewModel = hiltViewModel(),
    saved: @Composable () -> Unit = { com.pixelquest.app.quran.ui.bookmarks.SavedBookmarks(onOpenAyah = onOpenAyah) }
) {
    val preparing by viewModel.preparing.collectAsState()
    val list by listViewModel.state.collectAsState()
    QuranHomeScreen(
        onNavigateBack = onNavigateBack,
        preparing = preparing,
        list = list,
        onSearch = listViewModel::search,
        onOpenSurah = onOpenSurah,
        onOpenAyah = onOpenAyah,
        onOpenSearch = onOpenSearch,
        onOpenSources = onOpenSources,
        saved = saved
    )
}

/**
 * The Quran's home (UI v2, mock 2F): a card to carry on from the last place read, then the surahs
 * (searchable as the player types), the juz, or the ayahs saved. The top bar searches the whole
 * Quran, goes to a surah and ayah, and says where the text comes from. The first time, while the
 * Quran is being put on the phone, it says so instead ([preparing]).
 *
 * @param saved the Saved tab's list; a slot, as it reads its own ViewModel.
 */
@Composable
fun QuranHomeScreen(
    onNavigateBack: () -> Unit,
    preparing: QuranPreparing? = null,
    list: SurahListUiState = SurahListUiState(),
    onSearch: (String) -> Unit = {},
    onOpenSurah: (surah: Int) -> Unit = {},
    onOpenAyah: (surah: Int, ayah: Int) -> Unit = { surah, _ -> onOpenSurah(surah) },
    onOpenSearch: () -> Unit = {},
    onOpenSources: () -> Unit = {},
    saved: @Composable () -> Unit = {}
) {
    var goingTo by rememberSaveable { mutableStateOf(false) }
    if (goingTo) {
        GoToAyahDialog(
            ayahCounts = list.ayahCounts,
            onGo = { surah, ayah -> goingTo = false; onOpenAyah(surah, ayah) },
            onDismiss = { goingTo = false }
        )
    }
    Column(Modifier.fillMaxSize().background(PqColors.Ground).testTag(QURAN_HOME)) {
        PqTopBar(stringResource(R.string.quran_title), onBack = onNavigateBack, modifier = Modifier.padding(horizontal = PqDimens.ScreenPad)) {
            if (preparing == null && list.loaded) {
                PqIconButton(Icons.Rounded.Search, stringResource(R.string.quran_search_title), onOpenSearch, Modifier.testTag(SearchTags.ACTION))
                PqIconButton(Icons.Rounded.Tag, stringResource(R.string.quran_go_title), { goingTo = true }, Modifier.testTag(GoToTags.ACTION))
                PqIconButton(Icons.Outlined.Info, stringResource(R.string.quran_sources_title), onOpenSources, Modifier.testTag(SourcesTags.ACTION))
            }
        }
        if (preparing != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                modifier = Modifier.fillMaxSize().padding(24.dp)
            ) {
                Icon(Icons.Rounded.MenuBook, null, tint = PqColors.Ink, modifier = Modifier.size(48.dp))
                Preparing(preparing)
            }
        } else {
            var tab by rememberSaveable { mutableStateOf(QuranTab.SURAHS) }
            Column(modifier = Modifier.fillMaxSize()) {
                com.pixelquest.app.ui.tips.FirstRunTip(com.pixelquest.app.ui.tips.Tip.QURAN, Modifier.padding(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 8.dp))
                list.continueReading?.let { place ->
                    ContinueReadingCard(
                        place = place,
                        onOpen = { onOpenAyah(place.surah.number, place.ayah) },
                        modifier = Modifier.padding(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 8.dp)
                    )
                }
                QuranTabs(selected = tab, onSelect = { tab = it }, modifier = Modifier.padding(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 12.dp, bottom = 4.dp))
                when (tab) {
                    QuranTab.SURAHS -> SurahList(state = list, onSearch = onSearch, onOpenSurah = onOpenSurah)
                    QuranTab.JUZ -> JuzList(juzs = list.juzs, onOpenAyah = onOpenAyah)
                    QuranTab.SAVED -> saved()
                }
            }
        }
    }
}

/**
 * The one-time message while the Quran is put on the phone: what's happening, and how far it's got
 * (a bar TalkBack reads as a percentage).
 */
@Composable
private fun Preparing(preparing: QuranPreparing) {
    Text(
        text = stringResource(if (preparing.failed) R.string.quran_preparing_failed_title else R.string.quran_preparing_title),
        style = PqType.CardTitle,
        textAlign = TextAlign.Center,
        modifier = Modifier.semantics { heading() }.testTag(QURAN_PREPARING)
    )
    Text(
        text = stringResource(if (preparing.failed) R.string.quran_preparing_failed_body else R.string.quran_preparing_body),
        style = PqType.Body.copy(color = PqColors.Ink2),
        textAlign = TextAlign.Center
    )
    if (!preparing.failed) {
        val progress = preparing.progress ?: 0f
        PqProgressBar(
            progress = progress,
            height = 10.dp,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f) }
        )
    }
}

/** The Quran home's test tag. */
const val QURAN_HOME = "quran_home"

/** The preparing message's test tag. */
const val QURAN_PREPARING = "quran_preparing"
