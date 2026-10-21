package com.pixelquest.app.darood.sync

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pixelquest.app.MainActivity
import com.pixelquest.app.R
import com.pixelquest.app.darood.domain.WeekResult
import com.pixelquest.app.notification.NotificationChannels
import com.pixelquest.app.notification.PqNotifications
import com.pixelquest.app.party.domain.Party
import com.pixelquest.app.ui.navigation.Screen
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** The week's notification on the Darood channel: one per party, opening that party. */
class AndroidWeekResultNotifier @Inject constructor(@ApplicationContext private val context: Context) : WeekResultNotifier {

    override fun post(party: Party, week: WeekResult, me: String, names: Map<String, String?>) {
        val open = PendingIntent.getActivity(
            context, party.id.hashCode(),
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN_ROUTE, Screen.PartiesParty.createRoute(party.id))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = PqNotifications.builder(context, NotificationChannels.Spec.DAROOD.id)
            .setContentTitle(context.getString(R.string.darood_week_notif_title, party.name))
            .setContentText(context.getString(R.string.darood_week_notif_ready))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(notificationId(party.id), notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted.
        }
    }

    companion object {
        fun notificationId(partyId: String) = 940_000 + (partyId.hashCode() and 0xFFFF)
    }
}
