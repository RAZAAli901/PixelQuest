package com.pixelquest.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    textColor: Color = Color.Unspecified
) {
    val bgRes = when (variant) {
        PixelButtonVariant.YELLOW -> R.drawable.pixel_button_yellow
        PixelButtonVariant.BLUE -> R.drawable.pixel_button_blue
    }

    Box(
        modifier = modifier.clickable { onClick() },
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
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}
