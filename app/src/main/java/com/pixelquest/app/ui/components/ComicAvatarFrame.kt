package com.pixelquest.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.domain.AvatarTier
import com.pixelquest.app.domain.AvatarTierCalculator
import com.pixelquest.app.ui.theme.BangersFontFamily
import com.pixelquest.app.ui.theme.ComicShapeTokens
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.comicBorder
import com.pixelquest.app.ui.theme.comicDropShadow

/**
 * Step 16: ComicAvatarFrame implementing comic pop-art frames and tier badges.
 * Reinterprets Day 7's bronze/silver/gold tier system in comic style:
 * - Bronze: Burnt Orange comic ribbon (#F0A868)
 * - Silver: Sky Blue comic ribbon (#8ECAE6)
 * - Gold: Gold Accent comic ribbon (#FFB703)
 * With 2.5dp black ink border, 4dp flat drop shadow, and Bangers typography.
 */
@Composable
fun ComicAvatarFrame(
    avatarId: String,
    level: Int,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    isSimpleMode: Boolean = false
) {
    val tier = AvatarTierCalculator.calculateTier(level)

    val tierRibbonColor = when (tier) {
        AvatarTier.BRONZE -> ComicTokens.BurntOrange
        AvatarTier.SILVER -> ComicTokens.SkyBlue
        AvatarTier.GOLD -> ComicTokens.GoldAccent
    }

    val tierLabel = when (tier) {
        AvatarTier.BRONZE -> "BRONZE"
        AvatarTier.SILVER -> "SILVER"
        AvatarTier.GOLD -> "GOLD"
    }

    val frameShape = RoundedCornerShape(ComicShapeTokens.RadiusLarge) // 12.dp
    val shadowOffset = ComicShapeTokens.ShadowOffsetDefault // 4.dp

    Box(
        modifier = modifier.padding(bottom = if (!isSimpleMode) 14.dp else shadowOffset, end = shadowOffset),
        contentAlignment = Alignment.Center
    ) {
        // Main Avatar Comic Panel Frame
        Box(
            modifier = Modifier
                .comicDropShadow(
                    offsetX = shadowOffset,
                    offsetY = shadowOffset,
                    color = ComicTokens.SolidBlack,
                    shape = frameShape
                )
                .background(ComicTokens.PanelSurface, frameShape)
                .comicBorder(
                    width = ComicShapeTokens.BorderWidthDefault,
                    color = ComicTokens.SolidBlack,
                    shape = frameShape
                )
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            PixelAvatarDisplay(
                avatarId = avatarId,
                size = size
            )
        }

        // Comic Pop-Art Tier Ribbon Badge (pinned at bottom-center)
        if (!isSimpleMode) {
            val badgeShape = RoundedCornerShape(ComicShapeTokens.RadiusSmall)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 10.dp)
                    .comicDropShadow(
                        offsetX = 2.dp,
                        offsetY = 2.dp,
                        color = ComicTokens.SolidBlack,
                        shape = badgeShape
                    )
                    .background(tierRibbonColor, badgeShape)
                    .comicBorder(
                        width = ComicShapeTokens.BorderWidthThin,
                        color = ComicTokens.SolidBlack,
                        shape = badgeShape
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tier.badgeEmoji,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = tierLabel,
                        fontFamily = BangersFontFamily,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp,
                        color = ComicTokens.SolidBlack
                    )
                }
            }
        }
    }
}
