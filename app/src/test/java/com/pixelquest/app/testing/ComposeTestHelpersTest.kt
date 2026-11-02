package com.pixelquest.app.testing

import android.app.Application
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import com.pixelquest.app.ui.chat.LocalChatSimpleMode
import com.pixelquest.app.ui.theme.PqSkin
import com.pixelquest.app.ui.theme.PqSkinState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ComposeTestHelpersTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun composeTestHelpers_configuresPixelTheme() {
        var observedSkin: PqSkin? = null
        composeTestRule.setPixelTheme {
            observedSkin = PqSkinState.skin
            Text("Pixel Theme Content")
        }

        composeTestRule.onNodeWithText("Pixel Theme Content").assertIsDisplayed()
        assertEquals(PqSkin.Pixel, observedSkin)
    }

    @Test
    fun composeTestHelpers_configuresComicTheme() {
        var observedSkin: PqSkin? = null
        composeTestRule.setComicTheme {
            observedSkin = PqSkinState.skin
            Text("Comic Theme Content")
        }

        composeTestRule.onNodeWithText("Comic Theme Content").assertIsDisplayed()
        assertEquals(PqSkin.Comic, observedSkin)
    }

    @Test
    fun composeTestHelpers_configuresSimpleMode() {
        var simpleModeActive = false
        composeTestRule.setSimpleMode {
            simpleModeActive = LocalSimpleMode.current && LocalChatSimpleMode.current
            Text("Simple Mode Active")
        }

        composeTestRule.onNodeWithText("Simple Mode Active").assertIsDisplayed()
        assertTrue(simpleModeActive)
    }

    @Test
    fun composeTestHelpers_configuresFontScale() {
        var actualFontScale = 1.0f
        composeTestRule.setFontScale(1.75f) {
            actualFontScale = LocalDensity.current.fontScale
            Text("Large Font")
        }

        composeTestRule.onNodeWithText("Large Font").assertIsDisplayed()
        assertEquals(1.75f, actualFontScale, 0.01f)
    }

    @Test
    fun composeTestHelpers_configuresRtl() {
        var direction: LayoutDirection? = null
        composeTestRule.setRtl(Locale("ar")) {
            direction = LocalLayoutDirection.current
            Text("RTL Text")
        }

        composeTestRule.onNodeWithText("RTL Text").assertIsDisplayed()
        assertEquals(LayoutDirection.Rtl, direction)
    }

    @Test
    fun composeTestHelpers_configuresScreenSize() {
        composeTestRule.setScreenSize(ScreenSizes.TabletLandscape) {
            Text("Tablet Landscape Content")
        }

        composeTestRule.onNodeWithText("Tablet Landscape Content").assertIsDisplayed()
    }
}
