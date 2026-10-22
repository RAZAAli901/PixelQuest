package com.pixelquest.app.quran.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.pixelquest.app.quran.ui.about.QuranSourcesRoute
import com.pixelquest.app.quran.ui.reader.SurahReaderRoute
import com.pixelquest.app.quran.ui.search.QuranSearchRoute
import com.pixelquest.app.quran.ui.translations.TranslationPickerRoute
import com.pixelquest.app.ui.navigation.Screen

/**
 * The Quran reader's screens, added to the Islamic section's graph by its item (QuranIslamicItem):
 * the surah list (with the bookmarks as its Saved tab), search, the reader, its translation, and
 * About and Sources.
 */
fun NavGraphBuilder.quranScreens(navController: NavHostController) {
    quranScreen(Screen.Quran.route) {
        QuranRoute(
            onNavigateBack = { navController.popBackStack() },
            onOpenSurah = { surah -> navController.navigate(Screen.QuranSurah.createRoute(surah)) },
            onOpenAyah = { surah, ayah -> navController.navigate(Screen.QuranSurah.createRoute(surah, ayah)) },
            onOpenSearch = { navController.navigate(Screen.QuranSearch.route) { launchSingleTop = true } },
            onOpenSources = { navController.navigate(Screen.QuranSources.route) { launchSingleTop = true } },
        )
    }
    quranScreen(Screen.QuranSources.route) {
        QuranSourcesRoute(onNavigateBack = { navController.popBackStack() })
    }
    quranScreen(Screen.QuranTranslations.route) {
        TranslationPickerRoute(onNavigateBack = { navController.popBackStack() })
    }
    quranScreen(Screen.QuranSearch.route) {
        QuranSearchRoute(
            onNavigateBack = { navController.popBackStack() },
            onOpenSurah = { surah -> navController.navigate(Screen.QuranSurah.createRoute(surah)) },
            onOpenAyah = { surah, ayah -> navController.navigate(Screen.QuranSurah.createRoute(surah, ayah)) },
        )
    }
    quranScreen(
        route = Screen.QuranSurah.route,
        arguments = Screen.QuranSurah.arguments,
    ) {
        SurahReaderRoute(
            onNavigateBack = { navController.popBackStack() },
            onOpenTranslations = { navController.navigate(Screen.QuranTranslations.route) { launchSingleTop = true } },
            // The next (or previous) surah opens in this one's place: Back still returns to the list.
            onOpenSurah = { surah ->
                navController.navigate(Screen.QuranSurah.createRoute(surah)) {
                    popUpTo(Screen.QuranSurah.route) { inclusive = true }
                }
            },
        )
    }
}

/** One of the Quran's screens: in its look, told whether the app is in Simple Mode ([QuranLook]). */
private fun NavGraphBuilder.quranScreen(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    content: @Composable (NavBackStackEntry) -> Unit,
) {
    composable(route = route, arguments = arguments) { entry -> QuranLook { content(entry) } }
}
