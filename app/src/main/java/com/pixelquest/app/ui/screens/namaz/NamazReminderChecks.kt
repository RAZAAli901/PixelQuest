package com.pixelquest.app.ui.screens.namaz

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqButtonStyle
import com.pixelquest.app.ui.components.PqCard
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.PowerManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.pixelquest.app.R
import com.pixelquest.app.namaz.NamazChannels
import com.pixelquest.app.ui.components.DismissButton

/**
 * What the phone might be doing to the prayer reminders, each with a way to fix it
 * (docs/namaz-design.md, Reminders, Permissions). Shown while any prayer's reminder is on.
 * Each check runs again when the screen comes back, so a card goes once it's fixed in Android's settings.
 * The battery tip is only advice, so it can be hidden for good ([onDismissBatteryTip]).
 */
@Composable
fun NamazReminderChecks(
    modifier: Modifier = Modifier,
    batteryTipDismissed: Boolean = false,
    onDismissBatteryTip: () -> Unit = {}
) {
    val context = LocalContext.current
    var exactAllowed by remember { mutableStateOf(canScheduleExactAlarms(context)) }
    var notificationsShown by remember { mutableStateOf(canShowPrayerReminders(context)) }
    var batteryUnlimited by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }
    OnResume {
        exactAllowed = canScheduleExactAlarms(context)
        notificationsShown = canShowPrayerReminders(context)
        batteryUnlimited = isIgnoringBatteryOptimizations(context)
    }
    // Android's own prompt once; after a refusal it won't show again, so the button opens the settings.
    var askedOnce by remember { mutableStateOf(false) }
    val askForNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsShown = canShowPrayerReminders(context)
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!notificationsShown) {
            NamazCheckCard(
                message = stringResource(R.string.namaz_notifications_message),
                action = stringResource(R.string.namaz_notifications_allow),
                onAction = {
                    if (needsNotificationPermission(context) && !askedOnce) {
                        askedOnce = true
                        askForNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        context.startActivity(notificationSettingsIntent(context))
                    }
                },
                modifier = Modifier.testTag(NamazTestTags.NOTIFICATIONS_CARD)
            )
        }
        if (!exactAllowed) {
            NamazCheckCard(
                message = stringResource(R.string.namaz_exact_alarm_message),
                action = stringResource(R.string.namaz_exact_alarm_allow),
                onAction = { context.startActivity(exactAlarmSettingsIntent(context)) },
                modifier = Modifier.testTag(NamazTestTags.EXACT_ALARM_CARD)
            )
        }
        if (!batteryUnlimited && !batteryTipDismissed) {
            NamazCheckCard(
                message = stringResource(R.string.namaz_battery_message),
                action = stringResource(R.string.namaz_battery_open),
                onAction = { context.startActivity(batteryOptimizationSettingsIntent()) },
                onDismiss = onDismissBatteryTip,
                modifier = Modifier.testTag(NamazTestTags.BATTERY_CARD)
            )
        }
    }
}

/**
 * Whether a prayer reminder would appear: notifications allowed for the app (Android 13+'s
 * permission, or the app's switch in Android's settings), and the prayer channel not blocked.
 */
internal fun canShowPrayerReminders(context: Context): Boolean {
    if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
    val channel = context.getSystemService(NotificationManager::class.java).getNotificationChannel(NamazChannels.REMINDERS)
    return channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE
}

/** Android 13+ asks for notifications at run time. */
internal fun needsNotificationPermission(context: Context): Boolean = com.pixelquest.app.ui.notifications.NotificationAsk.needed(context)

/** Android's notification settings for this app. */
internal fun notificationSettingsIntent(context: Context): Intent =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

/** Whether Android leaves PixelQuest out of battery optimisation, so its alarms aren't held back. */
internal fun isIgnoringBatteryOptimizations(context: Context): Boolean =
    context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

/**
 * Android's list of apps and their battery optimisation, where the player can pick PixelQuest.
 * (Asking for the exemption directly needs a permission Play keeps for a few kinds of app.)
 */
internal fun batteryOptimizationSettingsIntent(): Intent =
    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

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

/** A warning with the one thing to do about it, and for advice, a way to hide it ([onDismiss]). */
@Composable
internal fun NamazCheckCard(
    message: String,
    action: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null
) {
    PqCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = PqColors.Ink, modifier = Modifier.padding(top = 2.dp).size(22.dp))
                Text(text = message, style = PqType.Body, modifier = Modifier.weight(1f))
                if (onDismiss != null) {
                    DismissButton(
                        contentDescription = stringResource(R.string.namaz_battery_dismiss),
                        onClick = onDismiss,
                        color = PqColors.Muted
                    )
                }
            }
            PqButton(text = action, onClick = onAction, style = PqButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
        }
    }
}
