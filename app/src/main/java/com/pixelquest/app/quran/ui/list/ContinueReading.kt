package com.pixelquest.app.quran.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.quran.ui.QuranTypography
import com.pixelquest.app.ui.components.PqCard
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import com.pixelquest.app.ui.theme.pqShape

/**
 * Where the player stopped reading (UI v2, mock 2F): the surah, in English and Arabic, and how far
 * through it they are, as a bar and "Ayah 255 / 286". A tap opens the reader there. TalkBack hears
 * it as "Continue reading: 2. Al-Baqara, ayah 255".
 */
@Composable
fun ContinueReadingCard(place: ContinueReading, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val title = stringResource(R.string.quran_continue_title)
    val where = stringResource(R.string.quran_continue_place, place.surah.number, place.surah.englishName, place.ayah)
    val progress = if (place.surah.ayahCount == 0) 0f else place.ayah / place.surah.ayahCount.toFloat()
    PqCard(
        modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = "$title: $where" }
            .testTag(CONTINUE_READING),
        bg = PqColors.Lilac,
        onClick = onOpen,
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title.uppercase(), style = PqType.Label)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(place.surah.englishName.uppercase(), style = PqType.Title, modifier = Modifier.weight(1f), maxLines = 1)
                Text(place.surah.arabicName, style = QuranTypography.of(arabicSize = 28f, color = PqColors.Ink).arabic, maxLines = 1)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(10.dp).clip(pqShape(5.dp)).background(PqColors.Paper).border(PqDimens.BorderThin, PqColors.Ink, pqShape(5.dp))) {
                    Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(10.dp).background(PqColors.Ink))
                }
                Text(
                    stringResource(R.string.quran_continue_progress, place.ayah, place.surah.ayahCount),
                    style = PqType.MetaStrong, modifier = Modifier.padding(start = 10.dp)
                )
            }
        }
    }
}

/** The Continue reading card's test tag. */
const val CONTINUE_READING = "quran_continue_reading"
