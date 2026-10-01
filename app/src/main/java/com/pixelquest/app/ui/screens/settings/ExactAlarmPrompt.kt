package com.pixelquest.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelButtonVariant
import com.pixelquest.app.ui.theme.PixelTheme

/**
 * Shown while Android won't let PixelQuest set exact alarms. Re-checks on resume, so it disappears
 * after the user grants "Alarms & reminders" and comes back.
 */
@Composable
fun ExactAlarmPrompt(canScheduleExact: () -> Boolean, onAllow: () -> Unit) {
    var allowed by remember { mutableStateOf(canScheduleExact()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) allowed = canScheduleExact()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    if (allowed) return

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Reminders can arrive up to 10 minutes late (longer if your phone is idle). Allow \"Alarms & reminders\" so they fire on time.",
            style = MaterialTheme.typography.bodySmall,
            color = PixelTheme.colors.onSurface
        )
        PixelButton(
            text = "⏰ ALLOW EXACT REMINDER TIMES",
            onClick = onAllow,
            variant = PixelButtonVariant.YELLOW,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
