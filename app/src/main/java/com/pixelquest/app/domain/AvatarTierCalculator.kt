package com.pixelquest.app.domain

import androidx.compose.ui.graphics.Color
import com.pixelquest.app.ui.theme.PixelGold

enum class AvatarTier(val displayName: String, val borderColor: Long, val badgeEmoji: String) {
    BRONZE("BRONZE TIER", 0xFFCD7F32, "🥉"),
    SILVER("SILVER TIER", 0xFFC0C0C0, "🥈"),
    GOLD("GOLD TIER", 0xFFFFD700, "🥇")
}

object AvatarTierCalculator {
    fun calculateTier(level: Int): AvatarTier {
        return when {
            level < 5 -> AvatarTier.BRONZE
            level < 10 -> AvatarTier.SILVER
            else -> AvatarTier.GOLD
        }
    }
}
