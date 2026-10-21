package com.pixelquest.app.push

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pixelquest.app.MainActivity
import com.pixelquest.app.R
import com.pixelquest.app.notification.NotificationChannels
import com.pixelquest.app.notification.PqNotifications
import com.pixelquest.app.ui.navigation.Screen

/**
 * A chess push, shown (J123): what it says is built here from its kind, never taken from the
 * push, and it's hidden on the lock screen. A push is only ids, and only well-formed ones are used:
 * anything else is dropped. Tapping opens the challenge or the game.
 */
object ChessPushNotifier {
    private val UUID = Regex("^[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}$")

    /** What a push becomes: its text and where it opens, or null for one that isn't ours. */
    data class Shown(val title: Int, val text: Int, val route: String, val id: Int)

    fun shownFor(data: Map<String, String>): Shown? {
        fun id(key: String) = data[key]?.takeIf { UUID.matches(it) }
        return when (data["kind"]) {
            "challenge_received" -> id("challenge_id")?.let {
                Shown(R.string.chess_push_challenge_title, R.string.chess_push_challenge_text, Screen.ChessChallenge.createRoute(it), it.hashCode())
            }
            "challenge_accepted" -> id("game_id")?.let {
                Shown(R.string.chess_push_accepted_title, R.string.chess_push_accepted_text, Screen.ChessGame.createRoute(it), it.hashCode())
            }
            "draw_offered" -> id("game_id")?.let {
                Shown(R.string.chess_push_draw_title, R.string.chess_push_draw_text, Screen.ChessGame.createRoute(it), it.hashCode() + 1)
            }
            else -> null
        }
    }

    fun show(context: Context, data: Map<String, String>) {
        val shown = shownFor(data) ?: return
        val open = PendingIntent.getActivity(
            context, shown.id,
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN_ROUTE, shown.route)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val channel = NotificationChannels.Spec.CHESS.id
        val hidden = PqNotifications.builder(context, channel).setContentTitle(context.getString(R.string.chess_push_hidden)).build()
        val notification = PqNotifications.builder(context, channel)
            .setContentTitle(context.getString(shown.title))
            .setContentText(context.getString(shown.text))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(hidden)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setContentIntent(open)
            .setAutoCancel(true)
            // A challenge lasts 10 minutes: its notification goes with it.
            .setTimeoutAfter(10 * 60 * 1000L)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(960_000 + (shown.id and 0xFFFF), notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted.
        }
    }
}
