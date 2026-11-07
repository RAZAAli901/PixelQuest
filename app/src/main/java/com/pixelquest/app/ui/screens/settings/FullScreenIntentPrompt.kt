package com.pixelquest.app.ui.screens.settings

import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.pixelquest.app.R
import com.pixelquest.app.notification.FullScreenIntentPermission
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqButtonStyle
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqType

/**
 * On Android 14+ (API 34+), USE_FULL_SCREEN_INTENT is a special app access permission.
 * Displays current status and opens the system grant screen when tapped.
 */
@Composable
fun FullScreenIntentPrompt(
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return

    var allowed by remember { mutableStateOf(FullScreenIntentPermission.canUseFullScreenIntent(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                allowed = FullScreenIntentPermission.canUseFullScreenIntent(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val onOpenSettings = {
        context.startActivity(FullScreenIntentPermission.openSettingsIntent(context))
    }

    if (allowed) {
        SettingLink(
            icon = Icons.Rounded.NotificationsActive,
            title = stringResource(R.string.settings_full_screen_title),
            onClick = onOpenSettings,
            value = stringResource(R.string.settings_full_screen_allowed),
        )
    } else {
        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SettingLink(
                icon = Icons.Rounded.NotificationsActive,
                title = stringResource(R.string.settings_full_screen_title),
                onClick = onOpenSettings,
                value = stringResource(R.string.settings_full_screen_denied),
            )
            Text(
                text = stringResource(R.string.settings_full_screen_prompt_text),
                style = PqType.Meta.copy(color = PqColors.Ink2),
            )
            PqButton(
                text = stringResource(R.string.settings_full_screen_allow_btn),
                onClick = onOpenSettings,
                style = PqButtonStyle.Secondary,
                icon = Icons.Rounded.NotificationsActive,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
