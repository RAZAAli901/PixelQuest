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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.R
import com.pixelquest.app.quran.data.BookmarkRepository
import com.pixelquest.app.quran.data.QuranRepository
import com.pixelquest.app.quran.domain.Ayah
import com.pixelquest.app.quran.ui.QuranTypography
import com.pixelquest.app.ui.components.PqGroupLabel
import com.pixelquest.app.ui.components.pqRuledRow
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
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

/** The Quran screen's Saved tab (UI v2, mock 2F): the player's bookmarks, from [BookmarksViewModel]. */
@Composable
fun SavedBookmarks(
    onOpenAyah: (surah: Int, ayah: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookmarksViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    BookmarksList(state = state, onOpenAyah = onOpenAyah, onRemove = viewModel::remove, modifier = modifier)
}

/**
 * The bookmarks, grouped by surah: under each surah's name, its ayahs by number with the start of
 * their translation, as a ruled card. A tap opens the reader there; the cross removes a bookmark.
 * With none, it says how to add one.
 */
@Composable
fun BookmarksList(
    state: BookmarksUiState,
    onOpenAyah: (surah: Int, ayah: Int) -> Unit,
    onRemove: (surah: Int, ayah: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.loaded && state.groups.isEmpty()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = modifier.fillMaxWidth().padding(horizontal = PqDimens.ScreenPad, vertical = 16.dp).testTag(BookmarkTags.SCREEN)
        ) {
            Text(text = stringResource(R.string.quran_bookmarks_none_title), style = PqType.BodyStrong)
            Text(text = stringResource(R.string.quran_bookmarks_none_body), style = PqType.Body.copy(color = PqColors.Ink2))
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 8.dp, bottom = 40.dp),
        modifier = modifier.fillMaxSize().testTag(BookmarkTags.SCREEN)
    ) {
        state.groups.forEach { group ->
            item(key = "surah-${group.surah}") {
                PqGroupLabel(
                    stringResource(R.string.quran_bookmarks_surah, group.surah, group.surahName),
                    Modifier.padding(top = 8.dp, bottom = 6.dp).semantics { heading() }
                )
            }
            itemsIndexed(group.ayahs, key = { _, ayah -> "${ayah.surah}:${ayah.number}" }) { i, ayah ->
                BookmarkRow(
                    ayah = ayah,
                    onOpen = { onOpenAyah(ayah.surah, ayah.number) },
                    onRemove = { onRemove(ayah.surah, ayah.number) },
                    modifier = Modifier.pqRuledRow(first = i == 0, last = i == group.ayahs.lastIndex)
                )
            }
        }
    }
}

/** One bookmark: its ayah's number and the start of its translation, and a cross to remove it. */
@Composable
private fun BookmarkRow(ayah: Ayah, onOpen: () -> Unit, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    val remove = stringResource(R.string.quran_bookmark_remove, ayah.surah, ayah.number)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag(BookmarkTags.row(ayah.surah, ayah.number))
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .weight(1f)
                .clickable(role = Role.Button, onClick = onOpen)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(text = stringResource(R.string.quran_bookmark_ayah, ayah.surah, ayah.number), style = PqType.BodyStrong)
            Text(
                text = ayah.translation,
                style = QuranTypography.snippet(PqColors.Ink2),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag(BookmarkTags.translation(ayah.surah, ayah.number))
            )
        }
        IconButton(onClick = onRemove, modifier = Modifier.testTag(BookmarkTags.remove(ayah.surah, ayah.number)).semantics { contentDescription = remove }) {
            Icon(Icons.Rounded.Close, null, tint = PqColors.Muted)
        }
    }
}

/** The bookmarks screen's test tags. */
object BookmarkTags {
    const val SCREEN = "quran_bookmarks"

    fun row(surah: Int, ayah: Int) = "quran_bookmark_${surah}_$ayah"

    fun remove(surah: Int, ayah: Int) = "quran_bookmark_remove_${surah}_$ayah"

    fun translation(surah: Int, ayah: Int) = "quran_bookmark_translation_${surah}_$ayah"
}
