package com.pixelquest.app.zz

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.components.AvatarClass
import com.pixelquest.app.ui.components.QuestCategory
import com.pixelquest.app.ui.components.QuestUi
import com.pixelquest.app.ui.navigation.PqBottomBar
import com.pixelquest.app.ui.navigation.PqTab
import com.pixelquest.app.ui.screens.HomeContent
import com.pixelquest.app.ui.screens.HomeUiState
import com.pixelquest.app.ui.theme.PqSkin
import org.junit.Test

class HomeShotTest : ScratchShot() {
    private val state = HomeUiState(
        heroName = "CBF", level = 1, avatar = AvatarClass.Hero, streakDays = 3, doneCount = 2, totalCount = 4,
        targetPercent = 70, xpToday = 40,
        upNext = listOf(
            QuestUi(1, "Go to Gym", "17:30", QuestCategory.Fitness, "Daily", overdue = true),
            QuestUi(2, "Read 15 Pages", "21:00", QuestCategory.Learning, "Daily"),
        ),
        doneToday = listOf(QuestUi(3, "20 Push-ups", "08:00", QuestCategory.Fitness, done = true, xp = 20)),
        isPerfectDay = false,
    )

    private fun home(skin: PqSkin, s: HomeUiState, name: String) = shot(name, skin) {
        Column(Modifier.fillMaxSize()) {
            HomeContent(s, "Good evening", {}, {}, {}, {}, {}, {}, {}, Modifier.weight(1f), partiesDot = true,
                prayerSection = {
                    val z = java.time.ZonedDateTime.of(2026, 10, 21, 18, 46, 0, 0, java.time.ZoneId.of("UTC"))
                    com.pixelquest.app.ui.screens.namaz.NamazSummaryCard(
                        com.pixelquest.app.ui.screens.namaz.NamazSummary(
                            com.pixelquest.app.domain.namaz.NextPrayer(
                                com.pixelquest.app.domain.namaz.PrayerMoment(com.pixelquest.app.domain.namaz.Prayer.MAGHRIB, z.toLocalDate(), z.minusHours(1)),
                                com.pixelquest.app.domain.namaz.PrayerMoment(com.pixelquest.app.domain.namaz.Prayer.ISHA, z.toLocalDate(), z),
                                java.time.Duration.ofMinutes(36), z.toLocalDate()
                            ), 4
                        ), {}
                    )
                },
                studySection = {
                    com.pixelquest.app.study.ui.HomeStudyCard(
                        com.pixelquest.app.study.ui.HomeStudy(listOf(com.pixelquest.app.study.ui.HomeDueLine("p", "Exam group", "i", "Chapter 4 notes", com.pixelquest.app.study.ui.DueState.TODAY, null))), {}, {}
                    )
                })
            PqBottomBar(PqTab.Home, {}, {}, deenDot = true)
        }
    }

    @Test fun comic() = home(PqSkin.Comic, state, "home")
    @Test fun pixel() = home(PqSkin.Pixel, state, "home")
    @Test fun perfect() = home(PqSkin.Comic, state.copy(upNext = emptyList(), doneCount = 4, isPerfectDay = true), "home-perfect")
    @Test fun empty() = home(PqSkin.Comic, state.copy(upNext = emptyList(), doneToday = emptyList(), doneCount = 0, totalCount = 0), "home-empty")
}
