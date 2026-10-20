package com.pixelquest.app.quran.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.quran.domain.Revelation
import com.pixelquest.app.quran.domain.Surah
import com.pixelquest.app.quran.ui.QuranTypography
import com.pixelquest.app.ui.components.PixelTextField
import com.pixelquest.app.ui.theme.ComicShapeTokens
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.ThemeMode
import com.pixelquest.app.ui.theme.inkOnPanel

/**
 * The 114 surahs, or those [state]'s search finds: a search field above a list, a row each.
 *
 * @param listState the list's scroll position, shared with its fast-scroll index.
 */
@Composable
fun SurahList(
    state: SurahListUiState,
    onSearch: (String) -> Unit,
    onOpenSurah: (surah: Int) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState()
) {
    Column(modifier = modifier.fillMaxSize()) {
        PixelTextField(
            value = state.query,
            onValueChange = onSearch,
            placeholder = stringResource(R.string.quran_surah_search),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag(QuranListTags.SEARCH)
        )
        if (state.loaded && state.surahs.isEmpty()) {
            Text(
                text = stringResource(R.string.quran_surah_none),
                style = MaterialTheme.typography.bodyMedium,
                color = PixelTheme.colors.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(24.dp)
            )
        }
        // The fast-scroll index beside the whole list; not while a search narrows it.
        val indexed = state.query.isBlank() && state.surahs.size > FAST_SCROLL_MARKS.size
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(start = 16.dp, end = if (indexed) 44.dp else 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize().testTag(QuranListTags.LIST)
            ) {
                items(state.surahs, key = { it.number }) { surah ->
                    SurahRow(surah = surah, onClick = { onOpenSurah(surah.number) })
                }
            }
            if (indexed) {
                FastScrollIndex(
                    listState = listState,
                    surahs = state.surahs.map { it.number },
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp, bottom = 16.dp)
                )
            }
        }
    }
}

/**
 * One surah: its number in a badge, its English name and what it means, where it was revealed and
 * how many ayahs it has, and its Arabic name, in Amiri Quran. One button to TalkBack, read in
 * English: "2. Al-Baqara, The Cow. Medinan, 286 ayahs".
 */
@Composable
fun SurahRow(surah: Surah, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = PixelTheme.colors
    val place = stringResource(if (surah.revelation == Revelation.MEDINAN) R.string.quran_medinan else R.string.quran_meccan)
    val count = pluralStringResource(R.plurals.quran_ayah_count, surah.ayahCount, surah.ayahCount)
    val spoken = stringResource(R.string.quran_surah_spoken, surah.number, surah.englishName, surah.englishMeaning, place, count)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .background(colors.surface, MaterialTheme.shapes.small)
            .clickable(role = Role.Button, onClick = onClick)
            // The tag before the clearing: anything after it is cleared too.
            .testTag(QuranListTags.row(surah.number))
            .clearAndSetSemantics { contentDescription = spoken }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        SurahNumberBadge(surah.number)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = surah.englishName, style = MaterialTheme.typography.titleSmall, color = inkOnPanel(colors.primaryText))
            Text(text = surah.englishMeaning, style = MaterialTheme.typography.bodySmall, color = colors.onSurface)
            Text(text = "$place · $count", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = surah.arabicName,
            style = QuranTypography.of(arabicSize = QuranTypography.ARABIC_MIN, color = colors.onSurface).arabic,
            maxLines = 1
        )
    }
}

/** A surah's number, in a badge shaped by the look: chamfered in Pixel, an inked circle in Comic, a circle elsewhere. */
@Composable
private fun SurahNumberBadge(number: Int) {
    val colors = PixelTheme.colors
    val shape: Shape = when (PixelTheme.mode) {
        ThemeMode.Pixel, ThemeMode.System -> CutCornerShape(6.dp)
        else -> CircleShape
    }
    val ink = if (PixelTheme.mode == ThemeMode.Comic) {
        Modifier.border(ComicShapeTokens.BorderWidthThin, ComicTokens.SolidBlack, shape)
    } else {
        Modifier
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .background(colors.primary.copy(alpha = 0.15f), shape)
            .then(ink)
    ) {
        Text(text = number.toString(), style = MaterialTheme.typography.labelLarge, color = inkOnPanel(colors.primaryText))
    }
}

/** The surah list's test tags. */
object QuranListTags {
    const val SEARCH = "quran_surah_search"
    const val LIST = "quran_surah_list"
    const val JUZ_LIST = "quran_juz_list"

    fun row(surah: Int) = "quran_surah_$surah"

    fun juz(number: Int) = "quran_juz_$number"

    fun tab(tab: QuranTab) = "quran_tab_${tab.name.lowercase()}"
}
