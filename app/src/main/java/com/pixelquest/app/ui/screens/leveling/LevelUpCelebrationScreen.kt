package com.pixelquest.app.ui.screens.leveling

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqCard
import com.pixelquest.app.ui.navigation.PixelTransitions
import com.pixelquest.app.ui.theme.LocalReduceMotion
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType

/**
 * A new level (UI v2): a gold card over the screen, with the level in large type, a fanfare, and
 * Continue. It pulses gently unless the player asked for less motion; Back closes it, and taps
 * outside it don't reach the quests underneath.
 */
@Composable
fun LevelUpCelebrationScreen(level: Int, onDismiss: () -> Unit) {
    val scale = if (LocalReduceMotion.current) {
        1f
    } else {
        rememberInfiniteTransition(label = "levelUp").animateFloat(
            initialValue = 1f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(animation = tween(durationMillis = 800), repeatMode = RepeatMode.Reverse),
            label = "levelUpPulse",
        ).value
    }
    val soundManager = com.pixelquest.app.audio.LocalSoundManager.current
    LaunchedEffect(Unit) { soundManager?.playLevelUpSound() }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    // Back closes the celebration instead of the app.
    androidx.activity.compose.BackHandler(onBack = onDismiss)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x8C161412))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
    ) {
        AnimatedVisibility(visible = visible, enter = PixelTransitions.LevelUpEnter, exit = PixelTransitions.LevelUpExit) {
            PqCard(bg = PqColors.Gold, shadow = PqDimens.ShadowLg, radius = 22.dp, modifier = Modifier.padding(24.dp).scale(scale)) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(24.dp),
                ) {
                    Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = PqColors.Ink, modifier = Modifier.size(48.dp))
                    Text(text = stringResource(R.string.pq_level_up_label), style = PqType.Label, modifier = Modifier.semantics { heading() })
                    Text(text = stringResource(R.string.pq_level_number_upper, level), style = PqType.Hero)
                    Text(text = stringResource(R.string.pq_level_up_congrats), style = PqType.Body)
                    PqButton(stringResource(R.string.pq_level_up_continue), onDismiss, Modifier.fillMaxWidth().padding(top = 6.dp), height = 52.dp)
                }
            }
        }
    }
}
