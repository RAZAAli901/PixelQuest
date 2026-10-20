package com.pixelquest.app.quran.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CutCornerShape
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
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.theme.ComicShapeTokens
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.ThemeMode
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
 * The look's frame for a row: in Pixel, chamfered corners and the pixel border, as Pixel's panels
 * have; in Comic, an inked outline and a flat black shadow down and right, as Comic's panels have;
 * in Light, rounded with a thin border, as Light's panels have. In Simple Mode, in any theme, a
 * plain rounded block.
 */
@Composable
fun quranFrame(): QuranFrame {
    val colors = PixelTheme.colors
    if (LocalQuranSimpleMode.current) return QuranFrame(PLAIN, colors.surface, null)
    return when (PixelTheme.mode) {
        ThemeMode.Pixel, ThemeMode.System -> QuranFrame(CutCornerShape(6.dp), colors.surface, BorderStroke(2.dp, colors.pixelBorder))
        ThemeMode.Comic -> QuranFrame(
            shape = ComicShapeTokens.ShapeDefault,
            fill = colors.surface,
            border = BorderStroke(ComicShapeTokens.BorderWidthThin, ComicTokens.SolidBlack),
            shadow = ComicShapeTokens.ShadowOffsetSmall
        )
        else -> QuranFrame(RoundedCornerShape(10.dp), colors.surface, BorderStroke(1.dp, colors.pixelBorder))
    }
}

/** The shape of a small choice or badge: chamfered in Pixel, round elsewhere and in Simple Mode. */
@Composable
fun quranChipShape(chamfer: Dp = 4.dp, round: Shape = RoundedCornerShape(50)): Shape =
    if (!LocalQuranSimpleMode.current && (PixelTheme.mode == ThemeMode.Pixel || PixelTheme.mode == ThemeMode.System)) CutCornerShape(chamfer) else round

/**
 * A panel on the Quran's screens (the Continue reading card, the sources, a translation's terms):
 * the look's own (PixelCard), or in Simple Mode a plain rounded block.
 */
@Composable
fun QuranPanel(
    modifier: Modifier = Modifier,
    variant: PixelPanelVariant = PixelPanelVariant.BORDER,
    contentPadding: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    if (LocalQuranSimpleMode.current) {
        Box(modifier = modifier.background(PixelTheme.colors.surface, PLAIN).padding(contentPadding)) { content() }
    } else {
        PixelCard(modifier = modifier, variant = variant, contentPadding = contentPadding, content = content)
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
