package com.pixelquest.app.quran.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.quran.domain.Surah
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelButtonVariant

/**
 * At the end of a surah: the way to the one before it and the one after, each by name. Al-Fatihah has
 * no previous surah and An-Nas no next, so there's just the one button there.
 */
@Composable
fun SurahNeighbours(previous: Surah?, next: Surah?, onOpenSurah: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp)
    ) {
        if (previous != null) {
            PixelButton(
                text = stringResource(R.string.quran_previous_surah, previous.englishName),
                onClick = { onOpenSurah(previous.number) },
                variant = PixelButtonVariant.BLUE,
                modifier = Modifier.weight(1f).testTag(ReaderTags.PREVIOUS)
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
        if (next != null) {
            PixelButton(
                text = stringResource(R.string.quran_next_surah, next.englishName),
                onClick = { onOpenSurah(next.number) },
                modifier = Modifier.weight(1f).testTag(ReaderTags.NEXT)
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
