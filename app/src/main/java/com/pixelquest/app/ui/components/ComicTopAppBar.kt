package com.pixelquest.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.ui.theme.ComicShapeTokens
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.ComicTypography
import com.pixelquest.app.ui.theme.comicBorder
import com.pixelquest.app.ui.theme.comicDropShadow

/**
 * Step 3 (Day 23): Comic-styled Top App Bar / Header.
 *
 * Implements comic-book title header treatment:
 * - Solid 2.5dp black ink border with 4dp flat black drop shadow.
 * - Bangers font title text with high-contrast ink styling and uppercase tracking.
 * - Tactile comic pop-art back navigation button and action slots.
 * - Newsprint/white surface cell styling matching Nitnode reference.
 */
@Composable
fun ComicTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    containerColor: Color = ComicTokens.PanelSurface
) {
    ComicPanel(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        variant = ComicPanelVariant.SURFACE,
        borderWidth = ComicShapeTokens.BorderWidthDefault,
        shadowOffset = ComicShapeTokens.ShadowOffsetDefault,
        cornerRadius = ComicShapeTokens.RadiusLarge,
        contentPadding = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (navigationIcon != null) {
                navigationIcon()
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = title.uppercase(),
                style = ComicTypography.titleLarge,
                color = ComicTokens.SolidBlack,
                letterSpacing = 0.8.sp,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )

            if (actions != null) {
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions
                )
            }
        }
    }
}

/**
 * Reusable comic-styled navigation icon button (e.g. Back button).
 */
@Composable
fun ComicNavigationButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {
        Text("◀", style = ComicTypography.titleMedium, color = ComicTokens.SolidBlack)
    }
) {
    val shape = RoundedCornerShape(ComicShapeTokens.ChipRadius)
    Box(
        modifier = modifier
            .size(40.dp)
            .comicDropShadow(
                offsetX = ComicShapeTokens.ShadowOffsetSmall,
                offsetY = ComicShapeTokens.ShadowOffsetSmall,
                shape = shape
            )
            .comicBorder(
                width = ComicShapeTokens.BorderWidthThin,
                color = ComicTokens.SolidBlack,
                shape = shape
            )
            .background(ComicTokens.SkyBlue, shape)
            .clickable(
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Preview(name = "Comic Top App Bar", showBackground = true)
@Composable
private fun ComicTopAppBarPreview() {
    ComicTopAppBar(
        title = "LEADERBOARD",
        navigationIcon = { ComicNavigationButton(onClick = {}) },
        actions = {
            Text("🔄", fontSize = 18.sp, modifier = Modifier.padding(horizontal = 4.dp))
        }
    )
}
