package com.pixelquest.app.quran.ui.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.R
import com.pixelquest.app.quran.data.QuranAssetSource
import com.pixelquest.app.quran.data.QuranRepository
import com.pixelquest.app.quran.data.QuranSources
import com.pixelquest.app.quran.domain.Translation
import com.pixelquest.app.quran.ui.QuranPanel
import com.pixelquest.app.ui.components.PixelTopAppBar
import com.pixelquest.app.ui.components.SymbolIcon
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.inkOnPanel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

/**
 * The About and Sources screen's state.
 *
 * @param sources the bundle's own statement of its sources; null until read.
 * @param translation the translation on the phone, with its credit, terms and notice.
 */
data class QuranSourcesUiState(val sources: QuranSources? = null, val translation: Translation? = null)

/** Where the Quran in the app comes from: read from its own files, and the translation's from the database. */
@HiltViewModel
class QuranSourcesViewModel @Inject constructor(
    assets: QuranAssetSource,
    repository: QuranRepository
) : ViewModel() {

    private val sources = MutableStateFlow<QuranSources?>(null)

    val state: StateFlow<QuranSourcesUiState> = combine(sources, repository.translations()) { bundle, translations ->
        QuranSourcesUiState(bundle, translations.firstOrNull())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuranSourcesUiState())

    init {
        viewModelScope.launch { sources.value = withContext(Dispatchers.IO) { QuranSources.read(assets) } }
    }
}

@Composable
fun QuranSourcesRoute(onNavigateBack: () -> Unit, viewModel: QuranSourcesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    QuranSourcesScreen(state = state, onNavigateBack = onNavigateBack)
}

/**
 * About and Sources: where the Quran's text, its surah data, its translation and its typeface come
 * from, each with its credit, its terms, and links to its source; the sources' own notices in full,
 * as their terms ask; and when the files were downloaded.
 */
@Composable
fun QuranSourcesScreen(state: QuranSourcesUiState, onNavigateBack: () -> Unit) {
    val colors = PixelTheme.colors
    Scaffold(
        containerColor = colors.background,
        topBar = {
            PixelTopAppBar(
                title = stringResource(R.string.quran_sources_title),
                navigationIcon = {
                    com.pixelquest.app.ui.components.PqBackButton(onNavigateBack)
                }
            )
        },
        modifier = Modifier.testTag(SourcesTags.SCREEN)
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Body(stringResource(R.string.quran_sources_intro))
            Section(stringResource(R.string.quran_sources_text_title), SourcesTags.TEXT) {
                Body(stringResource(R.string.quran_sources_text_body))
                Link(stringResource(R.string.quran_sources_link_tanzil), TANZIL)
                Link(stringResource(R.string.quran_sources_link_updates), TANZIL_UPDATES)
                state.sources?.let { Notice(it.notice) }
            }
            Section(stringResource(R.string.quran_sources_metadata_title), SourcesTags.METADATA) {
                state.sources?.let { Body(it.metadataNotice) }
                Link(stringResource(R.string.quran_sources_link_metadata), TANZIL_METADATA)
            }
            state.translation?.let { translation ->
                Section(stringResource(R.string.quran_sources_translation_title), SourcesTags.TRANSLATION) {
                    Body(stringResource(R.string.quran_sources_translation_body, translation.name, translation.translator, translation.language))
                    Body(stringResource(R.string.quran_translation_updated, translation.lastUpdate))
                    Body(translation.licenceNote)
                    Link(stringResource(R.string.quran_sources_link_translations), TANZIL_TRANSLATIONS)
                    Notice(translation.notice)
                }
            }
            Section(stringResource(R.string.quran_sources_font_title), SourcesTags.FONT) {
                Body(stringResource(R.string.quran_sources_font_body))
                Link(stringResource(R.string.quran_sources_link_amiri), AMIRI)
                Link(stringResource(R.string.quran_sources_link_ofl), OFL)
            }
            Section(stringResource(R.string.quran_sources_app_title), SourcesTags.APP) {
                val downloaded = LocalDate.parse(QuranSources.DOWNLOADED).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
                Body(stringResource(R.string.quran_sources_downloaded, downloaded))
                state.sources?.let { Body(stringResource(R.string.quran_sources_version, it.version)) }
            }
        }
    }
}

@Composable
private fun Section(title: String, tag: String, content: @Composable () -> Unit) {
    QuranPanel(contentPadding = 14.dp, modifier = Modifier.fillMaxWidth().testTag(tag)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = inkOnPanel(PixelTheme.colors.primaryText),
                modifier = Modifier.semantics { heading() }
            )
            content()
        }
    }
}

@Composable
private fun Body(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = PixelTheme.colors.onSurface)
}

/** A source's notice, whole and exactly as it is, in a typewriter face as in its file. */
@Composable
private fun Notice(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        color = PixelTheme.colors.onSurfaceVariant
    )
}

/** A link to a source: opens it in the browser. */
@Composable
private fun Link(label: String, url: String) {
    val uriHandler = LocalUriHandler.current
    Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.Underline),
        color = PixelTheme.colors.primaryText,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.quran_sources_open_link)) {
                // A phone without a browser just doesn't open it.
                runCatching { uriHandler.openUri(url) }
            }
            .padding(vertical = 12.dp)
            .testTag(SourcesTags.link(url)),
    )
}

private const val TANZIL = "https://tanzil.net"
private const val TANZIL_UPDATES = "https://tanzil.net/updates/"
private const val TANZIL_METADATA = "https://tanzil.net/docs/Quran_Metadata"
private const val TANZIL_TRANSLATIONS = "https://tanzil.net/trans/"
private const val AMIRI = "https://github.com/aliftype/amiri"
private const val OFL = "https://openfontlicense.org"

/** The About and Sources screen's test tags. */
object SourcesTags {
    const val SCREEN = "quran_sources"
    const val TEXT = "quran_sources_text"
    const val METADATA = "quran_sources_metadata"
    const val TRANSLATION = "quran_sources_translation"
    const val FONT = "quran_sources_font"
    const val APP = "quran_sources_app"

    /** The Quran screen's top-bar action that opens it. */
    const val ACTION = "quran_sources_action"

    fun link(url: String) = "quran_sources_link_$url"
}
