package com.pixelquest.app.ui.navigation

/** Every destination's route, in one place (moved here from NavGraph.kt). */
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object Tasks : Screen("tasks")
    object Stats : Screen("stats")
    object Profile : Screen("profile")
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
    /** Darood Party, from the Islamic hub. */
    object DaroodParty : Screen("${Routes.ISLAMIC}/darood")
    /** Starting a Darood Party. */
    object DaroodCreate : Screen("${Routes.ISLAMIC}/darood/create")
    /** One Darood Party's own screen (its chat). */
    object DaroodPartyChat : Screen("${Routes.ISLAMIC}/darood/party/{partyId}") {
        fun createRoute(partyId: String) = "${Routes.ISLAMIC}/darood/party/$partyId"
    }
    /** The Darood Party race board. */
    object DaroodRaceBoard : Screen("${Routes.ISLAMIC}/darood/race")
    /** A Darood Party's info: its members. */
    object DaroodPartyInfo : Screen("${Routes.ISLAMIC}/darood/party/{partyId}/info") {
        fun createRoute(partyId: String) = "${Routes.ISLAMIC}/darood/party/$partyId/info"
    }
    /** Adding people to a Darood Party by username. */
    object DaroodAddPeople : Screen("${Routes.ISLAMIC}/darood/party/{partyId}/add") {
        fun createRoute(partyId: String) = "${Routes.ISLAMIC}/darood/party/$partyId/add"
    }
    /** Claiming or changing the Darood Party username. */
    object UsernameSetup : Screen("username_setup")
    /** The Islamic section's landing screen, its cards (docs/islamic-hub.md). */
    object IslamicHub : Screen(Routes.ISLAMIC_HUB)
    /** The Quran, from the Islamic hub. */
    object Quran : Screen("${Routes.ISLAMIC}/quran")
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

    /** Whether [route] is one of the Islamic section's screens. */
    fun isIslamic(route: String?): Boolean = route != null && route.startsWith("$ISLAMIC/")

    /**
     * The Namaz Tracker's route before the hub (1.4). Reminders and alarms set by 1.4 still carry it
     * after the update, so it stays, opening the tracker in its new place.
     */
    const val LEGACY_NAMAZ = "namaz"

    /** Darood Party's route before the hub (1.4), kept the same way. */
    const val LEGACY_DAROOD = "darood"

    /** The bottom bar's five tabs: where the side menu opens from, by its icon or a swipe from the left edge. */
    val Tabs: Set<String> = setOf(
        Screen.Home.route,
        Screen.Tasks.route,
        Screen.Leaderboard.route,
        Screen.Stats.route,
        Screen.Profile.route
    )

    fun isTab(route: String?): Boolean = route in Tabs
}
