package com.pixelquest.app.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.PqColors

/**
 * Visual variant options for ComicButton.
 */
enum class ComicButtonVariant {
    PRIMARY,        // the primary red
    BURNT_ORANGE,   // amber
    SKY_BLUE,       // sky
    LAVENDER,       // lilac
    SURFACE         // paper
}

/** The fill a [ComicButton] of this [variant] paints, in UI v2's colours. */
internal fun comicButtonFill(variant: ComicButtonVariant): Color = when (variant) {
    ComicButtonVariant.PRIMARY -> PqColors.Red
    ComicButtonVariant.BURNT_ORANGE -> PqColors.Amber
    ComicButtonVariant.SKY_BLUE -> PqColors.Sky
    ComicButtonVariant.LAVENDER -> PqColors.Lilac
    ComicButtonVariant.SURFACE -> PqColors.Paper
}

/**
 * A comic button, as the screens from before UI v2 ask for one: now the skin's own, in [variant]'s
 * colour. The text is as given.
 */
@Composable
fun ComicButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ComicButtonVariant = ComicButtonVariant.PRIMARY,
    textColor: Color = Color.Unspecified,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
) {
    LegacyButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        fill = comicButtonFill(variant),
        textColor = if (textColor != Color.Unspecified) textColor else if (variant == ComicButtonVariant.PRIMARY) PqColors.OnRed else PqColors.Ink,
        enabled = enabled
    )
}
