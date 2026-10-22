package com.pixelquest.app.quran.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.R
import com.pixelquest.app.quran.domain.Revelation
import com.pixelquest.app.quran.domain.Surah
import com.pixelquest.app.quran.ui.QuranTypography
import com.pixelquest.app.ui.components.pqRuledRow
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import com.pixelquest.app.ui.theme.pqShape

/**
 * The 114 surahs, or those [state]'s search finds (UI v2, mock 2F): a search field above one ruled
 * card, a row each. The surah being read is marked.
 *
 * @param listState the list's scroll position, shared with its fast-scroll index.
 */
@Composable
fun SurahList(
    state: SurahListUiState,
    onSearch: (String) -> Unit,
    onOpenSurah: (surah: Int) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    Column(modifier = modifier.fillMaxSize()) {
        SurahSearchField(state.query, onSearch, Modifier.padding(horizontal = PqDimens.ScreenPad, vertical = 8.dp))
        if (state.loaded && state.surahs.isEmpty()) {
            Text(
                text = stringResource(R.string.quran_surah_none),
                style = PqType.Body.copy(color = PqColors.Ink2),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(24.dp),
            )
        }
        // The fast-scroll index beside the whole list; not while a search narrows it.
        val indexed = state.query.isBlank() && state.surahs.size > FAST_SCROLL_MARKS.size
        val current = state.continueReading?.surah?.number
        val surahNumbers = remember(state.surahs) { state.surahs.map { it.number } }
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(start = PqDimens.ScreenPad, end = if (indexed) 44.dp else PqDimens.ScreenPad, bottom = 40.dp),
                modifier = Modifier.fillMaxSize().testTag(QuranListTags.LIST),
            ) {
                itemsIndexed(state.surahs, key = { _, surah -> surah.number }) { i, surah ->
                    SurahRow(
                        surah = surah,
                        onClick = { onOpenSurah(surah.number) },
                        current = surah.number == current,
                        modifier = Modifier.pqRuledRow(first = i == 0, last = i == state.surahs.lastIndex),
                    )
                }
            }
            if (indexed) {
                FastScrollIndex(
                    listState = listState,
                    surahs = surahNumbers,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp, bottom = 16.dp),
                )
            }
        }
    }
}

/** The search over the surahs' names and numbers, as the player types. */
@Composable
private fun SurahSearchField(query: String, onSearch: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().height(46.dp).clip(pqShape(12.dp)).background(PqColors.Paper)
            .border(PqDimens.Border, PqColors.Ink, pqShape(12.dp)).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, null, tint = PqColors.Muted, modifier = Modifier.size(22.dp))
        Box(Modifier.weight(1f).padding(start = 8.dp)) {
            if (query.isEmpty()) Text(stringResource(R.string.quran_surah_search), style = PqType.Body.copy(color = PqColors.Muted))
            BasicTextField(
                query,
                onSearch,
                singleLine = true,
                textStyle = PqType.Body,
                cursorBrush = SolidColor(PqColors.Ink),
                modifier = Modifier.fillMaxWidth().testTag(QuranListTags.SEARCH),
            )
        }
    }
}

/**
 * One surah: its number in a diamond, its English name, what it means, where it was revealed and how
 * many ayahs it has, and its Arabic name, in Amiri Quran. One button to TalkBack, read in English:
 * "2. Al-Baqara, The Cow. Medinan, 286 ayahs".
 *
 * @param current the surah the player is reading, its diamond filled.
 */
@Composable
fun SurahRow(surah: Surah, onClick: () -> Unit, modifier: Modifier = Modifier, current: Boolean = false) {
    val place = stringResource(if (surah.revelation == Revelation.MEDINAN) R.string.quran_medinan else R.string.quran_meccan)
    val count = pluralStringResource(R.plurals.quran_ayah_count, surah.ayahCount, surah.ayahCount)
    val spoken = stringResource(R.string.quran_surah_spoken, surah.number, surah.englishName, surah.englishMeaning, place, count)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(role = Role.Button, onClick = onClick)
            // The tag before the clearing: anything after it is cleared too.
            .testTag(QuranListTags.row(surah.number))
            .clearAndSetSemantics { contentDescription = spoken }
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Box(
            Modifier.padding(4.dp).size(30.dp).rotate(45f).clip(pqShape(8.dp))
                .background(if (current) PqColors.Lilac else PqColors.Paper).border(PqDimens.BorderThin, PqColors.Ink, pqShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) { Text(surah.number.toString(), style = PqType.Button.copy(letterSpacing = 0.sp), modifier = Modifier.rotate(-45f)) }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = surah.englishName, style = PqType.BodyStrong)
            Text(
                text = stringResource(R.string.quran_surah_line, surah.englishMeaning, place, count),
                style = PqType.Meta,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = surah.arabicName,
            style = QuranTypography.of(arabicSize = QuranTypography.ARABIC_MIN, color = PqColors.Ink).arabic,
            maxLines = 1,
        )
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
