package com.pixelquest.app.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.pixelquest.app.R
import com.pixelquest.app.ui.theme.PqColors

// UI v2's own models (docs/ui-v2.md): the screens take these, mapped from the view models' state.
// Colours are getters, so they follow the active skin.

/** A quest's category as UI v2 draws it: its name, icon and colour. The app's own five categories. */
enum class QuestCategory(@StringRes val label: Int, val icon: ImageVector, private val tint: () -> Color) {
    Fitness(R.string.pq_category_fitness, Icons.Rounded.FitnessCenter, { PqColors.Amber }),
    Learning(R.string.pq_category_learning, Icons.AutoMirrored.Rounded.MenuBook, { PqColors.Lilac }),
    Health(R.string.pq_category_health, Icons.Rounded.Favorite, { PqColors.Rose }),
    Chores(R.string.pq_category_chores, Icons.Rounded.CleaningServices, { PqColors.Sky }),
    Other(R.string.pq_category_other, Icons.Rounded.Star, { PqColors.Gold }),
    ;

    val color: Color get() = tint()

    companion object {
        /** The category stored on a quest (its name, any case); anything else is Other. */
        fun from(name: String?): QuestCategory = entries.firstOrNull { it.name.equals(name, true) } ?: Other
    }
}

/**
 * A quest as a row shows it.
 *
 * @param time its scheduled time, already formatted ("21:00"), or null.
 * @param repeatLabel how it repeats ("Daily"), or null for a one-time quest.
 * @param overdue past its time today and not done yet.
 * @param missed a day gone by without it (Quests > History).
 */
@Immutable
data class QuestUi(
    val id: Long,
    val title: String,
    val time: String?,
    val category: QuestCategory,
    val repeatLabel: String? = null,
    val done: Boolean = false,
    val overdue: Boolean = false,
    val xp: Int = 0,
    val missed: Boolean = false,
)

@Immutable
data class QuestGroupUi(val title: String, val quests: List<QuestUi>)

enum class PrayerStatus { Prayed, Next, Upcoming, Missed }

/** A prayer as UI v2 lists it. [name] is already in the player's language. */
@Immutable
data class PrayerUi(val name: String, val time: String, val status: PrayerStatus, val countdown: String? = null, val key: String = name)

/** The icon for a prayer, by its key (fajr, dhuhr, asr, maghrib, isha). */
fun prayerIcon(key: String): ImageVector = when (key.lowercase()) {
    "fajr" -> Icons.Rounded.WbTwilight
    "dhuhr", "zuhr" -> Icons.Rounded.LightMode
    "asr" -> Icons.Rounded.WbCloudy
    "maghrib" -> Icons.Rounded.NightsStay
    else -> Icons.Rounded.Bedtime
}

/** The colour for a prayer, by its key. */
fun prayerColor(key: String): Color = when (key.lowercase()) {
    "fajr" -> PqColors.Sky
    "dhuhr", "zuhr" -> PqColors.Gold
    "asr" -> PqColors.Amber
    "maghrib" -> PqColors.Rose
    else -> PqColors.Lilac
}

@Immutable
data class NextPrayerUi(val name: String, val time: String, val countdown: String)

enum class DayStatus(private val tint: () -> Color) {
    Perfect({ PqColors.StatusPerfect }),
    Partial({ PqColors.StatusPartial }),
    Missed({ PqColors.StatusMissed }),
    None({ PqColors.Sunken }),
    ;

    val color: Color get() = tint()
}

/** The six hero classes, by the avatar id the profile stores, with their sprites. */
enum class AvatarClass(val id: String, @StringRes val label: Int, @DrawableRes val sprite: Int) {
    Hero("avatar_hero", R.string.pq_avatar_hero, R.drawable.avatar_hero),
    Mage("avatar_mage", R.string.pq_avatar_mage, R.drawable.avatar_mage),
    Rogue("avatar_rogue", R.string.pq_avatar_rogue, R.drawable.avatar_rogue),
    Warrior("avatar_warrior", R.string.pq_avatar_warrior, R.drawable.avatar_warrior),
    Paladin("avatar_paladin", R.string.pq_avatar_paladin, R.drawable.avatar_paladin),
    Ranger("avatar_ranger", R.string.pq_avatar_ranger, R.drawable.avatar_ranger),
    ;

    companion object {
        /** The class for a stored avatar id; the Hero for anything else. */
        fun from(id: String?): AvatarClass = entries.firstOrNull { it.id == id } ?: Hero
    }
}
