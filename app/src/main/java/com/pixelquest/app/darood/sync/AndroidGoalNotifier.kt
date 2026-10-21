package com.pixelquest.app.darood.sync

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pixelquest.app.MainActivity
import com.pixelquest.app.R
import com.pixelquest.app.darood.ui.formatCount
import com.pixelquest.app.notification.NotificationChannels
import com.pixelquest.app.notification.PqNotifications
import com.pixelquest.app.ui.navigation.Screen
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Goal reached and New goal set, on the Darood channel, opening the party's room. */
class AndroidGoalNotifier @Inject constructor(@ApplicationContext private val context: Context) : GoalNotifier {

    override fun post(news: GoalNews) {
        val party = news.party
        val open = PendingIntent.getActivity(
            context, party.id.hashCode() + 1,
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN_ROUTE, Screen.DaroodRoom.createRoute(party.id))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val (title, text) = when (news) {
            is GoalNews.Reached -> context.getString(R.string.darood_goal_notif_reached, news.goal.sequence, party.name) to
                context.getString(R.string.darood_goal_notif_reached_text, formatCount(news.goal.target))
            is GoalNews.NewGoal -> context.getString(R.string.darood_goal_notif_new, party.name) to
                context.getString(R.string.darood_goal_notif_new_text, news.goal.sequence, formatCount(news.goal.target))
        }
        val hidden = PqNotifications.builder(context, NotificationChannels.Spec.DAROOD.id).setContentTitle(context.getString(R.string.darood_goal_notif_hidden)).build()
        val notification = PqNotifications.builder(context, NotificationChannels.Spec.DAROOD.id)
            .setContentTitle(title)
            .setContentText(text)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(hidden)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(950_000 + ((party.id.hashCode() * 31 + news.goal.sequence) and 0xFFFF), notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted.
        }
    }
}
