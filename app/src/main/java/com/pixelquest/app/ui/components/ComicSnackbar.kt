package com.pixelquest.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.ui.theme.ComicShapeTokens
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.comicBorder
import com.pixelquest.app.ui.theme.comicDropShadow

/**
 * Step 18: ComicSnackbar implementing high-contrast comic-styled alert & toast treatment:
 * - 2.5dp solid black ink border
 * - 4dp hard flat drop shadow (SolidBlack)
 * - Comic panel pill badge with CoralRed accent for alert/missed notices
 * - Clean comic typography with Bangers alert tag
 */
@Composable
fun ComicSnackbar(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(ComicShapeTokens.RadiusDefault) // 10.dp
    val shadowOffset = ComicShapeTokens.ShadowOffsetDefault // 4.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(end = shadowOffset, bottom = shadowOffset)
            .comicDropShadow(
                offsetX = shadowOffset,
                offsetY = shadowOffset,
                color = ComicTokens.SolidBlack,
                shape = shape
            )
            .background(ComicTokens.PanelSurface, shape)
            .comicBorder(
                width = ComicShapeTokens.BorderWidthDefault,
                color = ComicTokens.SolidBlack,
                shape = shape
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Comic Action Badge Pill
            Box(
                modifier = Modifier
                    .background(ComicTokens.CoralRed, RoundedCornerShape(ComicShapeTokens.ChipRadius))
                    .comicBorder(ComicShapeTokens.BorderWidthThin, ComicTokens.SolidBlack, RoundedCornerShape(ComicShapeTokens.ChipRadius))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ALERT!",
                    style = MaterialTheme.typography.labelSmall,
                    color = ComicTokens.PanelSurface,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = ComicTokens.SolidBlack,
                modifier = Modifier.weight(1f)
            )

            if (actionLabel != null && onActionClick != null) {
                Spacer(modifier = Modifier.width(8.dp))
                ComicButton(
                    text = actionLabel,
                    onClick = onActionClick,
                    variant = ComicButtonVariant.PRIMARY,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}
