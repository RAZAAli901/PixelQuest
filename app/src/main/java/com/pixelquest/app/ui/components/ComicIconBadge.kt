package com.pixelquest.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.ComicShapeTokens
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.comicBorder
import com.pixelquest.app.ui.theme.comicDropShadow

/**
 * Step 23: ComicIconBadge - Pop-art wrapper applying comicBorder + comicDropShadow
 * + colored container around existing icon assets.
 */
@Composable
fun ComicIconBadge(
    @DrawableRes iconResId: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    badgeSize: Dp = 36.dp,
    iconSize: Dp = 20.dp,
    backgroundColor: Color = ComicTokens.SkyBlue,
    shape: Shape = RoundedCornerShape(ComicShapeTokens.ChipRadius),
    borderWidth: Dp = ComicShapeTokens.BorderWidthThin,
    shadowOffset: Dp = 2.dp,
    iconTint: Color? = ComicTokens.SolidBlack
) {
    Box(
        modifier = modifier.padding(end = shadowOffset, bottom = shadowOffset),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(badgeSize)
                .comicDropShadow(
                    offsetX = shadowOffset,
                    offsetY = shadowOffset,
                    color = ComicTokens.SolidBlack,
                    shape = shape
                )
                .background(backgroundColor, shape)
                .comicBorder(
                    width = borderWidth,
                    color = ComicTokens.SolidBlack,
                    shape = shape
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = iconResId),
                contentDescription = contentDescription,
                modifier = Modifier.size(iconSize),
                colorFilter = iconTint?.let { ColorFilter.tint(it) }
            )
        }
    }
}

/**
 * Step 23: Composable content overload of ComicIconBadge.
 */
@Composable
fun ComicIconBadge(
    modifier: Modifier = Modifier,
    badgeSize: Dp = 36.dp,
    backgroundColor: Color = ComicTokens.SkyBlue,
    shape: Shape = RoundedCornerShape(ComicShapeTokens.ChipRadius),
    borderWidth: Dp = ComicShapeTokens.BorderWidthThin,
    shadowOffset: Dp = 2.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.padding(end = shadowOffset, bottom = shadowOffset),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(badgeSize)
                .comicDropShadow(
                    offsetX = shadowOffset,
                    offsetY = shadowOffset,
                    color = ComicTokens.SolidBlack,
                    shape = shape
                )
                .background(backgroundColor, shape)
                .comicBorder(
                    width = borderWidth,
                    color = ComicTokens.SolidBlack,
                    shape = shape
                ),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}
