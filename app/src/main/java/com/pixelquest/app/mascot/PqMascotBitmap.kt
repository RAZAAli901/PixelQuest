package com.pixelquest.app.mascot

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.LruCache

/**
 * Static Pix bitmaps for notification large icons and the custom card.
 * Android can't animate notification images, so notifications get the first frame of each mood.
 */
object PqMascotBitmap {
    private val cache = LruCache<String, Bitmap>(24)

    /** Large icon: Pix on a soft tile (matches the doc's 44dp icon squares). */
    fun largeIcon(mood: PixMood, sizePx: Int, pixel: Boolean): Bitmap =
        render(mood, sizePx, pixel, tile = tileColor(mood, pixel))

    /** Transparent Pix, e.g. for the custom card ImageView. */
    fun render(mood: PixMood, sizePx: Int, pixel: Boolean, tile: Int? = null): Bitmap {
        val key = "${mood.key}-$sizePx-$pixel-$tile"
        cache.get(key)?.let { return it }
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG.inv())
        if (tile != null) {
            paint.color = tile
            val r = if (pixel) 0f else sizePx * 0.22f
            c.drawRoundRect(RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat()), r, r, paint)
        }
        val inset = if (tile != null) sizePx * 0.06f else 0f
        val cell = (sizePx - inset * 2) / 24f
        PixFrames.frame(mood, pixel).layers.forEach { layer ->
            layer.cells.forEach { cell0 ->
                paint.color = cell0.color
                val x = inset + cell0.x * cell
                val y = inset + cell0.y * cell
                c.drawRect(x, y, x + cell + 0.5f, y + cell + 0.5f, paint)
            }
        }
        cache.put(key, bmp)
        return bmp
    }

    private fun tileColor(mood: PixMood, pixel: Boolean): Int = when (mood) {
        PixMood.Celebrate, PixMood.LevelUp, PixMood.OnFire, PixMood.Wow -> if (pixel) 0xFFFDE7A0.toInt() else 0xFFFFC21A.toInt()
        PixMood.Worried, PixMood.Sad, PixMood.Lost -> 0xFFEFE9DD.toInt()
        PixMood.Sleepy, PixMood.Praying -> if (pixel) 0xFFE4DAF6.toInt() else 0xFFB39DDB.toInt()
        PixMood.Challenge -> if (pixel) 0xFFFBD5AE.toInt() else 0xFFF0A35E.toInt()
        else -> if (pixel) 0xFFDBEEFA.toInt() else 0xFF8ECAE6.toInt()
    }
}
