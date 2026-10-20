package com.pixelquest.app.quran.ui.bookmarks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.R
import com.pixelquest.app.quran.ui.framed
import com.pixelquest.app.quran.ui.quranFrame
import com.pixelquest.app.quran.data.BookmarkRepository
import com.pixelquest.app.quran.data.QuranRepository
import com.pixelquest.app.quran.domain.Ayah
import com.pixelquest.app.quran.ui.QuranTypography
import com.pixelquest.app.ui.components.PixelTopAppBar
import com.pixelquest.app.ui.components.SymbolIcon
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.inkOnPanel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One surah's bookmarks: its number and name, and its bookmarked ayahs in order. */
data class BookmarkGroup(val surah: Int, val surahName: String, val ayahs: List<Ayah>)

/**
 * The bookmarks screen's state.
 *
 * @param loaded false until the bookmarks first arrive, so "none yet" doesn't flash.
 */
data class BookmarksUiState(val groups: List<BookmarkGroup> = emptyList(), val loaded: Boolean = false)

/** The player's bookmarks, grouped by surah, each ayah with its text. */
@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val bookmarks: BookmarkRepository,
    private val quran: QuranRepository
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<BookmarksUiState> = bookmarks.all().mapLatest { marks ->
        val groups = marks.groupBy { it.first }.mapNotNull { (surah, ayahs) ->
            val info = quran.surah(surah) ?: return@mapNotNull null
            BookmarkGroup(surah, info.englishName, ayahs.mapNotNull { (_, ayah) -> quran.ayah(surah, ayah) })
        }
        BookmarksUiState(groups, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BookmarksUiState())

    fun remove(surah: Int, ayah: Int) {
        viewModelScope.launch { bookmarks.toggle(surah, ayah) }
    }
}

@Composable
fun BookmarksRoute(
    onNavigateBack: () -> Unit,
    onOpenAyah: (surah: Int, ayah: Int) -> Unit,
    viewModel: BookmarksViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    BookmarksScreen(state = state, onNavigateBack = onNavigateBack, onOpenAyah = onOpenAyah, onRemove = viewModel::remove)
}

/**
 * The bookmarks, grouped by surah: under each surah's name, its ayahs by number with the start of
 * their translation. A tap opens the reader there; the cross removes a bookmark. With none, it says
 * how to add one.
 */
@Composable
fun BookmarksScreen(
    state: BookmarksUiState,
    onNavigateBack: () -> Unit,
    onOpenAyah: (surah: Int, ayah: Int) -> Unit,
    onRemove: (surah: Int, ayah: Int) -> Unit
) {
    val colors = PixelTheme.colors
    Scaffold(
        containerColor = colors.background,
        topBar = {
            PixelTopAppBar(
                title = stringResource(R.string.quran_bookmarks),
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
        modifier = Modifier.testTag(BookmarkTags.SCREEN)
    ) { padding ->
        if (state.loaded && state.groups.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp)
            ) {
                Text(text = stringResource(R.string.quran_bookmarks_none_title), style = MaterialTheme.typography.titleMedium, color = colors.primaryText, textAlign = TextAlign.Center)
                Text(text = stringResource(R.string.quran_bookmarks_none_body), style = MaterialTheme.typography.bodyMedium, color = colors.onSurface, textAlign = TextAlign.Center)
            }
            return@Scaffold
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            state.groups.forEach { group ->
                item(key = "surah-${group.surah}") {
                    Text(
                        text = stringResource(R.string.quran_bookmarks_surah, group.surah, group.surahName),
                        style = MaterialTheme.typography.titleSmall,
                        color = inkOnPanel(colors.primaryText),
                        modifier = Modifier.padding(top = 8.dp).semantics { heading() }
                    )
                }
                items(group.ayahs, key = { "${it.surah}:${it.number}" }) { ayah ->
                    BookmarkRow(ayah = ayah, onOpen = { onOpenAyah(ayah.surah, ayah.number) }, onRemove = { onRemove(ayah.surah, ayah.number) })
                }
            }
        }
    }
}

/** One bookmark: its ayah's number and the start of its translation, and a cross to remove it. */
@Composable
private fun BookmarkRow(ayah: Ayah, onOpen: () -> Unit, onRemove: () -> Unit) {
    val colors = PixelTheme.colors
    val remove = stringResource(R.string.quran_bookmark_remove, ayah.surah, ayah.number)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .framed(quranFrame())
            .testTag(BookmarkTags.row(ayah.surah, ayah.number))
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .weight(1f)
                .clickable(role = Role.Button, onClick = onOpen)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.quran_bookmark_ayah, ayah.surah, ayah.number),
                style = MaterialTheme.typography.labelLarge,
                color = colors.primaryText
            )
            Text(
                text = ayah.translation,
                style = QuranTypography.snippet(colors.onSurface),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag(BookmarkTags.translation(ayah.surah, ayah.number))
            )
        }
        IconButton(onClick = onRemove, modifier = Modifier.testTag(BookmarkTags.remove(ayah.surah, ayah.number)).semantics { contentDescription = remove }) {
            Text(text = "✕", style = MaterialTheme.typography.titleMedium, color = colors.onSurfaceVariant)
        }
    }
}

/** The bookmarks screen's test tags. */
object BookmarkTags {
    const val SCREEN = "quran_bookmarks"

    /** The Quran screen's top-bar action that opens it. */
    const val ACTION = "quran_bookmarks_action"

    fun row(surah: Int, ayah: Int) = "quran_bookmark_${surah}_$ayah"

    fun remove(surah: Int, ayah: Int) = "quran_bookmark_remove_${surah}_$ayah"

    fun translation(surah: Int, ayah: Int) = "quran_bookmark_translation_${surah}_$ayah"
}
