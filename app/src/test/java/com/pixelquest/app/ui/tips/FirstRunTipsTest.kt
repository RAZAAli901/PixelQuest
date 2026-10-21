package com.pixelquest.app.ui.tips

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pixelquest.app.darood.ui.list.DaroodPartiesUiState
import com.pixelquest.app.islamic.ui.IslamicHubContent
import com.pixelquest.app.islamic.ui.IslamicHubUiState
import com.pixelquest.app.party.ui.PartiesContent
import com.pixelquest.app.quran.ui.QuranHomeScreen
import com.pixelquest.app.quran.ui.list.SurahListUiState
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** First-run tips: one on each of the hub, the Quran and Parties, until dismissed, and once dismissed, gone for good. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class FirstRunTipsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun eachScreen_showsItsTip_untilGotIt() {
        val store = MemoryTipsStore()
        composeTestRule.setContent {
            PixelQuestTheme {
                CompositionLocalProvider(LocalTipsStore provides store) {
                    androidx.compose.foundation.layout.Column {
                        androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.weight(1f)) {
                            IslamicHubContent(IslamicHubUiState(loaded = true), onOpen = {})
                        }
                        androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.weight(1f)) {
                            QuranHomeScreen(onNavigateBack = {}, list = SurahListUiState(loaded = true))
                        }
                        androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.weight(1f)) {
                            PartiesContent(DaroodPartiesUiState(signedIn = true), onSignIn = {}, onOpenParty = {}, onCreateParty = {})
                        }
                    }
                }
            }
        }
        for (tip in Tip.entries) composeTestRule.onNodeWithTag(TipTags.tip(tip)).assertExists()

        composeTestRule.onNodeWithTag(TipTags.dismiss(Tip.QURAN)).performClick()
        composeTestRule.onNodeWithTag(TipTags.tip(Tip.QURAN)).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TipTags.tip(Tip.ISLAMIC_HUB)).assertExists()
        assertEquals(setOf(Tip.QURAN), store.dismissed.value)
    }

    @Test
    fun aTipDismissedBefore_orNoStore_showsNothing() {
        composeTestRule.setContent {
            PixelQuestTheme {
                androidx.compose.foundation.layout.Column {
                    CompositionLocalProvider(LocalTipsStore provides MemoryTipsStore(setOf(Tip.ISLAMIC_HUB))) { FirstRunTip(Tip.ISLAMIC_HUB) }
                    FirstRunTip(Tip.QURAN)
                }
            }
        }
        composeTestRule.onNodeWithTag(TipTags.tip(Tip.ISLAMIC_HUB)).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TipTags.tip(Tip.QURAN)).assertDoesNotExist()
    }

    @Test
    fun aDismissal_isKept_acrossLaunches() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<Application>()
        PrefsTipsStore(context).dismiss(Tip.STUDY_PARTIES)
        assertEquals(setOf(Tip.STUDY_PARTIES), PrefsTipsStore(context).dismissed.value)
    }
}
