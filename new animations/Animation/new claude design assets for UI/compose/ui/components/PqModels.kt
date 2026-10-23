package com.pixelquest.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.pixelquest.ui.theme.PqColors

/** UI-only models. Map your existing entities/ViewModel state into these. Colours are getters so they follow the active skin. */

enum class QuestCategory(val label: String, val icon: ImageVector, private val tint: () -> Color) {
    Fitness("Fitness", Icons.Rounded.FitnessCenter, { PqColors.Amber }),
    Learning("Learning", Icons.Rounded.MenuBook, { PqColors.Lilac }),
    Health("Health", Icons.Rounded.Favorite, { PqColors.Rose }),
    Mind("Mindfulness", Icons.Rounded.SelfImprovement, { PqColors.Sky }),
    Other("Other", Icons.Rounded.Star, { PqColors.Gold });

    val color: Color get() = tint()

    companion object {
        fun from(name: String?): QuestCategory =
            entries.firstOrNull { it.name.equals(name, true) || it.label.equals(name, true) } ?: Other
    }
}

data class QuestUi(
    val id: String,
    val title: String,
    val time: String,            // "21:00"
    val category: QuestCategory,
    val repeatLabel: String? = "Daily",
    val done: Boolean = false,
    val overdue: Boolean = false,
    val xp: Int = 20,
)

data class QuestGroupUi(val title: String, val quests: List<QuestUi>)

enum class PrayerStatus { Prayed, Next, Upcoming, Missed }

data class PrayerUi(val name: String, val time: String, val status: PrayerStatus, val countdown: String? = null)

fun prayerIcon(name: String): ImageVector = when (name.lowercase()) {
    "fajr" -> Icons.Rounded.WbTwilight
    "dhuhr", "zuhr" -> Icons.Rounded.LightMode
    "asr" -> Icons.Rounded.WbCloudy
    "maghrib" -> Icons.Rounded.NightsStay
    else -> Icons.Rounded.Bedtime
}

fun prayerColor(name: String): Color = when (name.lowercase()) {
    "fajr" -> PqColors.Sky
    "dhuhr", "zuhr" -> PqColors.Gold
    "asr" -> PqColors.Amber
    "maghrib" -> PqColors.Rose
    else -> PqColors.Lilac
}

data class NextPrayerUi(val name: String, val time: String, val countdown: String)

enum class DayStatus(private val tint: () -> Color) {
    Perfect({ PqColors.StatusPerfect }),
    Partial({ PqColors.StatusPartial }),
    Missed({ PqColors.StatusMissed }),
    None({ PqColors.Sunken });

    val color: Color get() = tint()
}

enum class AvatarClass(val label: String, val hair: Color, val shirt: Color) {
    Hero("Hero", Color(0xFFFFC21A), Color(0xFF4A90D9)),
    Mage("Mage", Color(0xFF8E5CC4), Color(0xFF8E5CC4)),
    Rogue("Rogue", Color(0xFF3FA55B), Color(0xFF3FA55B)),
    Warrior("Warrior", Color(0xFF8A8A8A), Color(0xFFC0392B)),
    Paladin("Paladin", Color(0xFFFFC21A), Color(0xFFF2F2F2)),
    Ranger("Ranger", Color(0xFFE0701F), Color(0xFFE0701F)),
}
