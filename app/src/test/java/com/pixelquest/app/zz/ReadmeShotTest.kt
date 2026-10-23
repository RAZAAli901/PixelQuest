package com.pixelquest.app.zz

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.R
import com.pixelquest.app.darood.ui.list.DaroodPartiesUiState
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.local.RoomTransactionRunner
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.ai.HabitInsightResponse
import com.pixelquest.app.domain.model.DailyStatus
import com.pixelquest.app.domain.namaz.NextPrayer
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.domain.namaz.PrayerMoment
import com.pixelquest.app.islamic.IslamicIcon
import com.pixelquest.app.islamic.IslamicItem
import com.pixelquest.app.islamic.ui.DeenQuran
import com.pixelquest.app.islamic.ui.IslamicHubContent
import com.pixelquest.app.islamic.ui.IslamicHubUiState
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.Party
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.party.ui.PartiesContent
import com.pixelquest.app.quran.data.AndroidQuranAssetSource
import com.pixelquest.app.quran.data.QuranImporter
import com.pixelquest.app.quran.data.QuranRepositoryImpl
import com.pixelquest.app.quran.ui.QuranHomeScreen
import com.pixelquest.app.quran.ui.list.ContinueReading
import com.pixelquest.app.quran.ui.list.SurahListUiState
import com.pixelquest.app.ui.components.AvatarClass
import com.pixelquest.app.ui.components.PixelTextField
import com.pixelquest.app.ui.components.PqDangerButton
import com.pixelquest.app.ui.components.PqFieldLabel
import com.pixelquest.app.ui.components.PqSegmented
import com.pixelquest.app.ui.components.QuestCategory
import com.pixelquest.app.ui.components.QuestUi
import com.pixelquest.app.ui.navigation.PqBottomBar
import com.pixelquest.app.ui.navigation.PqTab
import com.pixelquest.app.ui.screens.HeroContent
import com.pixelquest.app.ui.screens.HeroSegment
import com.pixelquest.app.ui.screens.HomeContent
import com.pixelquest.app.ui.screens.HomeUiState
import com.pixelquest.app.ui.screens.QuestGroup
import com.pixelquest.app.ui.screens.QuestTab
import com.pixelquest.app.ui.screens.QuestsContent
import com.pixelquest.app.ui.screens.heroUiOf
import com.pixelquest.app.ui.screens.insight.AiInsightScreenContent
import com.pixelquest.app.ui.screens.insight.AiInsightUiState
import com.pixelquest.app.ui.screens.insight.CoachDisplayState
import com.pixelquest.app.ui.screens.insight.OlderInsight
import com.pixelquest.app.ui.screens.namaz.NamazSummary
import com.pixelquest.app.ui.screens.namaz.NamazSummaryCard
import com.pixelquest.app.ui.screens.profile.ProfileUiState
import com.pixelquest.app.ui.screens.settings.SettingLink
import com.pixelquest.app.ui.screens.settings.SettingNote
import com.pixelquest.app.ui.screens.settings.SettingSwitch
import com.pixelquest.app.ui.screens.settings.SettingsScreenScaffold
import com.pixelquest.app.ui.screens.stats.StatsUiState
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.PqSkin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.Instant

/** Not committed: the README's screenshots of UI v2, in the app's frame. */
class ReadmeShotTest : ScratchShot() {
    private val evening = ZonedDateTime.of(2026, 10, 21, 18, 46, 0, 0, ZoneId.of("UTC"))
    private val prayer = NamazSummary(
        NextPrayer(PrayerMoment(Prayer.MAGHRIB, evening.toLocalDate(), evening.minusHours(1)), PrayerMoment(Prayer.ISHA, evening.toLocalDate(), evening), Duration.ofMinutes(36), evening.toLocalDate()),
        prayed = 4, prayedPrayers = setOf(Prayer.FAJR, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB), allFiveStreak = 2
    )

