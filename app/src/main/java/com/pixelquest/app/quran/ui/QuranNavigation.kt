package com.pixelquest.app.quran.ui

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.pixelquest.app.quran.ui.reader.SurahReaderRoute
import com.pixelquest.app.quran.ui.reader.SurahReaderViewModel
import com.pixelquest.app.ui.navigation.Screen

/**
 * The Quran reader's screens, added to the Islamic section's graph by its item (QuranIslamicItem):
 * the surah list and the reader.
 */
fun NavGraphBuilder.quranScreens(navController: NavHostController) {
    composable(Screen.Quran.route) {
        QuranRoute(
            onNavigateBack = { navController.popBackStack() },
            onOpenSurah = { surah -> navController.navigate(Screen.QuranSurah.createRoute(surah)) },
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
        SurahReaderRoute(onNavigateBack = { navController.popBackStack() })
    }
}
