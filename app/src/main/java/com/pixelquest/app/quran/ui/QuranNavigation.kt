package com.pixelquest.app.quran.ui

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.pixelquest.app.quran.ui.about.QuranSourcesRoute
import com.pixelquest.app.quran.ui.bookmarks.BookmarksRoute
import com.pixelquest.app.quran.ui.reader.SurahReaderRoute
import com.pixelquest.app.quran.ui.reader.SurahReaderViewModel
import com.pixelquest.app.quran.ui.search.QuranSearchRoute
import com.pixelquest.app.quran.ui.translations.TranslationPickerRoute
import com.pixelquest.app.ui.navigation.Screen

/**
 * The Quran reader's screens, added to the Islamic section's graph by its item (QuranIslamicItem):
 * the surah list, search, bookmarks, the reader, its translation, and About and Sources.
 */
fun NavGraphBuilder.quranScreens(navController: NavHostController) {
    composable(Screen.Quran.route) {
        QuranRoute(
            onNavigateBack = { navController.popBackStack() },
            onOpenSurah = { surah -> navController.navigate(Screen.QuranSurah.createRoute(surah)) },
            onOpenAyah = { surah, ayah -> navController.navigate(Screen.QuranSurah.createRoute(surah, ayah)) },
            onOpenBookmarks = { navController.navigate(Screen.QuranBookmarks.route) { launchSingleTop = true } },
            onOpenSearch = { navController.navigate(Screen.QuranSearch.route) { launchSingleTop = true } },
            onOpenSources = { navController.navigate(Screen.QuranSources.route) { launchSingleTop = true } }
        )
    }
    composable(Screen.QuranSources.route) {
        QuranSourcesRoute(onNavigateBack = { navController.popBackStack() })
    }
    composable(Screen.QuranTranslations.route) {
        TranslationPickerRoute(onNavigateBack = { navController.popBackStack() })
    }
    composable(Screen.QuranSearch.route) {
        QuranSearchRoute(
            onNavigateBack = { navController.popBackStack() },
            onOpenSurah = { surah -> navController.navigate(Screen.QuranSurah.createRoute(surah)) },
            onOpenAyah = { surah, ayah -> navController.navigate(Screen.QuranSurah.createRoute(surah, ayah)) }
        )
    }
    composable(Screen.QuranBookmarks.route) {
        BookmarksRoute(
            onNavigateBack = { navController.popBackStack() },
            onOpenAyah = { surah, ayah -> navController.navigate(Screen.QuranSurah.createRoute(surah, ayah)) }
        )
    }
    composable(
        route = Screen.QuranSurah.route,
        arguments = listOf(
            navArgument(SurahReaderViewModel.ARG_SURAH) { type = NavType.IntType },
            navArgument(SurahReaderViewModel.ARG_AYAH) { type = NavType.IntType; defaultValue = 0 }
        )
    ) {
        SurahReaderRoute(
            onNavigateBack = { navController.popBackStack() },
            onOpenTranslations = { navController.navigate(Screen.QuranTranslations.route) { launchSingleTop = true } },
            // The next (or previous) surah opens in this one's place: Back still returns to the list.
            onOpenSurah = { surah ->
                navController.navigate(Screen.QuranSurah.createRoute(surah)) {
                    popUpTo(Screen.QuranSurah.route) { inclusive = true }
                }
            }
        )
    }
}
