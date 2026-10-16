package com.pixelquest.app.ui.screens.insight

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The AI Coach screen's spacing, padding and text sizes in one place, so its layout
 * (docs/ai-coach-compact.md) is tuned here instead of across the three theme views.
 */
object CoachDimens {
    /** Around the whole screen's content. */
    val ScreenPadding = 16.dp

    /** The screen's header banner. */
    object Header {
        val PixelPadding = 12.dp
        val ComicPadding = 14.dp
        val LightPadding = 16.dp
        val PixelAvatar = 44.dp
        val ComicAvatar = 52.dp
        val LightAvatar = 48.dp
        val PixelAvatarEmoji = 24.sp
        val ComicAvatarEmoji = 28.sp
        val LightAvatarEmoji = 24.sp
        val PixelAvatarGap = 12.dp
        val AvatarGap = 14.dp
        val StatusText = 9.sp
        val ComicTitle = 22.sp
        val ComicSubtitle = 11.sp
        val ComicChip = 13.sp
        /** Between the header and the state below it. */
        val PixelBottomGap = 14.dp
        val BottomGap = 16.dp
    }

    /** The insight card. */
    object Card {
        val PixelPadding = 16.dp
        val LightPadding = 20.dp
        val ComicBubblePadding = 16.dp
        val TitleGap = 14.dp
        val LightTitleGap = 16.dp
        val SectionGap = 12.dp
        val ComicSectionGap = 14.dp
        val LightSectionGap = 14.dp
        val FooterGap = 16.dp
        val LightFooterGap = 20.dp
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
        val PixelButtonHeight = 36.dp
        val LightButtonHeight = 38.dp
        val CooldownGap = 6.dp
        val PixelCooldownText = 8.sp
        val ComicCooldownText = 11.sp
    }
}
