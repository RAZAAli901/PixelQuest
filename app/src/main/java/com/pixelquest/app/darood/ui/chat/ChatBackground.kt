package com.pixelquest.app.darood.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.ThemeMode

/** The faint pattern behind a party's chat, one per look. */
enum class ChatPattern {
    /** Little square "pixels", staggered row by row: Pixel's, and Light's in its own colours. */
    PIXEL_GRID,

    /** Comic's printed halftone dots. */
    HALFTONE
}

fun chatPatternFor(mode: ThemeMode): ChatPattern = if (mode == ThemeMode.Comic) ChatPattern.HALFTONE else ChatPattern.PIXEL_GRID

/** How strongly the pattern shows: enough to tell the chat's ground from a plain page, never to read over. */
internal const val CHAT_PATTERN_ALPHA = 0.06f

/**
 * The backdrop of a party's chat: the screen's own background with a faint pattern, as chat apps
 * have, so the bubbles stand off it. Drawn, not an image, so it fits any size and costs no memory.
 * Simple Mode keeps it plain.
 */
@Composable
fun ChatBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val colors = PixelTheme.colors
    val mode = PixelTheme.mode
    val pattern = chatPatternFor(mode)
    val patterned = chatLook().pattern
    val ink = (if (mode == ThemeMode.Comic) ComicTokens.SolidBlack else colors.onBackground).copy(alpha = CHAT_PATTERN_ALPHA)
    Box(
        modifier = modifier
            .background(colors.background)
            .then(if (patterned) Modifier.drawBehind { drawChatPattern(pattern, ink) } else Modifier),
        content = content
    )
}

private fun DrawScope.drawChatPattern(pattern: ChatPattern, ink: Color) {
    when (pattern) {
        ChatPattern.PIXEL_GRID -> {
            val step = 24.dp.toPx()
            val pixel = Size(2.dp.toPx(), 2.dp.toPx())
            var row = 0
            var y = step / 2
            while (y < size.height) {
                var x = if (row % 2 == 0) step / 2 else step
                while (x < size.width) {
                    drawRect(ink, topLeft = Offset(x, y), size = pixel)
                    x += step
                }
                y += step
                row++
            }
        }
        ChatPattern.HALFTONE -> {
            val step = 14.dp.toPx()
            val radius = 1.5.dp.toPx()
            var row = 0
            var y = step / 2
            while (y < size.height) {
                var x = if (row % 2 == 0) step / 2 else step
                while (x < size.width) {
                    drawCircle(ink, radius = radius, center = Offset(x, y))
                    x += step
                }
                y += step
                row++
            }
        }
    }
}
