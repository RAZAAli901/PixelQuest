package com.pixelquest.app.ui.screens.leveling

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.ui.components.ComicButton
import com.pixelquest.app.ui.components.ComicButtonVariant
import com.pixelquest.app.ui.components.ComicPanel
import com.pixelquest.app.ui.components.ComicPanelVariant
import com.pixelquest.app.ui.theme.BangersFontFamily
import com.pixelquest.app.ui.theme.ComicShapeTokens
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.comicBorder
import com.pixelquest.app.ui.theme.comicDropShadow
import kotlin.math.cos
import kotlin.math.sin

/**
 * Step 32: Comic-styled visual treatment for LevelUpCelebration.
 * Features:
 * - Radial comic action-line burst background
 * - 14-point gold action starburst banner ("LEVEL UP!") in Bangers typography
 * - Crisp comic panel with bold level callout
 * - Comic primary action button
 */
@Composable
fun ComicLevelUpCelebration(
    level: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    scale: Float = 1f
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ComicTokens.PaperBackground.copy(alpha = 0.94f)),
        contentAlignment = Alignment.Center
    ) {
        // Background comic radial action lines
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
                drawPath(path, color = ComicTokens.SolidBlack.copy(alpha = 0.04f))
            }
        }

        // Central Celebration Card
        Column(
            modifier = Modifier
                .padding(24.dp)
                .scale(scale),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Starburst "LEVEL UP!" Badge
            Box(
                modifier = Modifier
                    .size(220.dp, 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val w = size.width
                    val h = size.height
                    val points = 16
                    val angleStep = (2f * Math.PI / points).toFloat()
                    val halfStep = angleStep / 2f

                    val path = Path()
                    for (i in 0 until points) {
                        val outerAngle = i * angleStep - (Math.PI / 2f).toFloat()
                        val innerAngle = outerAngle + halfStep
                        val ox = cx + (w / 2f - 4f) * cos(outerAngle)
                        val oy = cy + (h / 2f - 4f) * sin(outerAngle)
                        val ix = cx + (w / 2f * 0.78f) * cos(innerAngle)
                        val iy = cy + (h / 2f * 0.78f) * sin(innerAngle)

                        if (i == 0) path.moveTo(ox, oy) else path.lineTo(ox, oy)
                        path.lineTo(ix, iy)
                    }
                    path.close()

                    // Flat black shadow
                    drawContext.canvas.save()
                    drawContext.canvas.translate(4f.toDp().toPx(), 4f.toDp().toPx())
                    drawPath(path, color = ComicTokens.SolidBlack)
                    drawContext.canvas.restore()

                    // Gold burst fill
                    drawPath(path, color = ComicTokens.GoldAccent)

                    // Solid black ink outline
                    drawPath(
                        path = path,
                        color = ComicTokens.SolidBlack,
                        style = Stroke(width = 3f.toDp().toPx())
                    )
                }

                Text(
                    text = "LEVEL UP!",
                    fontFamily = BangersFontFamily,
                    fontSize = 34.sp,
                    letterSpacing = 1.2.sp,
                    color = ComicTokens.SolidBlack
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Comic Panel Body
            ComicPanel(
                variant = ComicPanelVariant.SURFACE,
                contentPadding = 24.dp,
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "NEW POWER UNLOCKED!",
                        fontFamily = BangersFontFamily,
                        fontSize = 16.sp,
                        letterSpacing = 0.8.sp,
                        color = ComicTokens.CoralRed
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "YOU ARE NOW LEVEL",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = ComicTokens.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$level",
                        fontFamily = BangersFontFamily,
                        fontSize = 58.sp,
                        letterSpacing = 1.sp,
                        color = ComicTokens.SolidBlack
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    ComicButton(
                        text = "CONTINUE QUEST",
                        onClick = onDismiss,
                        variant = ComicButtonVariant.PRIMARY,
                        modifier = Modifier.fillMaxWidth(0.85f)
                    )
                }
            }
        }
    }
}
