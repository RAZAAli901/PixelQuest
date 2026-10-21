package com.pixelquest.app.quran.ui

import com.pixelquest.app.ui.components.PqCard
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.ui.theme.ComicShapeTokens
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.isPixelSkin
import androidx.compose.ui.graphics.RectangleShape
import com.pixelquest.app.ui.theme.comicDropShadow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * How a row in the Quran's lists (surahs, juz, bookmarks, search results) is framed in the look:
 * its shape, its fill, its border, if it has one, and its flat shadow, if it has one.
 */
@Immutable
data class QuranFrame(val shape: Shape, val fill: Color, val border: BorderStroke?, val shadow: Dp? = null)

/**
 * The app's Simple Mode, for the Quran's screens: provided by [QuranLook] around each of them. In
 * Simple Mode, in any theme, they take the plain look the Islamic hub has there: softly rounded
 * blocks, no borders, no shadows.
 */
val LocalQuranSimpleMode = staticCompositionLocalOf { false }

/** Simple Mode's plain block: softly rounded, as the Islamic hub's cards are in Simple Mode. */
private val PLAIN = RoundedCornerShape(12.dp)

/**
 * The frame for a row: the skin's panel (docs/ui-v2.md), inked, with a flat shadow under it, rounded
 * in Comic and square in Pixel. In Simple Mode a plain rounded block.
 */
@Composable
fun quranFrame(): QuranFrame {
    if (LocalQuranSimpleMode.current) return QuranFrame(PLAIN, PqColors.Paper, null)
    return QuranFrame(
        shape = ComicShapeTokens.ShapeDefault,
        fill = PqColors.Paper,
        border = BorderStroke(PqDimens.BorderThin, PqColors.Ink),
        shadow = 2.dp
    )
}

/** The shape of a small choice or badge: square in the Pixel skin, round elsewhere and in Simple Mode. */
@Composable
fun quranChipShape(round: Shape = RoundedCornerShape(50)): Shape =
    if (!LocalQuranSimpleMode.current && isPixelSkin) RectangleShape else round

/**
 * A panel on the Quran's screens (the sources, a translation's terms, Go to): v2's card, or in
 * Simple Mode a plain rounded block.
 */
@Composable
fun QuranPanel(
    modifier: Modifier = Modifier,
    contentPadding: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    if (LocalQuranSimpleMode.current) {
        Box(modifier = modifier.background(PqColors.Paper, PLAIN).padding(contentPadding)) { content() }
    } else {
        PqCard(modifier) { Box(Modifier.padding(contentPadding)) { content() } }
    }
}

/** Whether the app is in Simple Mode, for [QuranLook]. */
@HiltViewModel
class QuranLookViewModel @Inject constructor(settings: SettingsRepository) : ViewModel() {
    val simpleMode: StateFlow<Boolean> = settings.simpleModeEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, false)
}

/** [content], one of the Quran's screens, told whether the app is in Simple Mode ([LocalQuranSimpleMode]). */
@Composable
fun QuranLook(viewModel: QuranLookViewModel = hiltViewModel(), content: @Composable () -> Unit) {
    val simple by viewModel.simpleMode.collectAsState()
    CompositionLocalProvider(LocalQuranSimpleMode provides simple, content = content)
}

/**
 * This row in [frame]: its shadow behind it, then filled and bordered in its shape. Not clipped to
 * it: a clip to Pixel's chamfered shape (a path) would test every tap against the path, and the
 * rows were never clipped.
 */
fun Modifier.framed(frame: QuranFrame): Modifier {
    val shadowed = if (frame.shadow != null) comicDropShadow(offsetX = frame.shadow, offsetY = frame.shadow, shape = frame.shape) else this
    val filled = shadowed.background(frame.fill, frame.shape)
    return if (frame.border != null) filled.border(frame.border, frame.shape) else filled
}
