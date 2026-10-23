package com.pixelquest.app.notification

import android.content.Context
import android.view.View
import android.widget.RemoteViews
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
 * How every PixelQuest notification looks: the PixelQuest mark as small icon, the player's skin's
 * accent, the expressive Pix mascot in the appropriate mood (praying, focus, worried, celebrate, onfire),
 * and rich comic cards with custom views.
 */
object PqNotifications {

    enum class CardBg { Gold, Paper, Sky }

    /** The PixelQuest mark, in white: Android tints it with [accent]. */
    val SMALL_ICON = R.drawable.ic_stat_pixelquest

    /**
     * The skin's red for text: Android tints the icon and the app's name with it, and it reads on a
     * light shade (4.5:1) and a dark one (3:1) in both skins.
     */
    fun accent(context: Context): Int = palette(context).primaryText.toArgb()

    /**
     * A notification on [channelId] with PixelQuest's icon, accent, and expressive Pix mascot [mood].
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
        NotificationCompat.Builder(context, channelId).setSmallIcon(SMALL_ICON).setColor(accent(context))
            .setContentTitle(context.getString(R.string.notif_hidden_title))
            .build()

    /**
     * The rich comic card with the Pix mascot, small and expanded, for [NotificationCompat.DecoratedCustomViewStyle]:
     * a [kicker] over a big [title], then [body], the [mood] mascot graphic, and an optional [progress] bar.
     */
    fun card(
        context: Context,
        kicker: String,
        title: String,
        body: String,
        progress: Int? = null,
        mood: PixMood = PixMood.Celebrate,
        bg: CardBg = CardBg.Gold,
    ): Pair<RemoteViews, RemoteViews> {
        val pixel = skin(context) == PqSkin.Pixel
        val bgRes = when (bg) {
            CardBg.Gold -> if (pixel) R.drawable.pix_card_gold_pixel else R.drawable.pix_card_gold
            CardBg.Paper -> if (pixel) R.drawable.pix_card_paper_pixel else R.drawable.pix_card_paper
            CardBg.Sky -> if (pixel) R.drawable.pix_card_sky_pixel else R.drawable.pix_card_sky
        }
        val density = try {
            context.resources.displayMetrics.density
        } catch (_: Exception) {
            2.5f
        }
        val mascotBitmap = PqMascotBitmap.render(mood, (72 * density).toInt(), pixel)

        fun views() = RemoteViews(context.packageName, R.layout.notif_pix_card).apply {
            setInt(R.id.pix_card, "setBackgroundResource", bgRes)
            setTextViewText(R.id.pix_kicker, kicker)
            setTextViewText(R.id.pix_title, title)
            setTextViewText(R.id.pix_body, body)
            if (pixel || bg != CardBg.Gold) {
                setTextColor(R.id.pix_title, if (pixel) 0xFF2B2622.toInt() else 0xFF161412.toInt())
            }
            setImageViewBitmap(R.id.pix_mascot, mascotBitmap)
            if (progress != null) {
                setViewVisibility(R.id.pix_progress, View.VISIBLE)
                setProgressBar(R.id.pix_progress, 100, progress, false)
            } else {
                setViewVisibility(R.id.pix_progress, View.GONE)
            }
        }
        val small = views().apply {
            setViewVisibility(R.id.pix_body, View.GONE)
            setViewVisibility(R.id.pix_progress, View.GONE)
        }
        return small to views()
    }

    /** Triggers an immediate test notification with Pix mascot and rich comic card for previewing. */
    fun previewTestNotification(context: Context) {
        val (small, big) = card(
            context,
            kicker = "PIX MASCOT · DUOLINGO STYLE",
            title = "STREAK ON FIRE!",
            body = "Pix is celebrating your 7-day streak! Keep it going today.",
            mood = PixMood.OnFire,
            bg = CardBg.Gold,
            progress = 70,
        )
        val notif = builder(context, NotificationChannels.Spec.PROGRESS.id, mood = PixMood.OnFire)
            .setContentTitle("Streak on fire! 7 days")
            .setContentText("Pix is celebrating your 7-day streak!")
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(small)
            .setCustomBigContentView(big)
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
