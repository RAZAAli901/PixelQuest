package com.pixelquest.app.quran

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.pixelquest.app.R
import com.pixelquest.app.islamic.IslamicIcon
import com.pixelquest.app.islamic.IslamicItem
import com.pixelquest.app.islamic.IslamicItemProvider
import com.pixelquest.app.quran.ui.QuranPlaceholderScreen
import com.pixelquest.app.ui.navigation.Screen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * The Quran on the Islamic hub (docs/islamic-hub.md). Always shown: it needs no account, and its
 * text will be on the phone. It brings its own screen into the section's graph, a placeholder until
 * the reader is built.
 */
class QuranIslamicItem @Inject constructor() : IslamicItemProvider {

    override val order: Int = 30

    override fun item(): Flow<IslamicItem> = flowOf(
        IslamicItem(
            id = ID,
            titleRes = R.string.quran_title,
            descriptionRes = R.string.quran_description,
            icon = IslamicIcon("📖", pixel = R.drawable.ic_quran_pixel, comic = R.drawable.ic_quran_comic),
            route = Screen.Quran.route
        )
    )

    override fun NavGraphBuilder.screens(navController: NavHostController) {
        composable(Screen.Quran.route) {
            QuranPlaceholderScreen(onNavigateBack = { navController.popBackStack() })
        }
    }

    companion object {
        const val ID = "quran"
    }
}
