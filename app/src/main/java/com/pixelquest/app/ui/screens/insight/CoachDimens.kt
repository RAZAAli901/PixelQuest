package com.pixelquest.app.ui.screens.insight

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The AI Coach screen's spacing, padding and text sizes in one place, so its layout
 * (docs/ai-coach-compact.md) is tuned here instead of across the three theme views.
 */
object CoachDimens {
    /** Around the whole screen's content (16dp before the compact layout). */
    val ScreenPadding = 8.dp

    /**
     * Between the screen's items: the header and the state below it, and the card's title, sections
     * and footer (12-20dp before, varying by theme).
     */
    val ItemGap = 8.dp

    /** The smallest touch target for anything tappable, however small it looks (Android's 48dp). */
    val MinTouchTarget = 48.dp

    /** The screen's header banner. */
    object Header {
        /** A slim row now: 12-16dp before, around a 44-52dp avatar. */
        val PixelPadding = 6.dp
        val ComicPadding = 6.dp
        val LightPadding = 8.dp
        /** The coach's avatar beside the title, the same in every theme. */
        val Avatar = 40.dp
        val PixelAvatarEmoji = 20.sp
        val ComicAvatarEmoji = 22.sp
        val LightAvatarEmoji = 20.sp
        val PixelAvatarGap = 8.dp
        val AvatarGap = 8.dp
        val StatusText = 9.sp
        /** One line now (it was 22sp over a subtitle). */
        val ComicTitle = 18.sp
        val ComicChip = 13.sp
        /** Between the title and its inline status chip. */
        val ChipGap = 6.dp
    }

    /** The insight card. */
    object Card {
        /** How much of the summary a collapsed insight shows. */
        const val PreviewLines = 3
        val PixelPadding = 16.dp
        val LightPadding = 20.dp
        val ComicBubblePadding = 16.dp
        val PixelBlockPadding = 10.dp
        val LightBlockPadding = 12.dp
        val BodyLineHeight = 20.sp
        val ComicBodyLineHeight = 21.sp
        val LightBodyLineHeight = 21.sp
        val ComicBody = 14.sp
        val ComicBubbleTitle = 13.sp
        val Badge = 9.sp
        val ComicFooterTitle = 14.sp
        val ComicFooterTime = 11.sp
    }
}
