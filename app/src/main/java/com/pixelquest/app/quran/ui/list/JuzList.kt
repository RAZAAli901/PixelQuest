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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.quran.ui.framed
import com.pixelquest.app.quran.ui.quranFrame
import com.pixelquest.app.quran.domain.Juz
import com.pixelquest.app.ui.theme.ComicShapeTokens
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.ThemeMode
import com.pixelquest.app.ui.theme.inkOnPanel

/** The Quran screen's two lists. */
enum class QuranTab { SURAHS, JUZ }

/**
 * Surahs or Juz: two tabs, the chosen one filled. Shaped by the look: chamfered in Pixel, inked in
 * Comic, rounded elsewhere. TalkBack hears them as tabs, the chosen one selected.
 */
@Composable
fun QuranTabs(selected: QuranTab, onSelect: (QuranTab) -> Unit, modifier: Modifier = Modifier) {
    val colors = PixelTheme.colors
    val shape: Shape = when (PixelTheme.mode) {
        ThemeMode.Pixel, ThemeMode.System -> CutCornerShape(4.dp)
        else -> RoundedCornerShape(50)
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth().selectableGroup()
    ) {
        QuranTab.entries.forEach { tab ->
            val chosen = tab == selected
            val ink = if (PixelTheme.mode == ThemeMode.Comic) {
                Modifier.border(ComicShapeTokens.BorderWidthThin, ComicTokens.SolidBlack, shape)
            } else {
                Modifier.border(1.dp, colors.primary.copy(alpha = if (chosen) 1f else 0.4f), shape)
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = com.pixelquest.app.quran.ui.reader.MIN_TARGET)
                    .background(if (chosen) colors.primary.copy(alpha = 0.18f) else colors.surface, shape)
                    .then(ink)
                    .selectable(selected = chosen, role = Role.Tab, onClick = { onSelect(tab) })
                    .testTag(QuranListTags.tab(tab))
            ) {
                Text(
                    text = stringResource(if (tab == QuranTab.SURAHS) R.string.quran_tab_surahs else R.string.quran_tab_juz),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (chosen) FontWeight.Bold else FontWeight.Normal,
                    color = inkOnPanel(colors.primaryText)
                )
            }
        }
    }
}

/**
 * The 30 juz, a row each: its number, and the surah and ayah it starts at. A tap opens the reader
 * there. One button to TalkBack: "Juz 2, from Al-Baqara, ayah 142".
 */
@Composable
fun JuzList(juzs: List<Juz>, onOpenAyah: (surah: Int, ayah: Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = PixelTheme.colors
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxSize().testTag(QuranListTags.JUZ_LIST)
    ) {
        items(juzs, key = { it.number }) { juz ->
            val title = stringResource(R.string.quran_juz_title, juz.number)
            val from = stringResource(R.string.quran_juz_from, juz.surahName, juz.surah, juz.ayah)
            val spoken = stringResource(R.string.quran_juz_spoken, juz.number, juz.surahName, juz.ayah)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .framed(quranFrame())
                    .clickable(role = Role.Button) { onOpenAyah(juz.surah, juz.ayah) }
                    .testTag(QuranListTags.juz(juz.number))
                    .clearAndSetSemantics { contentDescription = spoken }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = title, style = MaterialTheme.typography.titleSmall, color = inkOnPanel(colors.primaryText))
                    Text(text = from, style = MaterialTheme.typography.bodySmall, color = colors.onSurface)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "›", style = MaterialTheme.typography.titleLarge, color = colors.onSurfaceVariant)
            }
        }
    }
}
