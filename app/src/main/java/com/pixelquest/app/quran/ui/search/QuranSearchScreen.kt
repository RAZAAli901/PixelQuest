package com.pixelquest.app.quran.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.R
import com.pixelquest.app.quran.ui.framed
import com.pixelquest.app.quran.ui.quranFrame
import com.pixelquest.app.quran.ui.QuranTypography
import com.pixelquest.app.quran.ui.list.SurahRow
import com.pixelquest.app.quran.ui.reader.RightToLeft
import com.pixelquest.app.ui.components.PixelTextField
import com.pixelquest.app.ui.components.PixelTopAppBar
import com.pixelquest.app.ui.components.SymbolIcon
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.inkOnPanel

@Composable
fun QuranSearchRoute(
    onNavigateBack: () -> Unit,
    onOpenSurah: (surah: Int) -> Unit,
    onOpenAyah: (surah: Int, ayah: Int) -> Unit,
    viewModel: QuranSearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsState()
    val state by viewModel.state.collectAsState()
    QuranSearchScreen(
        query = query,
        state = state,
        onSearch = viewModel::search,
        onNavigateBack = onNavigateBack,
        onOpenSurah = onOpenSurah,
        onOpenAyah = onOpenAyah
    )
}

/**
 * Searching the Quran: a field, then what it found. Surahs first, by number or name; then ayahs,
 * each with its reference and its translation, the matched words marked. Before anything's typed,
 * it says what can be searched.
 *
 * @param query the field's text; [state] may still be for an earlier one.
 */
@Composable
fun QuranSearchScreen(
    query: String,
    state: QuranSearchUiState,
    onSearch: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onOpenSurah: (surah: Int) -> Unit,
    onOpenAyah: (surah: Int, ayah: Int) -> Unit,
    focusOnOpen: Boolean = true
) {
    val colors = PixelTheme.colors
    val focus = remember { FocusRequester() }
    if (focusOnOpen) LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Scaffold(
        containerColor = colors.background,
        topBar = {
            PixelTopAppBar(
                title = stringResource(R.string.quran_search_title),
                navigationIcon = {
                    com.pixelquest.app.ui.components.PqBackButton(onNavigateBack)
                }
            )
        },
        modifier = Modifier.testTag(SearchTags.SCREEN)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            PixelTextField(
                value = query,
                onValueChange = onSearch,
                placeholder = stringResource(R.string.quran_search_placeholder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .focusRequester(focus)
                    .testTag(SearchTags.FIELD)
            )
            when {
                query.isBlank() -> Note(stringResource(R.string.quran_search_hint))
                state.query == query && state.isEmpty -> Note(stringResource(R.string.quran_search_none, query.trim()))
                else -> Results(state, onOpenSurah, onOpenAyah)
            }
        }
    }
}

@Composable
private fun Note(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = PixelTheme.colors.onSurface,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(24.dp).testTag(SearchTags.NOTE)
    )
}

@Composable
private fun Results(
    state: QuranSearchUiState,
    onOpenSurah: (surah: Int) -> Unit,
    onOpenAyah: (surah: Int, ayah: Int) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize().testTag(SearchTags.RESULTS)
    ) {
        if (state.surahs.isNotEmpty()) {
            item(key = "surahs") { Heading(pluralStringResource(R.plurals.quran_search_surahs, state.surahs.size, state.surahs.size)) }
            items(state.surahs, key = { "surah-${it.number}" }) { surah ->
                SurahRow(surah = surah, onClick = { onOpenSurah(surah.number) })
            }
        }
        if (state.ayahs.isNotEmpty()) {
            item(key = "ayahs") {
                Heading(
                    if (state.capped) {
                        pluralStringResource(R.plurals.quran_search_ayahs_capped, state.ayahs.size, state.ayahs.size)
                    } else {
                        pluralStringResource(R.plurals.quran_search_ayahs, state.ayahs.size, state.ayahs.size)
                    }
                )
            }
            items(state.ayahs, key = { "ayah-${it.ayah.id}" }) { result ->
                AyahResultRow(
                    result = result,
                    query = state.query,
                    showArabic = state.arabic,
                    onClick = { onOpenAyah(result.ayah.surah, result.ayah.number) }
                )
            }
        }
    }
}

@Composable
private fun Heading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = inkOnPanel(PixelTheme.colors.primaryText),
        modifier = Modifier.padding(top = 8.dp).semantics { heading() }
    )
}

/**
 * An ayah found: its reference, and its translation with the matched words marked. Found by its
 * Arabic, the Arabic comes first, as the Quran's text is. One button.
 */
@Composable
private fun AyahResultRow(result: AyahResult, query: String, showArabic: Boolean, onClick: () -> Unit) {
    val colors = PixelTheme.colors
    val marked = SpanStyle(fontWeight = FontWeight.Bold, background = colors.primary.copy(alpha = 0.25f))
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .framed(quranFrame())
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(SearchTags.ayah(result.ayah.surah, result.ayah.number))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = stringResource(R.string.quran_search_reference, result.surahName, result.ayah.surah, result.ayah.number),
            style = MaterialTheme.typography.labelLarge,
            color = colors.primaryText
        )
        if (showArabic) {
            RightToLeft {
                Text(
                    text = result.ayah.arabic,
                    style = QuranTypography.of(arabicSize = QuranTypography.ARABIC_MIN, color = colors.onSurface).arabic,
                    modifier = Modifier.fillMaxWidth().testTag(SearchTags.arabic(result.ayah.surah, result.ayah.number))
                )
            }
        }
        Text(
            text = SearchHighlight.annotate(result.ayah.translation, query, marked),
            style = QuranTypography.snippet(colors.onSurface),
            modifier = Modifier.testTag(SearchTags.translation(result.ayah.surah, result.ayah.number))
        )
    }
}

/** The search screen's test tags. */
object SearchTags {
    const val SCREEN = "quran_search"
    const val FIELD = "quran_search_field"
    const val NOTE = "quran_search_note"
    const val RESULTS = "quran_search_results"

    /** The Quran screen's top-bar action that opens it. */
    const val ACTION = "quran_search_action"

    fun ayah(surah: Int, ayah: Int) = "quran_search_ayah_${surah}_$ayah"

    fun arabic(surah: Int, ayah: Int) = "quran_search_arabic_${surah}_$ayah"

    fun translation(surah: Int, ayah: Int) = "quran_search_translation_${surah}_$ayah"
}
