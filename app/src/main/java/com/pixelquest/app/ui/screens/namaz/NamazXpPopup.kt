package com.pixelquest.app.ui.screens.namaz

import com.pixelquest.app.ui.components.comic
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.ui.theme.LocalReduceMotion
import com.pixelquest.app.ui.theme.rememberReduceMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** XP just earned by marking [prayer], to show for a moment; [id] makes each one animate anew. */
data class NamazXpPopup(val id: Long, val prayer: Prayer, val xp: Int)

/**
 * "+10 XP", rising and fading over the screen after a prayer is marked: a gold v2 badge. With
 * reduced motion it appears and fades without moving. [onFinished] clears it.
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
    Text(
        text = text.uppercase(),
        style = PqType.CardTitle,
        modifier = modifier
            .graphicsLayer { translationY = rise.value.dp.toPx() }
            .alpha(fade.value)
            .testTag(NamazTestTags.XP_POPUP)
            .comic(bg = PqColors.Gold, radius = 14.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    )
}
