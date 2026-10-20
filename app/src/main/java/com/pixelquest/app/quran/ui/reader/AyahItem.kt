package com.pixelquest.app.quran.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pixelquest.app.quran.domain.Ayah
import com.pixelquest.app.quran.ui.QuranTypography
import com.pixelquest.app.ui.theme.PixelTheme

/**
 * One ayah: its Arabic, ending in its number inside the end-of-ayah sign, then its translation below.
 * [mode] shows the Arabic, the translation, or both; with the Arabic hidden, the number goes before
 * the translation instead.
 *
 * The ayah's own text is shown exactly as its source gives it: the sign is drawn after it, never
 * part of it (copying an ayah copies its text alone).
 */
@Composable
fun AyahItem(
    ayah: Ayah,
    mode: DisplayMode,
    typography: QuranTypography,
    modifier: Modifier = Modifier
) {
    val colors = PixelTheme.colors
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth().testTag(ReaderTags.ayah(ayah.number))
    ) {
        if (mode != DisplayMode.TRANSLATION) {
            Text(
                text = AyahMarker.withMarker(ayah.arabic, ayah.number),
                style = typography.arabic,
                modifier = Modifier.fillMaxWidth().testTag(ReaderTags.arabic(ayah.number))
            )
        }
        if (mode != DisplayMode.ARABIC) {
            if (mode == DisplayMode.TRANSLATION) {
                Text(
                    text = ayah.number.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.primaryText
                )
            }
            Text(
                text = ayah.translation,
                style = typography.translation,
                modifier = Modifier.fillMaxWidth().testTag(ReaderTags.translation(ayah.number))
            )
        }
    }
}

/** The end-of-ayah sign with an ayah's number, as Amiri Quran draws it. */
object AyahMarker {
    /** ARABIC END OF AYAH: followed by digits, Amiri Quran draws them inside its ornament. */
    private const val END_OF_AYAH = '۝'

    /** A space that never breaks: the sign stays on the same line as the ayah's last word. */
    private const val KEEP_TOGETHER = ' '

    /** [number] in Arabic-Indic digits (٠ to ٩), as the sign holds them. */
    fun arabicDigits(number: Int): String = number.toString().map { '٠' + (it - '0') }.joinToString("")

    /** The sign for [number]: the end-of-ayah mark, then the number's digits. */
    fun of(number: Int): String = END_OF_AYAH + arabicDigits(number)

    /** [arabic], exactly as given, then the sign for [number] after it. */
    fun withMarker(arabic: String, number: Int): String = arabic + KEEP_TOGETHER + of(number)
}
