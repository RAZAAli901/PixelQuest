package com.pixelquest.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R

enum class PixelButtonVariant {
    YELLOW,
    BLUE
}

@Composable
fun PixelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: PixelButtonVariant = PixelButtonVariant.YELLOW,
    textColor: Color = Color.Unspecified,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val bgRes = when (variant) {
        PixelButtonVariant.YELLOW -> if (isPressed) R.drawable.pixel_button_yellow_pressed else R.drawable.pixel_button_yellow
        PixelButtonVariant.BLUE -> if (isPressed) R.drawable.pixel_button_blue_pressed else R.drawable.pixel_button_blue
    }

    val contentOffsetY = if (isPressed) 2.dp else 0.dp

    Box(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = bgRes),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.matchParentSize()
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (textColor == Color.Unspecified) {
                if (variant == PixelButtonVariant.YELLOW) Color.Black else Color.White
            } else textColor,
            modifier = Modifier
                .offset(y = contentOffsetY)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}
