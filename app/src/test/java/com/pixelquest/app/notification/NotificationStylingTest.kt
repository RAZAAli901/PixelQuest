package com.pixelquest.app.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationStylingTest {

    @Test
    fun testNotificationAccentColorMaintainsHighContrast() {
        // PixelQuest Daylight Gold / Retro Amber: 0xFFB45309
        val accentColor = NotificationHelper.NOTIFICATION_ACCENT_COLOR
        assertEquals(0xFFB45309.toInt(), accentColor)

        // Calculate relative luminance of 0xFFB45309
        val r = ((accentColor shr 16) and 0xFF) / 255.0
        val g = ((accentColor shr 8) and 0xFF) / 255.0
        val b = (accentColor and 0xFF) / 255.0

        fun toLinear(c: Double) = if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
        val lAmber = 0.2126 * toLinear(r) + 0.7152 * toLinear(g) + 0.0722 * toLinear(b)

        // Light notification shade background (pure white = 1.0)
        val contrastAgainstLightShade = (1.0 + 0.05) / (lAmber + 0.05)
        // Dark notification shade background (#121212 = luminance ~0.005)
        val contrastAgainstDarkShade = (lAmber + 0.05) / (0.005 + 0.05)

        assertTrue(
            "Expected contrast against light notification shade to be >= 4.5:1, got $contrastAgainstLightShade",
            contrastAgainstLightShade >= 4.5
        )
        assertTrue(
            "Expected contrast against dark notification shade to be >= 3.0:1, got $contrastAgainstDarkShade",
            contrastAgainstDarkShade >= 3.0
        )
    }
}
