package com.pixelquest.app.ui.screens.namaz

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.pixelquest.app.R
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelButtonVariant
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.inkOnPanel

/**
 * What the phone might be doing to the prayer reminders, each with a way to fix it
 * (docs/namaz-design.md, Reminders, Permissions). Shown while any prayer's reminder is on.
 * Each check runs again when the screen comes back, so a card goes once it's fixed in Android's settings.
 */
@Composable
fun NamazReminderChecks(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var exactAllowed by remember { mutableStateOf(canScheduleExactAlarms(context)) }
    OnResume { exactAllowed = canScheduleExactAlarms(context) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!exactAllowed) {
            NamazCheckCard(
                message = stringResource(R.string.namaz_exact_alarm_message),
                action = stringResource(R.string.namaz_exact_alarm_allow),
                onAction = { context.startActivity(exactAlarmSettingsIntent(context)) },
                modifier = Modifier.testTag(NamazTestTags.EXACT_ALARM_CARD)
            )
        }
    }
}

/** Without "Alarms & reminders" (Android 12+), reminders come within a 10-minute window instead. */
internal fun canScheduleExactAlarms(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

/** Android's "Alarms & reminders" page for this app. */
internal fun exactAlarmSettingsIntent(context: Context): Intent =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.fromParts("package", context.packageName, null))
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

/** Runs [block] each time the screen comes back to the front. */
@Composable
internal fun OnResume(block: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) block() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

/** A warning with the one thing to do about it. */
@Composable
internal fun NamazCheckCard(message: String, action: String, onAction: () -> Unit, modifier: Modifier = Modifier) {
    val colors = PixelTheme.colors
    PixelCard(variant = PixelPanelVariant.BORDER, contentPadding = namazStyle().cardPadding, modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = "⚠️ $message", style = MaterialTheme.typography.bodyMedium, color = inkOnPanel(colors.onSurface))
            PixelButton(text = action, onClick = onAction, variant = PixelButtonVariant.YELLOW, modifier = Modifier.fillMaxWidth())
        }
    }
}
