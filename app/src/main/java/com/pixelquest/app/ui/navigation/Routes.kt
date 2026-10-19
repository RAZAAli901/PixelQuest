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
    /** The Namaz Tracker, from the side menu. */
    object Namaz : Screen("namaz")
    /** Darood Party, from the side menu. */
    object DaroodParty : Screen("darood")
    /** Starting a Darood Party. */
    object DaroodCreate : Screen("darood/create")
    /** One Darood Party's own screen (its chat). */
    object DaroodPartyChat : Screen("darood/party/{partyId}") {
        fun createRoute(partyId: String) = "darood/party/$partyId"
    }
    /** A Darood Party's info: its members. */
    object DaroodPartyInfo : Screen("darood/party/{partyId}/info") {
        fun createRoute(partyId: String) = "darood/party/$partyId/info"
    }
    /** Adding people to a Darood Party by username. */
    object DaroodAddPeople : Screen("darood/party/{partyId}/add") {
        fun createRoute(partyId: String) = "darood/party/$partyId/add"
    }
    /** Claiming or changing the Darood Party username. */
    object UsernameSetup : Screen("username_setup")
}

/** Groups of routes the app's frame cares about. */
object Routes {
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
