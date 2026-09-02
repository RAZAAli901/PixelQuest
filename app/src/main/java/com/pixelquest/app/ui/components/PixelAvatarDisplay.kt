package com.pixelquest.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixelquest.app.domain.AvatarCatalog

@Composable
fun PixelAvatarDisplay(
    avatarId: String,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
) {
    val avatar = AvatarCatalog.getAvatarById(avatarId)
    Image(
        painter = painterResource(id = avatar.drawableRes),
        contentDescription = avatar.name,
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size)
    )
}
