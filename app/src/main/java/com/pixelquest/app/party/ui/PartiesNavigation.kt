package com.pixelquest.app.party.ui

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.pixelquest.app.darood.ui.create.CreatePartyScreen
import com.pixelquest.app.darood.ui.create.CreatePartyViewModel
import com.pixelquest.app.darood.ui.info.PartyInfoScreen
import com.pixelquest.app.darood.ui.invite.AddPeopleScreen
import com.pixelquest.app.darood.ui.list.DaroodPartiesViewModel
import com.pixelquest.app.party.domain.PartyType
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.study.ui.StudyItemScreen
import com.pixelquest.app.study.ui.StudyItemScreenArgs
import com.pixelquest.app.study.ui.StudyPartyScreen
import com.pixelquest.app.study.ui.StudyPartyViewModel
import com.pixelquest.app.ui.navigation.Screen

/**
 * The main app's parties (docs/party-core.md): the study parties, from the side menu's Parties,
 * outside the Islamic section. The screens are the shared ones Darood Parties use, told they're
 * showing study parties. A study party opens on its own screen (its tabs); its info opens from there.
 */
fun NavGraphBuilder.partiesScreens(navController: NavHostController) {
    val study = navArgument(DaroodPartiesViewModel.ARG_TYPE) { type = NavType.StringType; defaultValue = PartyType.STUDY.wire }
    composable(Screen.Parties.route, arguments = listOf(study)) {
        PartiesScreen(
            onSignIn = { navController.navigate(Screen.Account.route) },
            onOpenParty = { partyId -> navController.navigate(Screen.PartiesParty.createRoute(partyId)) { launchSingleTop = true } },
            onCreateParty = { navController.navigate(Screen.PartiesCreate.route) { launchSingleTop = true } }
        )
    }
    composable(
        Screen.PartiesCreate.route,
        arguments = listOf(navArgument(CreatePartyViewModel.ARG_TYPE) { type = NavType.StringType; defaultValue = PartyType.STUDY.wire })
    ) {
        CreatePartyScreen(
            onNavigateBack = { navController.popBackStack() },
            // The new party opens in place of the form, so Back returns to the list.
            onCreated = { partyId ->
                navController.navigate(Screen.PartiesParty.createRoute(partyId)) {
                    popUpTo(Screen.PartiesCreate.route) { inclusive = true }
                }
            },
            onChooseUsername = { navController.navigate(Screen.UsernameSetup.route) { launchSingleTop = true } }
        )
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
            onOpenItem = { itemId -> navController.navigate(Screen.PartiesItem.createRoute(partyId, itemId)) { launchSingleTop = true } }
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
            viewModel = if (party != null) hiltViewModel(party) else hiltViewModel()
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
