package com.pixelquest.app.quran.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.R
import com.pixelquest.app.quran.ui.list.SurahList
import com.pixelquest.app.quran.ui.list.SurahListUiState
import com.pixelquest.app.quran.ui.list.SurahListViewModel
import com.pixelquest.app.ui.components.PixelProgressBar
import com.pixelquest.app.ui.components.PixelTopAppBar
import com.pixelquest.app.ui.components.SymbolIcon
import com.pixelquest.app.ui.theme.PixelTheme

/** The Quran's screen, with its state from [QuranViewModel] and [SurahListViewModel]. */
@Composable
fun QuranRoute(
    onNavigateBack: () -> Unit,
    onOpenSurah: (surah: Int) -> Unit = {},
    viewModel: QuranViewModel = hiltViewModel(),
    listViewModel: SurahListViewModel = hiltViewModel()
) {
    val preparing by viewModel.preparing.collectAsState()
    val list by listViewModel.state.collectAsState()
    QuranHomeScreen(
        onNavigateBack = onNavigateBack,
        preparing = preparing,
        list = list,
        onSearch = listViewModel::search,
        onOpenSurah = onOpenSurah
    )
}

/**
 * The Quran's home: the 114 surahs, searchable as the player types. The first time, while the
 * Quran is being put on the phone, it says so instead ([preparing]).
 */
@Composable
fun QuranHomeScreen(
    onNavigateBack: () -> Unit,
    preparing: QuranPreparing? = null,
    list: SurahListUiState = SurahListUiState(),
    onSearch: (String) -> Unit = {},
    onOpenSurah: (surah: Int) -> Unit = {}
) {
    val colors = PixelTheme.colors
    Scaffold(
        containerColor = colors.background,
        topBar = {
            PixelTopAppBar(
                title = stringResource(R.string.quran_title),
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
        },
        modifier = Modifier.testTag(QURAN_HOME)
    ) { padding ->
        if (preparing != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp)
            ) {
                Text(text = "📖", fontSize = 48.sp, modifier = Modifier.clearAndSetSemantics {})
                Preparing(preparing)
            }
        } else {
            SurahList(
                state = list,
                onSearch = onSearch,
                onOpenSurah = onOpenSurah,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

/**
 * The one-time message while the Quran is put on the phone: what's happening, and how far it's got
 * (a bar TalkBack reads as a percentage).
 */
@Composable
private fun Preparing(preparing: QuranPreparing) {
    val colors = PixelTheme.colors
    Text(
        text = stringResource(if (preparing.failed) R.string.quran_preparing_failed_title else R.string.quran_preparing_title),
        style = MaterialTheme.typography.titleMedium,
        color = colors.primaryText,
        textAlign = TextAlign.Center,
        modifier = Modifier.semantics { heading() }.testTag(QURAN_PREPARING)
    )
    Text(
        text = stringResource(if (preparing.failed) R.string.quran_preparing_failed_body else R.string.quran_preparing_body),
        style = MaterialTheme.typography.bodyMedium,
        color = colors.onSurface,
        textAlign = TextAlign.Center
    )
    if (!preparing.failed) {
        val progress = preparing.progress ?: 0f
        PixelProgressBar(
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
