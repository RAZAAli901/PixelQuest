package com.pixelquest.app.ui.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.navArgument

/**
 * Every destination's route and typed argument contracts in one centralized location.
 */
sealed class Screen(val route: String) {
    open val arguments: List<NamedNavArgument> = emptyList()

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
        const val ARG_TASK_ID = "taskId"
        override val arguments: List<NamedNavArgument> = listOf(
            navArgument(ARG_TASK_ID) { type = NavType.LongType },
        )
        fun createRoute(taskId: Long) = "task_analytics/$taskId"
        fun getTaskId(entry: NavBackStackEntry): Long = entry.arguments?.getLong(ARG_TASK_ID) ?: 0L
        fun getTaskId(handle: SavedStateHandle): Long = handle.get<Long>(ARG_TASK_ID) ?: 0L
    }

    object CreateTask : Screen("create_task")

    object EditTask : Screen("edit_task/{taskId}") {
        const val ARG_TASK_ID = "taskId"
        override val arguments: List<NamedNavArgument> = listOf(
            navArgument(ARG_TASK_ID) { type = NavType.LongType },
        )
        fun createRoute(taskId: Long) = "edit_task/$taskId"
        fun getTaskId(entry: NavBackStackEntry): Long = entry.arguments?.getLong(ARG_TASK_ID) ?: 0L
        fun getTaskId(handle: SavedStateHandle): Long = handle.get<Long>(ARG_TASK_ID) ?: 0L
    }

    object Onboarding : Screen("onboarding")
    object Settings : Screen("settings")
    object Account : Screen("account")
    object Leaderboard : Screen("leaderboard")
    object AiInsight : Screen("ai_insight")

    /** The Namaz Tracker, from the Islamic hub. */
    object Namaz : Screen("${Routes.ISLAMIC}/namaz")

    /** The main app's parties (docs/party-core.md): the study parties, outside the Islamic section. */
    object Parties : Screen(Routes.PARTIES)

    /** Starting a study party. */
    object PartiesCreate : Screen("${Routes.PARTIES}/create")

    /** A study party's own screen: its tabs (docs/study-party.md). */
    object PartiesParty : Screen("${Routes.PARTIES}/party/{partyId}") {
        const val ARG_PARTY_ID = "partyId"
        override val arguments: List<NamedNavArgument> = listOf(
            navArgument(ARG_PARTY_ID) { type = NavType.StringType },
        )
        fun createRoute(partyId: String) = "${Routes.PARTIES}/party/$partyId"
        fun getPartyId(entry: NavBackStackEntry): String = entry.arguments?.getString(ARG_PARTY_ID).orEmpty()
        fun getPartyId(handle: SavedStateHandle): String = handle.get<String>(ARG_PARTY_ID).orEmpty()
    }

    /** One item in a study party: its details and who has finished it. */
    object PartiesItem : Screen("${Routes.PARTIES}/party/{partyId}/item/{itemId}") {
        const val ARG_PARTY_ID = "partyId"
        const val ARG_ITEM_ID = "itemId"
        override val arguments: List<NamedNavArgument> = listOf(
            navArgument(ARG_PARTY_ID) { type = NavType.StringType },
            navArgument(ARG_ITEM_ID) { type = NavType.StringType },
        )
        fun createRoute(partyId: String, itemId: String) = "${Routes.PARTIES}/party/$partyId/item/$itemId"
        fun getItemId(entry: NavBackStackEntry): String = entry.arguments?.getString(ARG_ITEM_ID).orEmpty()
        fun getItemId(handle: SavedStateHandle): String = handle.get<String>(ARG_ITEM_ID).orEmpty()
        data class Args(val partyId: String, val itemId: String)
        fun parseArgs(entry: NavBackStackEntry): Args = Args(
            partyId = entry.arguments?.getString(ARG_PARTY_ID).orEmpty(),
            itemId = entry.arguments?.getString(ARG_ITEM_ID).orEmpty(),
        )
        fun parseArgs(handle: SavedStateHandle): Args = Args(
            partyId = handle.get<String>(ARG_PARTY_ID).orEmpty(),
            itemId = handle.get<String>(ARG_ITEM_ID).orEmpty(),
        )
    }

    /** A study party's info: its members. */
    object PartiesInfo : Screen("${Routes.PARTIES}/party/{partyId}/info") {
        const val ARG_PARTY_ID = "partyId"
        override val arguments: List<NamedNavArgument> = listOf(
            navArgument(ARG_PARTY_ID) { type = NavType.StringType },
        )
        fun createRoute(partyId: String) = "${Routes.PARTIES}/party/$partyId/info"
        fun getPartyId(entry: NavBackStackEntry): String = entry.arguments?.getString(ARG_PARTY_ID).orEmpty()
        fun getPartyId(handle: SavedStateHandle): String = handle.get<String>(ARG_PARTY_ID).orEmpty()
    }

    /** Adding people to a study party by username. */
    object PartiesAddPeople : Screen("${Routes.PARTIES}/party/{partyId}/add") {
        const val ARG_PARTY_ID = "partyId"
        override val arguments: List<NamedNavArgument> = listOf(
            navArgument(ARG_PARTY_ID) { type = NavType.StringType },
        )
        fun createRoute(partyId: String) = "${Routes.PARTIES}/party/$partyId/add"
        fun getPartyId(entry: NavBackStackEntry): String = entry.arguments?.getString(ARG_PARTY_ID).orEmpty()
        fun getPartyId(handle: SavedStateHandle): String = handle.get<String>(ARG_PARTY_ID).orEmpty()
    }

    /** A Darood Party's room: its count, kept as a chat (mock 4E). Its own route, so the bar can hide. */
    object DaroodRoom : Screen("${Routes.PARTIES}/darood/{partyId}") {
        const val ARG_PARTY_ID = "partyId"
        override val arguments: List<NamedNavArgument> = listOf(
            navArgument(ARG_PARTY_ID) { type = NavType.StringType },
        )
        fun createRoute(partyId: String) = "${Routes.PARTIES}/darood/$partyId"
        fun getPartyId(entry: NavBackStackEntry): String = entry.arguments?.getString(ARG_PARTY_ID).orEmpty()
        fun getPartyId(handle: SavedStateHandle): String = handle.get<String>(ARG_PARTY_ID).orEmpty()
    }

    /** A Darood Party's room with its Weekly ranking sheet open: where the week's notification goes. */
    object DaroodWeek : Screen("${Routes.PARTIES}/darood/{partyId}/week") {
        const val ARG_PARTY_ID = "partyId"
        override val arguments: List<NamedNavArgument> = listOf(
            navArgument(ARG_PARTY_ID) { type = NavType.StringType },
        )
        fun createRoute(partyId: String) = "${Routes.PARTIES}/darood/$partyId/week"
        fun getPartyId(entry: NavBackStackEntry): String = entry.arguments?.getString(ARG_PARTY_ID).orEmpty()
        fun getPartyId(handle: SavedStateHandle): String = handle.get<String>(ARG_PARTY_ID).orEmpty()
    }

    /** A Darood Party's weekly ranking: its last 8 closed weeks and the player's place trend. */
    object DaroodWeeks : Screen("${Routes.PARTIES}/darood/{partyId}/weeks") {
        const val ARG_PARTY_ID = "partyId"
        override val arguments: List<NamedNavArgument> = listOf(
            navArgument(ARG_PARTY_ID) { type = NavType.StringType },
        )
        fun createRoute(partyId: String) = "${Routes.PARTIES}/darood/$partyId/weeks"
        fun getPartyId(entry: NavBackStackEntry): String = entry.arguments?.getString(ARG_PARTY_ID).orEmpty()
        fun getPartyId(handle: SavedStateHandle): String = handle.get<String>(ARG_PARTY_ID).orEmpty()
    }

    /** A chess challenge, from its notification (docs/chess-design.md). */
    object ChessChallenge : Screen("chess/challenge/{challengeId}") {
        const val ARG_CHALLENGE_ID = "challengeId"
        override val arguments: List<NamedNavArgument> = listOf(
            navArgument(ARG_CHALLENGE_ID) { type = NavType.StringType },
        )
        fun createRoute(challengeId: String) = "chess/challenge/$challengeId"
        fun getChallengeId(entry: NavBackStackEntry): String = entry.arguments?.getString(ARG_CHALLENGE_ID).orEmpty()
        fun getChallengeId(handle: SavedStateHandle): String = handle.get<String>(ARG_CHALLENGE_ID).orEmpty()
    }

    /** A chess game, to play or watch. */
    object ChessGame : Screen("chess/game/{gameId}") {
        const val ARG_GAME_ID = "gameId"
        override val arguments: List<NamedNavArgument> = listOf(
            navArgument(ARG_GAME_ID) { type = NavType.StringType },
        )
        fun createRoute(gameId: String) = "chess/game/$gameId"
        fun getGameId(entry: NavBackStackEntry): String = entry.arguments?.getString(ARG_GAME_ID).orEmpty()
        fun getGameId(handle: SavedStateHandle): String = handle.get<String>(ARG_GAME_ID).orEmpty()
    }

    /** Two players' record against each other (J157). */
    object ChessHeadToHead : Screen("chess/h2h/{userId}?name={name}") {
        const val ARG_USER_ID = "userId"
        const val ARG_NAME = "name"
        override val arguments: List<NamedNavArgument> = listOf(
            navArgument(ARG_USER_ID) { type = NavType.StringType },
            navArgument(ARG_NAME) {
                type = NavType.StringType
                defaultValue = ""
            },
        )
        fun createRoute(userId: String, name: String?) = "chess/h2h/$userId?name=${android.net.Uri.encode(name.orEmpty())}"
        data class Args(val userId: String, val name: String)
        fun parseArgs(entry: NavBackStackEntry): Args = Args(
            userId = entry.arguments?.getString(ARG_USER_ID).orEmpty(),
            name = entry.arguments?.getString(ARG_NAME).orEmpty(),
        )
        fun parseArgs(handle: SavedStateHandle): Args = Args(
            userId = handle.get<String>(ARG_USER_ID).orEmpty(),
            name = handle.get<String>(ARG_NAME).orEmpty(),
        )
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
        const val ARG_SURAH = "surah"
        const val ARG_AYAH = "ayah"
        override val arguments: List<NamedNavArgument> = listOf(
            navArgument(ARG_SURAH) { type = NavType.IntType },
            navArgument(ARG_AYAH) {
                type = NavType.IntType
                defaultValue = 0
            },
        )
        fun createRoute(surah: Int, ayah: Int = 0) = "${Routes.ISLAMIC}/quran/surah/$surah?ayah=$ayah"
        data class Args(val surah: Int, val ayah: Int)
        fun parseArgs(entry: NavBackStackEntry): Args = Args(
            surah = entry.arguments?.getInt(ARG_SURAH) ?: 1,
            ayah = entry.arguments?.getInt(ARG_AYAH) ?: 0,
        )
        fun parseArgs(handle: SavedStateHandle): Args = Args(
            surah = handle.get<Int>(ARG_SURAH) ?: 1,
            ayah = handle.get<Int>(ARG_AYAH) ?: 0,
        )
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
