package com.pixelquest.app.notification

import android.content.Context
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pixelquest.app.R
import com.pixelquest.app.data.repository.SettingsRepositoryImpl
import com.pixelquest.app.mascot.PixMood
import com.pixelquest.app.mascot.PqMascotBitmap
import com.pixelquest.app.ui.theme.ComicSpec
import com.pixelquest.app.ui.theme.PixelSpec
import com.pixelquest.app.ui.theme.PqPalette
import com.pixelquest.app.ui.theme.PqSkin

/**
 * How every PixelQuest notification looks:
 *  - Pix mascot silhouette as the small icon (pure white on transparent, no sword).
 *  - Single expressive Pix mascot in the appropriate mood as the large icon (like Duolingo).
 *  - Clean native Android typography (BigTextStyle) with full-width text flow.
 */
object PqNotifications {

    /** Pix mascot silhouette (no sword): Android tints it with [accent]. */
    val SMALL_ICON = R.drawable.ic_stat_pix

    /**
     * The skin's red for text: Android tints the icon and the app's name with it, and it reads on a
     * light shade (4.5:1) and a dark one (3:1) in both skins.
     */
    fun accent(context: Context): Int = palette(context).primaryText.toArgb()

    /**
     * A notification on [channelId] with PixelQuest's Pix silhouette icon, accent, and a single expressive
     * Pix mascot [mood] on the right.
     * It is private (S047): when the phone hides sensitive content on the lock screen, it shows only
     * [lockScreenVersion], never a quest's name, a prayer or a message.
     */
    fun builder(
        context: Context,
        channelId: String,
        mood: PixMood = PixMood.Happy,
    ): NotificationCompat.Builder {
        val pixel = skin(context) == PqSkin.Pixel
        val largePx = try {
            context.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)
        } catch (_: Exception) {
            96
        }
        val mascotBitmap = PqMascotBitmap.largeIcon(mood, largePx, pixel)

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(SMALL_ICON)
            .setColor(accent(context))
            .setLargeIcon(mascotBitmap)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(lockScreenVersion(context, channelId))
    }

    /** What a locked phone shows instead: a generic title, no text. */
    fun lockScreenVersion(context: Context, channelId: String): android.app.Notification =
        NotificationCompat.Builder(context, channelId)
            .setSmallIcon(SMALL_ICON)
            .setColor(accent(context))
            .setContentTitle(context.getString(R.string.notif_hidden_title))
            .build()

    /** Triggers an immediate test notification with Pix mascot (Duolingo style: single mascot, no sword, clean native typography). */
    fun previewTestNotification(context: Context) {
        val title = "Streak on fire! 🔥 7 days"
        val text = "Pix is celebrating your 7-day streak! Keep it going today."
        val bigText = "Pix is celebrating your 7-day streak! Keep it going today. Complete one quest before midnight to keep your flame burning."

        val notif = builder(context, NotificationChannels.Spec.PROGRESS.id, mood = PixMood.OnFire)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(999_888, notif)
        } catch (_: SecurityException) {
        }
    }

    /** Settings > Look, as stored: a notification is often built when no screen is open. */
    fun skin(context: Context): PqSkin = SettingsRepositoryImpl.storedThemeMode(context).skin

    private fun palette(context: Context): PqPalette = if (skin(context) == PqSkin.Pixel) PixelSpec.palette else ComicSpec.palette
}
