package com.pixelquest.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.pixelquest.app.R
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqButtonStyle
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqType

/**
 * Shown under notification settings to manage exact alarms permission.
 * Displays "Allowed" link when granted, or a guidance card with an action button
 * when exact alarm permission is missing. Re-checks on resume.
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

    if (allowed) {
        SettingLink(
            icon = Icons.Rounded.Alarm,
            title = stringResource(R.string.settings_exact_alarms_title),
            onClick = onAllow,
            value = stringResource(R.string.settings_exact_alarms_allowed),
        )
    } else {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SettingLink(
                icon = Icons.Rounded.Alarm,
                title = stringResource(R.string.settings_exact_alarms_title),
                onClick = onAllow,
                value = stringResource(R.string.settings_exact_alarms_denied),
            )
            Text(
                text = stringResource(R.string.settings_exact_alarms_prompt_text),
                style = PqType.Meta.copy(color = PqColors.Ink2),
            )
            PqButton(
                text = stringResource(R.string.settings_exact_alarms_allow_btn),
                onClick = onAllow,
                style = PqButtonStyle.Secondary,
                icon = Icons.Rounded.Alarm,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
