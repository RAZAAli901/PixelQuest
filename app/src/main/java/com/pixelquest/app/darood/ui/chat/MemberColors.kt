package com.pixelquest.app.darood.ui.chat

import androidx.compose.ui.graphics.Color
import com.pixelquest.app.ui.theme.PqColors

/**
 * Each member's own colour in a party's chat, for their name over their bubbles, so members tell
 * apart at a glance, as in a group chat. It comes from the user id alone, so it's the same on every
 * phone, in every party, every time; and every colour reads on the ground and paper (4.5:1 at least).
 */
object MemberColors {
    /** Deep inks that read on both skins' light ground and paper. */
    val palette = listOf(
        Color(0xFF1565C0), Color(0xFF1B5E20), Color(0xFFC62828), Color(0xFF6A1B9A),
        Color(0xFFAD1457), Color(0xFF00695C), Color(0xFFBF360C), Color(0xFF283593)
    )

    /**
     * Where [userId] falls in a palette of [size]. String.hashCode is fixed by the language, so
     * this is the same everywhere.
     */
    fun indexOf(userId: String, size: Int): Int = Math.floorMod(userId.hashCode(), size)
}

/** [userId]'s colour; the primary text colour when who posted isn't known. */
fun memberColor(userId: String?): Color {
    if (userId == null) return PqColors.RedText
    return MemberColors.palette[MemberColors.indexOf(userId, MemberColors.palette.size)]
}
