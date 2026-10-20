package com.pixelquest.app.quran.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.quran.domain.Ayah
import com.pixelquest.app.quran.ui.QuranTypography
import com.pixelquest.app.ui.theme.PixelTheme

/**
 * One ayah: its Arabic, ending in its number inside the end-of-ayah sign, then its translation below.
 * [mode] shows the Arabic, the translation, or both; with the Arabic hidden, the number goes before
 * the translation instead.
 *
 * The Arabic is always laid out right to left, from the right edge, its sign at its end on the left;
 * the translation always left to right. Whatever the phone's own language direction, each keeps its
 * own ([RightToLeft], [LeftToRight]).
 *
 * The ayah's own text is shown exactly as its source gives it: the sign is drawn after it, never
 * part of it (copying an ayah copies its text alone).
 */
@Composable
fun AyahItem(
    ayah: Ayah,
    mode: DisplayMode,
    typography: QuranTypography,
    modifier: Modifier = Modifier,
    bookmarked: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val colors = PixelTheme.colors
    val tap = if (onClick != null) {
        Modifier.clickable(role = Role.Button, onClickLabel = stringResource(R.string.quran_ayah_open_actions), onClick = onClick)
    } else {
        Modifier
    }
    val marked = stringResource(R.string.quran_bookmarked)
    Box(modifier = modifier.fillMaxWidth()) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(tap)
            .semantics { if (bookmarked) stateDescription = marked }
            .testTag(ReaderTags.ayah(ayah.number))
    ) {
        if (mode != DisplayMode.TRANSLATION) {
            RightToLeft {
                Text(
                    text = AyahMarker.withMarker(ayah.arabic, ayah.number),
                    style = typography.arabic,
                    modifier = Modifier.fillMaxWidth().testTag(ReaderTags.arabic(ayah.number))
                )
            }
        }
        if (mode != DisplayMode.ARABIC) {
            LeftToRight {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
    }
    if (bookmarked) BookmarkRibbon(modifier = Modifier.align(Alignment.TopStart))
    }
}

/**
 * A small ribbon at a bookmarked ayah's top-left corner, in the look's accent: a strip with a notch
 * cut in its foot, as a ribbon marker has. TalkBack hears "Bookmarked" from the ayah instead.
 */
@Composable
private fun BookmarkRibbon(modifier: Modifier = Modifier) {
    val ribbon = GenericShape { size, _ ->
        moveTo(0f, 0f)
        lineTo(size.width, 0f)
        lineTo(size.width, size.height)
        lineTo(size.width / 2, size.height * 0.72f)
        lineTo(0f, size.height)
        close()
    }
    Box(
        modifier = modifier
            .offset(x = (-12).dp, y = (-8).dp)
            .size(width = 10.dp, height = 18.dp)
            .background(PixelTheme.colors.secondary, ribbon)
            .testTag(ReaderTags.RIBBON)
            .clearAndSetSemantics {}
    )
}

/** [content] laid out right to left, as Arabic is, whatever the phone's direction. */
@Composable
fun RightToLeft(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, content = content)
}

/** [content] laid out left to right, as the translation is, even on a phone set to a right-to-left language. */
@Composable
fun LeftToRight(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr, content = content)
}

/** The end-of-ayah sign with an ayah's number, as Amiri Quran draws it. */
object AyahMarker {
    /** ARABIC END OF AYAH: followed by digits, Amiri Quran draws them inside its ornament. */
    private const val END_OF_AYAH = '\u06DD'

    /** A space that never breaks: the sign stays on the same line as the ayah's last word. */
    private const val KEEP_TOGETHER = '\u00A0'

    /** [number] in Arabic-Indic digits (٠ to ٩), as the sign holds them. */
    fun arabicDigits(number: Int): String = number.toString().map { '\u0660' + (it - '0') }.joinToString("")

    /** The sign for [number]: the end-of-ayah mark, then the number's digits. */
    fun of(number: Int): String = END_OF_AYAH + arabicDigits(number)

    /** [arabic], exactly as given, then the sign for [number] after it. */
    fun withMarker(arabic: String, number: Int): String = arabic + KEEP_TOGETHER + of(number)
}