    /** A tab's screen over the bottom bar, as the app shows it. */
    private fun tab(name: String, skin: PqSkin, tab: PqTab, screen: @Composable () -> Unit) = shot(name, skin) {
        PixelQuestTheme {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) { screen() }
                PqBottomBar(tab, {}, {}, deenDot = true)
            }
        }
    }

    // ---- Home ----
    private val home = HomeUiState(
        heroName = "Hero", level = 4, avatar = AvatarClass.Hero, streakDays = 6, doneCount = 2, totalCount = 4,
        targetPercent = 70, xpToday = 40,
        upNext = listOf(
            QuestUi(1, "Go to the gym", "17:30", QuestCategory.Fitness, "Daily", overdue = true),
            QuestUi(2, "Read 15 pages", "21:00", QuestCategory.Learning, "Daily"),
        ),
        doneToday = listOf(QuestUi(3, "20 push-ups", "08:00", QuestCategory.Fitness, done = true, xp = 20)),
        isPerfectDay = false,
    )

    private fun homeShot(skin: PqSkin) = shot("v2-home", skin) {
        PixelQuestTheme {
            Column(Modifier.fillMaxSize()) {
                HomeContent(home, "Good evening", {}, {}, {}, {}, {}, {}, {}, Modifier.weight(1f), partiesDot = true,
                    prayerSection = { NamazSummaryCard(prayer, {}) },
                    studySection = {
                        com.pixelquest.app.study.ui.HomeStudyCard(
                            com.pixelquest.app.study.ui.HomeStudy(listOf(com.pixelquest.app.study.ui.HomeDueLine("p", "Exam group", "i", "Chapter 4 notes", com.pixelquest.app.study.ui.DueState.TODAY, null))), {}, {}
                        )
                    })
                PqBottomBar(PqTab.Home, {}, {}, deenDot = true)
            }
        }
    }

    @Test fun homeComic() = homeShot(PqSkin.Comic)
    @Test fun homePixel() = homeShot(PqSkin.Pixel)

    // ---- Quests ----
    private val groups = listOf(
        QuestGroup("m", "Morning · 2 done", listOf(
            QuestUi(1, "20 push-ups", "08:00", QuestCategory.Fitness, "Daily", done = true),
            QuestUi(2, "Drink water", "11:00", QuestCategory.Health, done = true))),
        QuestGroup("e", "Evening · 2 left", listOf(
            QuestUi(3, "Go to the gym", "17:30", QuestCategory.Fitness, "Daily", overdue = true),
            QuestUi(4, "Read 15 pages", "21:00", QuestCategory.Learning, "Daily"))),
    )

    @Test fun questsComic() = tab("v2-quests", PqSkin.Comic, PqTab.Quests) { QuestsContent(QuestTab.Today, {}, 4, groups, {}, {}, {}, canSkip = { !it.done }) }
    @Test fun questsPixel() = tab("v2-quests", PqSkin.Pixel, PqTab.Quests) { QuestsContent(QuestTab.Today, {}, 4, groups, {}, {}, {}, canSkip = { !it.done }) }

    // ---- Deen ----
    private val items = listOf(
        IslamicItem("namaz", R.string.namaz_title, R.string.islamic_namaz_description, IslamicIcon("🕌"), "islamic/namaz", badgeCount = 1, badgeLabel = R.plurals.islamic_namaz_badge),
        IslamicItem("darood", R.string.darood_title, R.string.islamic_darood_description, IslamicIcon("📿"), "islamic/darood", badgeCount = 1, badgeLabel = R.plurals.islamic_darood_badge),
        IslamicItem("quran", R.string.quran_title, R.string.quran_description, IslamicIcon("📖"), "islamic/quran")
    )

    private fun deen(skin: PqSkin) = tab("v2-deen", skin, PqTab.Deen) {
        IslamicHubContent(
            state = IslamicHubUiState(items = items, isSimpleMode = false, loaded = true),
            onOpen = {}, showComingSoon = true, prayer = prayer,
            quran = DeenQuran("Al-Baqara", 142, 286), today = LocalDate.of(2026, 10, 21),
        )
    }

    @Test fun deenComic() = deen(PqSkin.Comic)
    @Test fun deenPixel() = deen(PqSkin.Pixel)

    // ---- Hero ----
    private val today = LocalDate.of(2026, 10, 21)
    private val hero = heroUiOf(
        ProfileUiState(profile = UserProfileEntity(username = "Hero", avatarId = "avatar_hero", level = 4, totalXp = 940, perfectDaysTowardNextLevel = 2),
            streak = StreakEntity(currentStreak = 6, longestStreak = 9), totalTasksCompleted = 58, xpLastWeek = 320, daroodUsername = "hero_1"),
        StatsUiState(heatmapStatusMap = (0..60).associate { today.minusDays(it.toLong()) to listOf(DailyStatus.PERFECT, DailyStatus.PARTIAL, DailyStatus.PERFECT, DailyStatus.MISSED)[it % 4] },
            weeklyTrend = listOf("W-3" to 0.5f, "W-2" to 0.6f, "W-1" to 0.8f, "NOW" to 0.7f), longestStreak = 9, overallCompletionRate = 0.72f),
        today
    )

    @Test fun heroComic() = tab("v2-hero", PqSkin.Comic, PqTab.Hero) { HeroContent(hero, HeroSegment.Profile, {}, {}, {}, {}, {}, {}, {}) }
    @Test fun heroPixel() = tab("v2-hero", PqSkin.Pixel, PqTab.Hero) { HeroContent(hero, HeroSegment.Stats, {}, {}, {}, {}, {}, {}, {}) }

    // ---- The Quran: the list from the app's own bundled files ----
    @Test fun quran() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val surahs = runBlocking {
            QuranImporter(AndroidQuranAssetSource(context), db.quranDao(), RoomTransactionRunner(db)) { QuranImporter.TRANSLATION_FILE }.import()
            QuranRepositoryImpl(db.quranDao()).surahs().first()
        }
        val state = SurahListUiState(surahs = surahs, loaded = true, continueReading = ContinueReading(surahs[1], 142))
        shot("v2-quran", PqSkin.Comic) { PixelQuestTheme { QuranHomeScreen(onNavigateBack = {}, list = state) } }
    }

    // ---- Parties ----
    private fun party(id: String, name: String, members: Int, total: Long, milestone: Long?, type: PartyType = PartyType.DAROOD, status: MemberStatus = MemberStatus.ACTIVE) = Party(
        id, name, "me", 10, milestone, null, null, Instant.EPOCH, total = total, memberCount = members, myStatus = status, type = type)

    @Test fun parties() = shot("v2-parties", PqSkin.Comic) {
        PixelQuestTheme {
            PartiesContent(
                DaroodPartiesUiState(signedIn = true,
                    parties = listOf(party("1", "Friday circle", 5, 7_260, 10_000), party("2", "Physics group", 3, 0, null, PartyType.STUDY)),
                    invites = listOf(party("3", "Exam group", 4, 0, null, PartyType.STUDY, MemberStatus.INVITED))),
                {}, {}, {}, onNavigateBack = {}, username = "hero_1"
            )
        }
    }

    // ---- The AI Coach ----
    @Test fun coach() = shot("v2-coach", PqSkin.Comic) {
        PixelQuestTheme {
            AiInsightScreenContent(
                uiState = AiInsightUiState.Success(
                    insight = HabitInsightResponse(
                        "You finished 18 of 21 quests this week. Mornings are your strongest time: every fitness quest before 9am got done, while two of three evening reading quests slipped.",
                        "Move one reading quest to the morning, right after your workout.",
                        "Three perfect days in a row is your best run yet.",
                        generatedAt = 1_760_000_000_000L
                    ),
                    isCached = false,
                    remainingCooldownSeconds = 3 * 3600L
                ),
                displayState = CoachDisplayState(), onRefresh = {},
                olderInsights = listOf(OlderInsight(1, HabitInsightResponse("Mornings went well, evenings slipped.", "Try reading at lunch.", "Keep going.", generatedAt = 1_759_900_000_000L))),
                onNavigateBack = {}
            )
        }
    }

    // ---- Settings, with the Look switch ----
    @Test fun settings() = shot("v2-settings", PqSkin.Comic) {
        PixelQuestTheme {
            SettingsScreenScaffold(
                onNavigateBack = {},
                accountSection = {
                    PixelTextField("Hero", {}, label = "Hero name", modifier = Modifier.fillMaxWidth())
                    SettingLink(Icons.Rounded.Tune, "Difficulty", {}, value = "Medium")
                    SettingLink(Icons.Rounded.Cloud, "Cloud & leaderboard", {})
                },
                notificationsSection = {
                    SettingSwitch("Notifications", "Reminders and nudges from PixelQuest", true, {})
                    SettingSwitch("Reminder sound", "Off, reminders arrive silently", true, {})
                },
                appearanceSection = {
                    PqFieldLabel(androidx.compose.ui.res.stringResource(R.string.pq_settings_look))
                    PqSegmented(listOf(androidx.compose.ui.res.stringResource(R.string.pq_look_comic), androidx.compose.ui.res.stringResource(R.string.pq_look_pixel)), 0, {})
                    SettingSwitch("Sound effects", null, true, {})
                    SettingSwitch("Haptics", null, true, {})
                    SettingSwitch("Reduce motion", "Pulses, spinners and bounces hold still", false, {})
                },
                dangerZoneSection = {
                    SettingNote("Wipes every quest, your XP, level and streaks on this phone. It asks twice first.")
                    PqDangerButton("Reset all progress", {})
                }
            )
        }
    }
}
