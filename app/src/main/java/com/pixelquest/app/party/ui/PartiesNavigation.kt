package com.pixelquest.app.party.ui

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.pixelquest.app.darood.ui.create.CreatePartyScreen
import com.pixelquest.app.darood.ui.chat.PartyChatRoute
import com.pixelquest.app.darood.ui.info.PartyInfoScreen
import com.pixelquest.app.darood.ui.invite.AddPeopleScreen
import com.pixelquest.app.darood.ui.race.RaceBoardScreen
import com.pixelquest.app.party.domain.PartyType
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.study.ui.StudyItemScreen
import com.pixelquest.app.study.ui.StudyItemScreenArgs
import com.pixelquest.app.study.ui.StudyPartyScreen
import com.pixelquest.app.study.ui.StudyPartyViewModel
import com.pixelquest.app.ui.navigation.Screen

/**
 * The parties (docs/party-core.md; UI v2, mocks 4B to 4J), outside the Islamic section: the hub of
 * every kind, starting one, a Darood Party's room or a study party's own screen, its info, adding
 * people, and Darood's race board.
 */
fun NavGraphBuilder.partiesScreens(navController: NavHostController) {
    // A party opens in its kind's own screen.
    fun roomOf(partyId: String, type: PartyType) =
        if (type == PartyType.DAROOD) Screen.DaroodRoom.createRoute(partyId) else Screen.PartiesParty.createRoute(partyId)
    composable(Screen.Parties.route) {
        PartiesScreen(
            onSignIn = { navController.navigate(Screen.Account.route) },
            onOpenParty = { party -> navController.navigate(roomOf(party.id, party.type)) { launchSingleTop = true } },
            onCreateParty = { navController.navigate(Screen.PartiesCreate.route) { launchSingleTop = true } },
            onNavigateBack = { navController.popBackStack() },
            onOpenRaceBoard = { navController.navigate(Screen.PartiesRaceBoard.route) { launchSingleTop = true } },
            onChooseUsername = { navController.navigate(Screen.UsernameSetup.route) { launchSingleTop = true } }
        )
    }
    composable(Screen.PartiesCreate.route) {
        CreatePartyScreen(
            onNavigateBack = { navController.popBackStack() },
            // The new party opens in place of the form, so Back returns to the list.
            onCreated = { partyId, type ->
                navController.navigate(roomOf(partyId, type)) {
                    popUpTo(Screen.PartiesCreate.route) { inclusive = true }
                }
            },
            onChooseUsername = { navController.navigate(Screen.UsernameSetup.route) { launchSingleTop = true } }
        )
    }
    composable(
        route = Screen.DaroodRoom.route,
        arguments = listOf(navArgument("partyId") { type = NavType.StringType })
    ) { entry ->
        val partyId = entry.arguments?.getString("partyId").orEmpty()
        PartyChatRoute(
            partyId = partyId,
            onNavigateBack = { navController.popBackStack() },
            onOpenInfo = { navController.navigate(Screen.PartiesInfo.createRoute(partyId)) { launchSingleTop = true } },
            onAddPeople = { navController.navigate(Screen.PartiesAddPeople.createRoute(partyId)) { launchSingleTop = true } },
            onOpenWeeks = { navController.navigate(Screen.DaroodWeeks.createRoute(partyId)) { launchSingleTop = true } },
            weeklyViewModel = hiltViewModel()
        )
    }
    // From the week's notification: the same room, its Weekly ranking sheet open.
    composable(
        route = Screen.DaroodWeek.route,
        arguments = listOf(navArgument("partyId") { type = NavType.StringType })
    ) { entry ->
        val partyId = entry.arguments?.getString("partyId").orEmpty()
        PartyChatRoute(
            partyId = partyId,
            onNavigateBack = { navController.popBackStack() },
            onOpenInfo = { navController.navigate(Screen.PartiesInfo.createRoute(partyId)) { launchSingleTop = true } },
            onAddPeople = { navController.navigate(Screen.PartiesAddPeople.createRoute(partyId)) { launchSingleTop = true } },
            openWeekly = true,
            onOpenWeeks = { navController.navigate(Screen.DaroodWeeks.createRoute(partyId)) { launchSingleTop = true } },
            weeklyViewModel = hiltViewModel()
        )
    }
    composable(
        route = Screen.DaroodWeeks.route,
        arguments = listOf(navArgument("partyId") { type = NavType.StringType })
    ) {
        com.pixelquest.app.darood.ui.weekly.WeeklyRankingScreen(onNavigateBack = { navController.popBackStack() })
    }
    // Chess's notifications open these; Phase E (docs/chess-design.md) puts the board here.
    composable(Screen.ChessChallenge.route, arguments = listOf(navArgument("challengeId") { type = NavType.StringType })) {
        com.pixelquest.app.chess.ui.IncomingChallengeRoute(
            onNavigateBack = { navController.popBackStack() },
            // Accepted: the game opens in the challenge's place.
            onOpenGame = { game ->
                navController.navigate(Screen.ChessGame.createRoute(game)) { popUpTo(Screen.ChessChallenge.route) { inclusive = true } }
            }
        )
    }
    composable(Screen.ChessGame.route, arguments = listOf(navArgument("gameId") { type = NavType.StringType })) {
        com.pixelquest.app.chess.ui.ChessGameRoute(
            onNavigateBack = { navController.popBackStack() },
            // Next live game: in this one's place, so Back still leaves watching.
            onOpenGame = { next -> navController.navigate(Screen.ChessGame.createRoute(next)) { popUpTo(Screen.ChessGame.route) { inclusive = true } } }
        )
    }
    composable(
        Screen.ChessHeadToHead.route,
        arguments = listOf(navArgument("userId") { type = NavType.StringType }, navArgument("name") { type = NavType.StringType; defaultValue = "" })
    ) {
        com.pixelquest.app.chess.ui.HeadToHeadRoute(onNavigateBack = { navController.popBackStack() })
    }
    composable(Screen.PartiesRaceBoard.route) {
        RaceBoardScreen(onNavigateBack = { navController.popBackStack() })
    }
    composable(
        route = Screen.PartiesParty.route,
        arguments = listOf(navArgument(StudyPartyViewModel.ARG_PARTY) { type = NavType.StringType })
    ) { entry ->
        val partyId = entry.arguments?.getString(StudyPartyViewModel.ARG_PARTY).orEmpty()
        StudyPartyScreen(
            onNavigateBack = { navController.popBackStack() },
            onOpenInfo = { navController.navigate(Screen.PartiesInfo.createRoute(partyId)) { launchSingleTop = true } },
            onAddPeople = { navController.navigate(Screen.PartiesAddPeople.createRoute(partyId)) { launchSingleTop = true } },
            onOpenItem = { itemId -> navController.navigate(Screen.PartiesItem.createRoute(partyId, itemId)) { launchSingleTop = true } },
            remindersViewModel = hiltViewModel(),
            chatViewModel = hiltViewModel(),
            chessViewModel = hiltViewModel(),
            onOpenChessGame = { game -> navController.navigate(Screen.ChessGame.createRoute(game)) { launchSingleTop = true } },
            onOpenRival = { id, name -> navController.navigate(Screen.ChessHeadToHead.createRoute(id, name)) { launchSingleTop = true } }
        )
    }
    composable(
        route = Screen.PartiesItem.route,
        arguments = listOf(
            navArgument(StudyPartyViewModel.ARG_PARTY) { type = NavType.StringType },
            navArgument(StudyItemScreenArgs.ARG_ITEM) { type = NavType.StringType }
        )
    ) { entry ->
        // The party's own screen's view model, when the item was opened from it: one source for both.
        val party = remember(entry) { runCatching { navController.getBackStackEntry(Screen.PartiesParty.route) }.getOrNull() }
        StudyItemScreen(
            itemId = entry.arguments?.getString(StudyItemScreenArgs.ARG_ITEM).orEmpty(),
            onNavigateBack = { navController.popBackStack() },
            viewModel = if (party != null) hiltViewModel(party) else hiltViewModel(),
            chatViewModel = if (party != null) hiltViewModel(party) else hiltViewModel()
        )
    }
    composable(
        route = Screen.PartiesInfo.route,
        arguments = listOf(navArgument("partyId") { type = NavType.StringType })
    ) { entry ->
        val partyId = entry.arguments?.getString("partyId").orEmpty()
        PartyInfoScreen(
            onNavigateBack = { navController.popBackStack() },
            onAddPeople = { navController.navigate(Screen.PartiesAddPeople.createRoute(partyId)) { launchSingleTop = true } },
            // Out of the party: back to the list.
            onLeft = {
                if (!navController.popBackStack(Screen.Parties.route, inclusive = false)) {
                    navController.navigate(Screen.Parties.route) { launchSingleTop = true }
                }
            }
        )
    }
    composable(
        route = Screen.PartiesAddPeople.route,
        arguments = listOf(navArgument("partyId") { type = NavType.StringType })
    ) {
        AddPeopleScreen(onNavigateBack = { navController.popBackStack() })
    }
}
