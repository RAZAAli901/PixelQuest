package com.pixelquest.app.ui.screens.namaz

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.R
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.ui.theme.BangersFontFamily
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.LocalReduceMotion
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.ThemeMode
import com.pixelquest.app.ui.theme.comicDropShadow
import com.pixelquest.app.ui.theme.rememberReduceMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** XP just earned by marking [prayer], to show for a moment; [id] makes each one animate anew. */
data class NamazXpPopup(val id: Long, val prayer: Prayer, val xp: Int)

/**
 * "+10 XP", rising and fading over the screen after a prayer is marked, in the theme's look: a
 * coral comic bubble in Comic, gold pixel text in Pixel, plain in Light. With reduced motion it
 * appears and fades without moving. [onFinished] clears it.
 */
@Composable
fun NamazXpPopupView(popup: NamazXpPopup, onFinished: () -> Unit, modifier: Modifier = Modifier) {
    val reduceMotion = rememberReduceMotion(LocalReduceMotion.current)
    val currentOnFinished = rememberUpdatedState(onFinished)
    val rise = remember(popup.id) { Animatable(0f) }
    val fade = remember(popup.id) { Animatable(1f) }
    LaunchedEffect(popup.id) {
        if (!reduceMotion) launch { rise.animateTo(-48f, tween(durationMillis = 900)) }
        delay(700)
        fade.animateTo(0f, tween(durationMillis = 300))
        currentOnFinished.value()
    }

    val text = stringResource(R.string.namaz_xp_popup, popup.xp)
    val colors = PixelTheme.colors
    val placed = modifier
        .graphicsLayer { translationY = rise.value.dp.toPx() }
        .alpha(fade.value)
        .testTag(NamazTestTags.XP_POPUP)
    when (PixelTheme.mode) {
        ThemeMode.Comic -> {
            val shape = RoundedCornerShape(14.dp)
            Text(
                text = text.uppercase(),
                fontFamily = BangersFontFamily,
                fontSize = 26.sp,
                color = ComicTokens.SolidBlack,
                modifier = placed
                    .comicDropShadow(offsetX = 3.dp, offsetY = 3.dp, shape = shape)
                    .background(ComicTokens.CoralRed, shape)
                    .border(2.5.dp, ComicTokens.SolidBlack, shape)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
        ThemeMode.Pixel, ThemeMode.System -> Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            color = colors.primary,
            modifier = placed
                .background(colors.background.copy(alpha = 0.85f), RectangleShape)
                .border(2.dp, colors.primary, RectangleShape)
                .padding(horizontal = 14.dp, vertical = 6.dp)
        )
        else -> Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = colors.primaryText,
            modifier = placed
                .background(colors.surface, RoundedCornerShape(20.dp))
                .border(1.dp, colors.primaryText.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        )
    }
}
