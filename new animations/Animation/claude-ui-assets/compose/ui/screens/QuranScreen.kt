package com.pixelquest.ui.screens

import com.pixelquest.ui.theme.*

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.ui.components.*
import com.pixelquest.ui.theme.PixelQuestTheme
import com.pixelquest.ui.theme.PqColors
import com.pixelquest.ui.theme.PqDimens
import com.pixelquest.ui.theme.PqType

enum class QuranTab(val label: String) { Surahs("Surahs"), Juz("Juz"), Saved("Saved") }

data class SurahUi(val number: Int, val name: String, val meaning: String, val revelation: String, val ayahs: Int, val arabic: String)

data class QuranUiState(
    val continueReading: QuranProgressUi?,
    val tab: QuranTab,
    val query: String,
    val surahs: List<SurahUi>,     // already filtered by query
    val currentSurah: Int?,        // highlights the surah being read
)

@Composable
fun QuranScreen(
    state: QuranUiState,
    onBack: () -> Unit,
    onReaderSettings: () -> Unit,
    onContinue: () -> Unit,
    onTabChange: (QuranTab) -> Unit,
    onQueryChange: (String) -> Unit,
    onSurahClick: (SurahUi) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    LazyColumn(
        Modifier.fillMaxSize().background(PqColors.Ground),
        contentPadding = PaddingValues(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 8.dp, bottom = contentPadding.calculateBottomPadding() + 40.dp),
    ) {
        item { PqTopBar("Quran", onBack = onBack) { PqIconButton(Icons.Rounded.TextFields, "Reader settings", onReaderSettings) } }
        state.continueReading?.let { c ->
            item {
                PqCard(Modifier.padding(top = 12.dp), bg = PqColors.Lilac, onClick = onContinue) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("CONTINUE READING", style = PqType.Label)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(c.surahName.uppercase(), style = PqType.Title, modifier = Modifier.weight(1f))
                            Text(c.surahArabic, style = PqType.Arabic.copy(fontSize = 28.sp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f).height(10.dp).clip(pqShape(5.dp)).background(PqColors.Paper).border(PqDimens.BorderThin, PqColors.Ink, pqShape(5.dp))) {
                                Box(Modifier.fillMaxWidth(c.progress).height(10.dp).background(PqColors.Ink))
                            }
                            Text("  Ayah ${c.ayah} / ${c.totalAyahs}", style = PqType.MetaStrong)
                        }
                    }
                }
            }
        }
        item { PqSegmented(QuranTab.entries.map { it.label }, state.tab.ordinal, { onTabChange(QuranTab.entries[it]) }, Modifier.padding(top = 12.dp)) }
        item {
            Row(
                Modifier.padding(top = 12.dp).fillMaxWidth().height(46.dp).clip(pqShape(12.dp)).background(PqColors.Paper)
                    .border(PqDimens.Border, PqColors.Ink, pqShape(12.dp)).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, null, tint = PqColors.Muted, modifier = Modifier.size(22.dp))
                Box(Modifier.weight(1f).padding(start = 8.dp)) {
                    if (state.query.isEmpty()) Text("Surah name or number", style = PqType.Body.copy(color = PqColors.Muted))
                    BasicTextField(state.query, onQueryChange, singleLine = true, textStyle = PqType.Body, cursorBrush = SolidColor(PqColors.Ink), modifier = Modifier.fillMaxWidth())
                }
            }
        }
        // One ruled card instead of 114 floating cards (114 light rows is fine in a Column)
        item {
            Column(
                Modifier.padding(top = 12.dp).fillMaxWidth().clip(pqShape(PqDimens.Radius)).background(PqColors.Paper)
                    .border(PqDimens.Border, PqColors.Ink, pqShape(PqDimens.Radius))
            ) {
                state.surahs.forEachIndexed { i, s ->
                    SurahRow(s, s.number == state.currentSurah) { onSurahClick(s) }
                    if (i != state.surahs.lastIndex) Box(Modifier.fillMaxWidth().height(2.dp).background(PqColors.Divider))
                }
            }
        }
    }
}

@Composable
private fun SurahRow(s: SurahUi, current: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.padding(4.dp).size(30.dp).rotate(45f).clip(pqShape(8.dp))
                .background(if (current) PqColors.Lilac else PqColors.Paper).border(PqDimens.BorderThin, PqColors.Ink, pqShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) { Text("${s.number}", style = PqType.Button.copy(letterSpacing = 0.sp), modifier = Modifier.rotate(-45f)) }
        Column(Modifier.weight(1f)) {
            Text(s.name, style = PqType.BodyStrong)
            Text("${s.meaning} · ${s.revelation} · ${s.ayahs}", style = PqType.Meta, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(s.arabic, style = PqType.Arabic)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun QuranPreview() = PixelQuestTheme {
    QuranScreen(
        QuranUiState(
            QuranProgressUi("Al-Baqara", "البقرة", 142, 286), QuranTab.Surahs, "",
            listOf(
                SurahUi(1, "Al-Faatiha", "The Opening", "Meccan", 7, "الفاتحة"),
                SurahUi(2, "Al-Baqara", "The Cow", "Medinan", 286, "البقرة"),
                SurahUi(3, "Aal-i-Imraan", "Family of Imraan", "Medinan", 200, "آل عمران"),
                SurahUi(4, "An-Nisaa", "The Women", "Medinan", 176, "النساء"),
            ),
            currentSurah = 2,
        ),
        {}, {}, {}, {}, {}, {},
    )
}
