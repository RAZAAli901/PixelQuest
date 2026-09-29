package com.pixelquest.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.ui.components.ComicPanel
import com.pixelquest.app.ui.components.ComicPanelVariant
import com.pixelquest.app.ui.components.ComicProgressBar
import com.pixelquest.app.ui.components.PixelAvatarFrame
import com.pixelquest.app.ui.theme.BangersFontFamily
import com.pixelquest.app.ui.theme.ComicShapeTokens
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.ComicTypography
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import kotlin.math.cos
import kotlin.math.sin

/**
 * Step 7 (Day 23): Comic-styled Splash Screen Variant.
 *
 * Implements full comic pop-art splash presentation:
 * - Radial comic action-line burst background radiating across aged newsprint.
 * - Bold Bangers logo text with vibrant Coral Red header.
 * - Solid 3.5dp black ink border and 6dp flat drop-shadow panel cell.
 * - Integrated comic avatar frame and comic dynamic progress energy bar.
 */
@Composable
fun ComicSplashScreen(
    onSplashTimeout: () -> Unit = {}
) {
    val progressAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progressAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1500)
        )
        onSplashTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComicTokens.PaperBackground),
        contentAlignment = Alignment.Center
    ) {
        // Procedural radial comic burst rays
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val maxR = size.maxDimension
            val rayCount = 24
            val angleStep = (2f * Math.PI / rayCount).toFloat()

            for (i in 0 until rayCount step 2) {
                val startAngle = i * angleStep
                val endAngle = (i + 1) * angleStep
                val path = Path().apply {
                    moveTo(cx, cy)
                    lineTo(cx + maxR * cos(startAngle), cy + maxR * sin(startAngle))
                    lineTo(cx + maxR * cos(endAngle), cy + maxR * sin(endAngle))
                    close()
                }
                drawPath(path, color = ComicTokens.SolidBlack.copy(alpha = 0.05f))
            }
        }

        // Central comic panel cell
        ComicPanel(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp),
            variant = ComicPanelVariant.SURFACE,
            borderWidth = ComicShapeTokens.BorderWidthThick,
            shadowOffset = ComicShapeTokens.ShadowOffsetLarge,
            cornerRadius = ComicShapeTokens.RadiusLarge,
            contentPadding = 24.dp
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "PIXELQUEST",
                    fontFamily = BangersFontFamily,
                    fontSize = 38.sp,
                    color = ComicTokens.CoralRed,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                PixelAvatarFrame(
                    avatarId = "avatar_hero",
                    level = 10,
                    size = 80.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "COMIC POP HABIT QUEST",
                    fontFamily = BangersFontFamily,
                    fontSize = 18.sp,
                    color = ComicTokens.SolidBlack,
                    letterSpacing = 0.8.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "POWERING UP...",
                    style = ComicTypography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = ComicTokens.TextPrimary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                ComicProgressBar(
                    progress = progressAnim.value,
                    modifier = Modifier.fillMaxWidth(0.92f),
                    height = 20.dp
                )
            }
        }
    }
}

@Preview(name = "Splash Screen - Comic Mode", showBackground = true)
@Composable
private fun ComicSplashScreenPreview() {
    PixelQuestTheme(themeMode = ThemeMode.Comic) {
        ComicSplashScreen()
    }
}
