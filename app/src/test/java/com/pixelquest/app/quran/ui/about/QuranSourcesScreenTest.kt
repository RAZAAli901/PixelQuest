package com.pixelquest.app.quran.ui.about

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pixelquest.app.quran.data.QuranSources
import com.pixelquest.app.quran.domain.Translation
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** About and Sources: each source with its credit, its notice whole, and links that open the browser. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w411dp-h914dp")
class QuranSourcesScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    // Stand-ins: QuranSourcesTest reads the real notices.
    private val state = QuranSourcesUiState(
        sources = QuranSources("v1", "# A NOTICE\n# kept whole", "Metadata notice"),
        translation = Translation("en.sahih", "Saheeh International", "Saheeh International", "English", "April 24, 2011", "Non-commercial use only.", "# Its notice")
    )
    private val opened = mutableListOf<String>()

    private fun show(mode: ThemeMode = ThemeMode.Pixel) {
        val handler = object : UriHandler {
            override fun openUri(uri: String) { opened += uri }
        }
        composeTestRule.setContent {
            CompositionLocalProvider(LocalUriHandler provides handler) {
                PixelQuestTheme(themeMode = mode) { QuranSourcesScreen(state = state, onNavigateBack = {}) }
            }
        }
    }

    @Test
    fun everySource_hasItsSection() {
        show()

        for (tag in listOf(SourcesTags.TEXT, SourcesTags.METADATA, SourcesTags.TRANSLATION, SourcesTags.FONT, SourcesTags.APP)) {
            composeTestRule.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun theNotices_areShownWhole() {
        show(ThemeMode.Comic)

        composeTestRule.onNodeWithText("# A NOTICE\n# kept whole").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("# Its notice").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Non-commercial use only.").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Data version v1").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun aLink_opensItsSource() {
        show(ThemeMode.Comic)

        composeTestRule.onNodeWithTag(SourcesTags.link("https://tanzil.net")).performScrollTo().performClick()
        composeTestRule.onNodeWithTag(SourcesTags.link("https://openfontlicense.org")).performScrollTo().performClick()

        assertEquals(listOf("https://tanzil.net", "https://openfontlicense.org"), opened)
    }
}
