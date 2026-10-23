package com.pixelquest.app.zz

import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.model.DailyStatus
import com.pixelquest.app.ui.screens.HeroContent
import com.pixelquest.app.ui.screens.HeroSegment
import com.pixelquest.app.ui.screens.heroUiOf
import com.pixelquest.app.ui.screens.profile.ProfileUiState
import com.pixelquest.app.ui.screens.stats.StatsUiState
import com.pixelquest.app.ui.theme.PqSkin
import org.junit.Test
import java.time.LocalDate

class HeroShotTest : ScratchShot() {
    private val today = LocalDate.of(2026, 10, 21)
    private val state = heroUiOf(
        ProfileUiState(profile = UserProfileEntity(username = "CBF", avatarId = "avatar_hero", level = 1, totalXp = 340, perfectDaysTowardNextLevel = 2),
            streak = StreakEntity(currentStreak = 3, longestStreak = 5), totalTasksCompleted = 18, xpLastWeek = 120, daroodUsername = "cbf_1"),
        StatsUiState(heatmapStatusMap = (0..60).associate { today.minusDays(it.toLong()) to listOf(DailyStatus.PERFECT, DailyStatus.PARTIAL, DailyStatus.MISSED, DailyStatus.NO_TASKS_SCHEDULED)[it % 4] },
            weeklyTrend = listOf("W-3" to 0f, "W-2" to 0.4f, "W-1" to 0.7f, "NOW" to 0.14f), longestStreak = 5, overallCompletionRate = 0.6f),
        today
    )
    private fun hero(skin: PqSkin, seg: HeroSegment, name: String) = shot(name, skin) {
        HeroContent(state, seg, {}, {}, {}, {}, {}, {}, {})
    }
    @Test fun profile() = hero(PqSkin.Comic, HeroSegment.Profile, "hero-profile")
    @Test fun stats() = hero(PqSkin.Comic, HeroSegment.Stats, "hero-stats")
    @Test fun pixel() = hero(PqSkin.Pixel, HeroSegment.Stats, "hero-stats")
}
