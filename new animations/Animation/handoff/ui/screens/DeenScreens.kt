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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.ui.components.*
import com.pixelquest.ui.theme.PixelQuestTheme
import com.pixelquest.ui.theme.PqColors
import com.pixelquest.ui.theme.PqDimens
import com.pixelquest.ui.theme.PqType

// =====================  DEEN TAB (replaces "Islamic")  =====================

data class QuranProgressUi(val surahName: String, val surahArabic: String, val ayah: Int, val totalAyahs: Int) {
    val progress get() = ayah / totalAyahs.toFloat()
}

data class DeenUiState(
    val dateLabel: String,                // "Tue, 20 Oct"
    val nextPrayer: NextPrayerUi?,
    val prayers: List<PrayerUi>,          // always 5, in order
    val allFiveStreakDays: Int,
    val quran: QuranProgressUi?,
)

@Composable
fun DeenScreen(
    state: DeenUiState,
    onOpenTracker: () -> Unit,
    onOpenQuran: () -> Unit,
    onOpenDarood: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    LazyColumn(
        Modifier.fillMaxSize().background(PqColors.Ground),
        contentPadding = PaddingValues(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 8.dp, bottom = contentPadding.calculateBottomPadding() + 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { PqTopBar("Deen") { Text(state.dateLabel, style = PqType.MetaStrong.copy(color = PqColors.Muted, fontSize = 13.bsp)) } }
        item {
            PqCard(bg = PqColors.Sky, shadow = PqDimens.ShadowLg, radius = 18.dp) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("NEXT PRAYER", style = PqType.Label, modifier = Modifier.weight(1f))
                        Icon(Icons.Rounded.LocalFireDepartment, null, tint = PqColors.Flame, modifier = Modifier.size(16.dp))
                        Text("${state.allFiveStreakDays} days all five", style = PqType.MetaStrong)
                    }
                    val p = state.nextPrayer
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text((p?.name ?: "All prayed").uppercase(), style = PqType.display(56, 52), modifier = Modifier.weight(1f))
                        if (p != null) Column(horizontalAlignment = Alignment.End) {
                            Text(p.time, style = PqType.BodyStrong.copy(fontSize = 18.bsp))
                            Text(p.countdown, style = PqType.MetaStrong.copy(fontSize = 13.bsp))
                        }
                    }
                    PrayerPips(state.prayers)
                    PqButton("Open Namaz tracker", onOpenTracker, Modifier.fillMaxWidth(), style = PqButtonStyle.Secondary, trailingIcon = Icons.Rounded.ArrowForward, height = 44.dp)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DeenTile(Icons.Rounded.MenuBook, "Quran", state.quran?.let { "Continue ${it.surahName} · ayah ${it.ayah}" } ?: "Read in Arabic and English",
                    PqColors.Lilac, state.quran?.progress, onOpenQuran, Modifier.weight(1f))
                DeenTile(Icons.Rounded.Groups, "Darood Circle", "Count Darood together with friends", PqColors.Amber, null, onOpenDarood, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PrayerPips(prayers: List<PrayerUi>) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        prayers.forEach { p ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val box = Modifier.fillMaxWidth().height(36.dp)
                if (p.status == PrayerStatus.Prayed) {
                    Box(box.clip(pqShape(9.dp)).background(PqColors.Paper).border(PqDimens.BorderThin, PqColors.Ink, pqShape(9.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Check, null, tint = PqColors.Ink, modifier = Modifier.size(20.dp))
                    }
                } else Box(box.dashedBorder())
                Text(p.name, style = PqType.NavLabel)
            }
        }
    }
}

@Composable
private fun DeenTile(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String, bg: Color, progress: Float?, onClick: () -> Unit, modifier: Modifier) {
    PqCard(modifier.heightIn(min = 150.dp), bg = bg, onClick = onClick) {
        Column(Modifier.fillMaxWidth().heightIn(min = 150.dp).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = PqColors.Ink, modifier = Modifier.size(30.dp))
            Text(title, style = PqType.BodyStrong.copy(fontSize = 16.bsp))
            Text(body, style = PqType.Meta.copy(color = PqColors.Ink))
            if (progress != null) {
                Spacer(Modifier.weight(1f))
                Box(Modifier.fillMaxWidth().height(10.dp).clip(pqShape(5.dp)).background(PqColors.Paper).border(PqDimens.BorderThin, PqColors.Ink, pqShape(5.dp))) {
                    Box(Modifier.fillMaxWidth(progress).height(10.dp).background(PqColors.Ink))
                }
            }
        }
    }
}

// =====================  NAMAZ TRACKER  =====================

data class WeekDayUi(val letter: String, val date: Int, val status: DayStatus, val isToday: Boolean)

data class NamazUiState(
    val week: List<WeekDayUi>,       // last 7 days, today last
    val prayers: List<PrayerUi>,
    val xpToday: Int,
) { val prayedCount get() = prayers.count { it.status == PrayerStatus.Prayed } }

