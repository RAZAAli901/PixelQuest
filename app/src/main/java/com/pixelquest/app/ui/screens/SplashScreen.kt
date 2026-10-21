package com.pixelquest.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.ui.components.PqProgressBar
import com.pixelquest.app.ui.components.comic
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType

/**
 * The app's first frame (UI v2): its sword-badge mark on red, as the launcher icon has it, the name,
 * and a bar that fills while the app gets ready, then [onSplashTimeout].
 */
@Composable
fun SplashScreen(onSplashTimeout: () -> Unit = {}) {
    val progress = remember { Animatable(0f) }
    val done by rememberUpdatedState(onSplashTimeout)
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(durationMillis = 1500))
        done()
    }
    Box(modifier = Modifier.fillMaxSize().background(PqColors.Ground), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(32.dp)) {
            Box(Modifier.size(132.dp).comic(bg = PqColors.Red, radius = 32.dp, shadow = PqDimens.ShadowLg), contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.size(132.dp))
            }
            Text(text = "PIXELQUEST", style = PqType.Hero)
            Text(text = "Your day, as quests", style = PqType.Meta.copy(color = PqColors.Ink2))
            PqProgressBar(progress.value, modifier = Modifier.fillMaxWidth(0.6f))
        }
    }
}
