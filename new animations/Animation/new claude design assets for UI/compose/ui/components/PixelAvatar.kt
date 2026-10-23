package com.pixelquest.ui.components

import com.pixelquest.ui.theme.*

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.pixelquest.ui.theme.PqColors

/**
 * 10×10 pixel hero drawn in code (no assets). If you already ship avatar drawables,
 * replace the body with Image(painterResource(...), filterQuality = FilterQuality.None).
 */
private val AVATAR = listOf(
    "..........",
    "..hhhhhh..",
    ".hhhhhhhh.",
    ".hssssssh.",
    ".sesssses.",
    ".ssssssss.",
    "..ssmmss..",
    "..tttttt..",
    ".tttttttt.",
    ".tttttttt.",
)

@Composable
fun PixelAvatar(avatar: AvatarClass, size: Dp, modifier: Modifier = Modifier) {
    val shape = pqShape(size * 0.16f)
    Box(modifier.size(size).clip(shape).background(PqColors.AvatarBg).border(size * 0.04f, PqColors.Ink, shape)) {
        Canvas(Modifier.fillMaxSize()) {
            val cell = this.size.width * 0.08f
            val ox = this.size.width * 0.10f
            val oy = this.size.width * 0.12f
            AVATAR.forEachIndexed { r, row ->
                row.forEachIndexed { c, ch ->
                    val color = when (ch) {
                        'h' -> avatar.hair
                        't' -> avatar.shirt
                        's' -> Color(0xFFF2C29B)
                        'e' -> PqColors.Ink
                        'm' -> Color(0xFFC0675A)
                        else -> null
                    }
                    // +0.5px overlap hides hairline seams between cells
                    if (color != null) drawRect(color, Offset(ox + c * cell, oy + r * cell), Size(cell + 0.5f, cell + 0.5f))
                }
            }
        }
    }
}
