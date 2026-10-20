package com.pixelquest.app.quran.ui.reader

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.quran.domain.Surah
import com.pixelquest.app.quran.ui.QuranTypography

/**
 * The Bismillah above a surah, by the rule (docs/quran-data.md): above every surah but At-Tawbah,
 * which has none, and Al-Fatihah, whose first ayah it is. Shown exactly as that surah's data gives
 * it (95's and 97's carry a shadda the others don't), centred, in the reader's Arabic. It's never an
 * ayah here: it has no number.
 */
@Composable
fun BismillahHeader(bismillah: String, typography: QuranTypography, modifier: Modifier = Modifier) {
    val name = stringResource(R.string.quran_bismillah)
    Text(
        text = bismillah,
        style = typography.arabic.copy(textAlign = TextAlign.Center),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .semantics { heading(); contentDescription = name }
            .testTag(ReaderTags.BISMILLAH)
    )
}

/** How many items come before the first ayah in the reader's list: the Bismillah, where [surah] has one. */
internal fun headerCount(surah: Surah?): Int = if (surah?.bismillah != null) 1 else 0
