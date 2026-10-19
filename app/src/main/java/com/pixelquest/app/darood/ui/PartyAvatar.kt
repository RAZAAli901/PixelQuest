package com.pixelquest.app.darood.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.ComicShapeTokens
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.ThemeMode
import com.pixelquest.app.ui.theme.comicBorder

/**
 * A party's picture, as a group chat has one: the first letters of its name in a circle. It's
 * decorative (the name is always beside it), so TalkBack skips it.
 */
@Composable
fun PartyAvatar(name: String, modifier: Modifier = Modifier, size: Dp = 36.dp) {
    val colors = PixelTheme.colors
    val ink = if (PixelTheme.mode == ThemeMode.Comic) {
        Modifier.comicBorder(width = ComicShapeTokens.BorderWidthThin, color = ComicTokens.SolidBlack, shape = CircleShape)
    } else {
        Modifier
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .then(ink)
            .background(colors.secondary, CircleShape)
            .testTag(DaroodTestTags.PARTY_AVATAR)
            .clearAndSetSemantics { }
    ) {
        Text(text = initialsOf(name), style = MaterialTheme.typography.labelLarge, color = colors.onSecondary)
    }
}

/**
 * Up to two letters for [name]: the first of each of its first two words ("Friday circle" is FC),
 * or one for a single word. Whole characters, so an emoji isn't cut in half; "?" for no name yet.
 */
fun initialsOf(name: String): String {
    val words = name.trim().split(Regex("""\s+""")).filter { it.isNotEmpty() }
    if (words.isEmpty()) return "?"
    return words.take(2).joinToString("") { it.substring(0, it.offsetByCodePoints(0, 1)) }.uppercase()
}