@Composable
fun NamazScreen(
    state: NamazUiState,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onDaySelect: (WeekDayUi) -> Unit,
    onTogglePrayed: (PrayerUi) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    LazyColumn(
        Modifier.fillMaxSize().background(PqColors.Ground),
        contentPadding = PaddingValues(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 8.dp, bottom = contentPadding.calculateBottomPadding() + 40.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { PqTopBar("Namaz", onBack = onBack) { PqIconButton(Icons.Rounded.Tune, "Prayer settings", onSettings) } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                state.week.forEach { d ->
                    val bg = if (d.isToday) PqColors.Ink else PqColors.Paper
                    val fg = if (d.isToday) PqColors.Paper else PqColors.Ink
                    Column(
                        Modifier.weight(1f).height(62.dp).clip(pqShape(12.dp)).background(bg)
                            .border(PqDimens.BorderThin, PqColors.Ink, pqShape(12.dp)).clickable { onDaySelect(d) },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
                    ) {
                        Text(d.letter, style = PqType.NavLabel.copy(color = fg))
                        Text("${d.date}", style = PqType.BodyStrong.copy(fontSize = 16.bsp, color = fg))
                        val dot = if (d.isToday) PqColors.Gold else if (d.status == DayStatus.None) Color.Transparent else d.status.color
                        Box(Modifier.size(8.dp).clip(PqCircle).background(dot))
                    }
                }
            }
        }
        item {
            PqCard(radius = PqDimens.Radius) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row { Text("${state.prayedCount} / 5 prayed today", style = PqType.BodyStrong.copy(fontSize = 16.bsp), modifier = Modifier.weight(1f)); Text("+${state.xpToday} XP", style = PqType.MetaStrong.copy(color = PqColors.RedText)) }
                    PqSegmentBar(5, state.prayedCount, fill = PqColors.Progress, height = 10.dp)
                }
            }
        }
        items(state.prayers, key = { it.name }) { p -> PrayerRow(p) { onTogglePrayed(p) } }
    }
}

@Composable
private fun PrayerRow(p: PrayerUi, onToggle: () -> Unit) {
    val next = p.status == PrayerStatus.Next
    val m = if (next) Modifier.comic(radius = PqDimens.Radius, shadowColor = PqColors.Red, tappable = true)
    else Modifier.clip(pqShape(PqDimens.Radius)).background(PqColors.Paper).border(PqDimens.BorderThin, PqColors.Ink, pqShape(PqDimens.Radius))
    Row(m.fillMaxWidth().padding(horizontal = 12.dp, vertical = if (next) 10.dp else 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(40.dp).clip(PqCircle).background(prayerColor(p.name)).border(PqDimens.BorderThin, PqColors.Ink, PqCircle), contentAlignment = Alignment.Center) {
            Icon(prayerIcon(p.name), null, tint = PqColors.Ink, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(p.name, style = PqType.BodyStrong)
            Text(
                if (next && p.countdown != null) "${p.time} · ${p.countdown}" else p.time,
                style = if (next) PqType.MetaStrong.copy(color = PqColors.RedText) else PqType.Meta,
            )
        }
        when (p.status) {
            PrayerStatus.Prayed -> Row(
                Modifier.height(30.dp).clip(PqCircle).background(PqColors.Ink).clickable(onClick = onToggle).padding(start = 6.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) { Icon(Icons.Rounded.Check, null, tint = PqColors.Paper, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(3.dp)); Text("Prayed", style = PqType.Button.copy(fontSize = 12.lsp, color = PqColors.Paper, letterSpacing = 0.sp)) }
            PrayerStatus.Missed -> PqButton("Make up", onToggle, style = PqButtonStyle.Secondary, height = 34.dp)
            else -> PqButton("Mark", onToggle, style = PqButtonStyle.Secondary, height = 34.dp)
        }
    }
}

// ---------- Previews ----------

private val samplePrayers = listOf(
    PrayerUi("Fajr", "4:48 AM", PrayerStatus.Prayed),
    PrayerUi("Dhuhr", "11:48 AM", PrayerStatus.Prayed),
    PrayerUi("Asr", "3:47 PM", PrayerStatus.Prayed),
    PrayerUi("Maghrib", "5:25 PM", PrayerStatus.Prayed),
    PrayerUi("Isha", "6:46 PM", PrayerStatus.Next, "in 36 min"),
)

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun DeenPreview() = PixelQuestTheme {
    DeenScreen(DeenUiState("Tue, 20 Oct", NextPrayerUi("Isha", "6:46 PM", "in 36 min"), samplePrayers, 2, QuranProgressUi("Al-Baqara", "البقرة", 142, 286)), {}, {}, {})
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun NamazPreview() = PixelQuestTheme {
    val days = listOf("W" to 14, "T" to 15, "F" to 16, "S" to 17, "S" to 18, "M" to 19, "T" to 20)
    val st = listOf(DayStatus.Partial, DayStatus.Perfect, DayStatus.Missed, DayStatus.Partial, DayStatus.Perfect, DayStatus.Perfect, DayStatus.None)
    NamazScreen(NamazUiState(days.mapIndexed { i, (l, d) -> WeekDayUi(l, d, st[i], i == 6) }, samplePrayers, 48), {}, {}, {}, {})
}
