package com.pixelquest.app.ui.navigation

/** Every destination's route, in one place (moved here from NavGraph.kt). */
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object Tasks : Screen("tasks")
    /** The Hero tab: the player's profile and stats (docs/ui-v2.md). */
    object Hero : Screen("hero")
    object AvatarSelection : Screen("avatar_selection")
    object DifficultySelection : Screen("difficulty_selection")
    object LevelHistory : Screen("level_history")
    object TaskHistory : Screen("task_history")
    object TaskAnalytics : Screen("task_analytics/{taskId}") {
        fun createRoute(taskId: Long) = "task_analytics/$taskId"
    }
    object CreateTask : Screen("create_task")
    object EditTask : Screen("edit_task/{taskId}") {
        fun createRoute(taskId: Long) = "edit_task/$taskId"
    }
    object Onboarding : Screen("onboarding")
    object Settings : Screen("settings")
    object Account : Screen("account")
    object Leaderboard : Screen("leaderboard")
    object ThemeSelection : Screen("theme_selection")
    object AiInsight : Screen("ai_insight")
    /** The Namaz Tracker, from the Islamic hub. */
    object Namaz : Screen("${Routes.ISLAMIC}/namaz")
    /** The main app's parties (docs/party-core.md): the study parties, outside the Islamic section. */
    object Parties : Screen(Routes.PARTIES)
    /** Starting a study party. */
    object PartiesCreate : Screen("${Routes.PARTIES}/create")
    /** A study party's own screen: its tabs (docs/study-party.md). */
    object PartiesParty : Screen("${Routes.PARTIES}/party/{partyId}") {
        fun createRoute(partyId: String) = "${Routes.PARTIES}/party/$partyId"
    }
    /** One item in a study party: its details and who has finished it. */
    object PartiesItem : Screen("${Routes.PARTIES}/party/{partyId}/item/{itemId}") {
        fun createRoute(partyId: String, itemId: String) = "${Routes.PARTIES}/party/$partyId/item/$itemId"
    }
    /** A study party's info: its members. */
    object PartiesInfo : Screen("${Routes.PARTIES}/party/{partyId}/info") {
        fun createRoute(partyId: String) = "${Routes.PARTIES}/party/$partyId/info"
    }
    /** Adding people to a study party by username. */
    object PartiesAddPeople : Screen("${Routes.PARTIES}/party/{partyId}/add") {
        fun createRoute(partyId: String) = "${Routes.PARTIES}/party/$partyId/add"
    }
    /** A Darood Party's room: its count, kept as a chat (mock 4E). Its own route, so the bar can hide. */
    object DaroodRoom : Screen("${Routes.PARTIES}/darood/{partyId}") {
        fun createRoute(partyId: String) = "${Routes.PARTIES}/darood/$partyId"
    }
    /** The Darood Party race board. */
    object PartiesRaceBoard : Screen("${Routes.PARTIES}/race")
    /** Claiming or changing the Darood Party username. */
    object UsernameSetup : Screen("username_setup")
    /** The Islamic section's landing screen, its cards (docs/islamic-hub.md). */
    object IslamicHub : Screen(Routes.ISLAMIC_HUB)
    /** The Quran, from the Islamic hub. */
    object Quran : Screen("${Routes.ISLAMIC}/quran")
    /** Where the Quran in the app comes from, with its terms. */
    object QuranSources : Screen("${Routes.ISLAMIC}/quran/sources")
    /** Choosing the Quran's translation. */
    object QuranTranslations : Screen("${Routes.ISLAMIC}/quran/translations")
    /** Searching the whole Quran. */
    object QuranSearch : Screen("${Routes.ISLAMIC}/quran/search")
    /** One surah in the Quran reader, opened at an ayah (0: its start). */
    object QuranSurah : Screen("${Routes.ISLAMIC}/quran/surah/{surah}?ayah={ayah}") {
        fun createRoute(surah: Int, ayah: Int = 0) = "${Routes.ISLAMIC}/quran/surah/$surah?ayah=$ayah"
    }
}

/** Groups of routes the app's frame cares about. */
object Routes {
    /**
     * The Islamic section's nested graph (docs/islamic-hub.md): the hub, and every Islamic screen
     * under `islamic/`.
     */
    const val ISLAMIC = "islamic"

    /** The hub, where the Islamic section starts. */
    const val ISLAMIC_HUB = "$ISLAMIC/hub"

    /** The parties of every kind (docs/party-core.md): the hub, and every party screen under `parties/`. */
    const val PARTIES = "parties"

    /** Whether [route] is one of the main app's party screens. */
    fun isParties(route: String?): Boolean = route != null && (route == PARTIES || route.startsWith("$PARTIES/"))

    /** Whether [route] is one of the Islamic section's screens. */
    fun isIslamic(route: String?): Boolean = route != null && route.startsWith("$ISLAMIC/")

    /**
     * The Namaz Tracker's route before the hub (1.4). Reminders and alarms set by 1.4 still carry it
     * after the update, so it stays, opening the tracker in its new place.
     */
    const val LEGACY_NAMAZ = "namaz"

    /** Darood Party's route before the hub (1.4), kept the same way: it opens the parties. */
    const val LEGACY_DAROOD = "darood"

    /**
     * Routes whose screens have no bottom bar: the splash, onboarding, the Quran reader, and a Darood
     * Party's chat (mock 4E), which needs the room for its composer.
     */
    fun isFullScreen(route: String?): Boolean = route in FULL_SCREEN

    private val FULL_SCREEN = setOf(Screen.Splash.route, Screen.Onboarding.route, Screen.QuranSurah.route, Screen.DaroodRoom.route)
}
