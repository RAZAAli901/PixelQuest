package com.pixelquest.app.notification

import android.content.Context
import android.widget.RemoteViews
import android.view.View
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import com.pixelquest.app.R
import com.pixelquest.app.data.repository.SettingsRepositoryImpl
import com.pixelquest.app.ui.theme.ComicSpec
import com.pixelquest.app.ui.theme.PixelSpec
import com.pixelquest.app.ui.theme.PqPalette
import com.pixelquest.app.ui.theme.PqSkin

/**
 * How every PixelQuest notification looks (UI v2, mocks 1D to 1F; docs/ui-v2.md): the PixelQuest
 * mark as its small icon, the player's skin's red as its accent, and for a moment worth a card (a
 * streak at risk) the comic card the design draws, in that skin. The channels stay
 * [NotificationChannels] and NamazChannels, so each player's settings for them are kept.
 */
object PqNotifications {

    /** The PixelQuest mark, in white: Android tints it with [accent]. */
    val SMALL_ICON = R.drawable.ic_stat_pixelquest

    /**
     * The skin's red for text: Android tints the icon and the app's name with it, and it reads on a
     * light shade (4.5:1) and a dark one (3:1) in both skins.
     */
    fun accent(context: Context): Int = palette(context).primaryText.toArgb()

    /**
     * A notification on [channelId] with PixelQuest's icon and accent. It is private (S047): when the
     * phone hides sensitive content on the lock screen, it shows only [lockScreenVersion], never a
     * quest's name, a prayer or a message. A notifier with its own wording sets its own public version.
     */
    fun builder(context: Context, channelId: String): NotificationCompat.Builder =
        NotificationCompat.Builder(context, channelId).setSmallIcon(SMALL_ICON).setColor(accent(context))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(lockScreenVersion(context, channelId))

    /** What a locked phone shows instead: a generic title, no text. */
    fun lockScreenVersion(context: Context, channelId: String): android.app.Notification =
        NotificationCompat.Builder(context, channelId).setSmallIcon(SMALL_ICON).setColor(accent(context))
            .setContentTitle(context.getString(R.string.notif_hidden_title))
            .build()

    /**
     * The comic card (mocks 1D and 1F), small and expanded, for [NotificationCompat.DecoratedCustomViewStyle]:
     * a [kicker] over a big [title], then [body], and a bar at [progress] percent if there is one.
     */
    fun card(context: Context, kicker: String, title: String, body: String, progress: Int? = null): Pair<RemoteViews, RemoteViews> {
        val pixel = skin(context) == PqSkin.Pixel
        val primary = palette(context).primary.toArgb()
        val big = RemoteViews(context.packageName, R.layout.notif_pq_big).apply {
            setTextViewText(R.id.pq_kicker, kicker)
            setTextViewText(R.id.pq_title, title)
            setTextViewText(R.id.pq_body, body)
            if (pixel) {
                setInt(R.id.pq_card, "setBackgroundResource", R.drawable.notif_card_bg_pixel)
                setTextColor(R.id.pq_title, primary)
            }
            if (progress != null) {
                setViewVisibility(R.id.pq_progress, View.VISIBLE)
                setProgressBar(R.id.pq_progress, 100, progress, false)
            }
        }
        val small = RemoteViews(context.packageName, R.layout.notif_pq_small).apply {
            setTextViewText(R.id.pq_title, title)
            setTextViewText(R.id.pq_body, body)
        }
        return small to big
    }

    /** Settings > Look, as stored: a notification is often built when no screen is open. */
    fun skin(context: Context): PqSkin = SettingsRepositoryImpl.storedThemeMode(context).skin

    private fun palette(context: Context): PqPalette = if (skin(context) == PqSkin.Pixel) PixelSpec.palette else ComicSpec.palette
}
