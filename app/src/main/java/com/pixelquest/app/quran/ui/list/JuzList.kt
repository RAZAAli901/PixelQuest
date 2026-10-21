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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.R
import com.pixelquest.app.quran.domain.Juz
import com.pixelquest.app.ui.components.pqRuledRow
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import com.pixelquest.app.ui.theme.pqShape

/** The Quran screen's three lists (UI v2, mock 2F): the surahs, the juz, and the ayahs saved. */
enum class QuranTab { SURAHS, JUZ, SAVED }

/**
 * Surahs, Juz or Saved: one segmented control, the chosen one inked. TalkBack hears them as tabs,
 * the chosen one selected; each is a full-size target.
 */
@Composable
fun QuranTabs(selected: QuranTab, onSelect: (QuranTab) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(pqShape(12.dp)).background(PqColors.Paper)
            .border(PqDimens.Border, PqColors.Ink, pqShape(12.dp)).padding(3.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        QuranTab.entries.forEach { tab ->
            val chosen = tab == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = com.pixelquest.app.quran.ui.reader.MIN_TARGET)
                    .clip(pqShape(9.dp))
                    .background(if (chosen) PqColors.Ink else Color.Transparent)
                    .selectable(selected = chosen, role = Role.Tab, onClick = { onSelect(tab) })
                    .testTag(QuranListTags.tab(tab))
            ) {
                Text(
                    text = stringResource(
                        when (tab) {
                            QuranTab.SURAHS -> R.string.quran_tab_surahs
                            QuranTab.JUZ -> R.string.quran_tab_juz
                            QuranTab.SAVED -> R.string.quran_tab_saved
                        }
                    ),
                    style = PqType.Button.copy(letterSpacing = 0.sp, color = if (chosen) PqColors.Paper else PqColors.Ink)
                )
            }
        }
    }
}

/**
 * The 30 juz, a row each of one ruled card: its number, and the surah and ayah it starts at. A tap
 * opens the reader there. One button to TalkBack: "Juz 2, from Al-Baqara, ayah 142".
 */
@Composable
fun JuzList(juzs: List<Juz>, onOpenAyah: (surah: Int, ayah: Int) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(
        contentPadding = PaddingValues(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 8.dp, bottom = 40.dp),
        modifier = modifier.fillMaxSize().testTag(QuranListTags.JUZ_LIST)
    ) {
        itemsIndexed(juzs, key = { _, juz -> juz.number }) { i, juz ->
            val title = stringResource(R.string.quran_juz_title, juz.number)
            val from = stringResource(R.string.quran_juz_from, juz.surahName, juz.surah, juz.ayah)
            val spoken = stringResource(R.string.quran_juz_spoken, juz.number, juz.surahName, juz.ayah)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .pqRuledRow(first = i == 0, last = i == juzs.lastIndex)
                    .heightIn(min = 56.dp)
                    .clickable(role = Role.Button) { onOpenAyah(juz.surah, juz.ayah) }
                    .testTag(QuranListTags.juz(juz.number))
                    .clearAndSetSemantics { contentDescription = spoken }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = PqType.BodyStrong)
                    Text(text = from, style = PqType.Meta)
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = PqColors.Muted, modifier = Modifier.size(22.dp))
            }
        }
    }
}
