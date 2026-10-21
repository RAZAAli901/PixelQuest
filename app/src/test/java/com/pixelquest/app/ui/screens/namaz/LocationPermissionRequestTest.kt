package com.pixelquest.app.ui.screens.namaz

import android.Manifest
import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** The location request explains itself before Android's prompt, and skips both once allowed. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LocationPermissionRequestTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val results = mutableListOf<Boolean>()

    private fun showRequester() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                val request = rememberLocationPermissionRequest { results += it }
                Button(onClick = { request.launch() }) { Text("USE MY LOCATION") }
            }
        }
    }

    @Test
    fun notAllowedYet_explainsFirst_andNotNowLeavesItOff() {
        showRequester()

        composeTestRule.onNodeWithText("USE MY LOCATION").performClick()
        composeTestRule.onNodeWithText("prayer times", substring = true).assertExists()
        composeTestRule.onNodeWithText("NOT NOW").performClick()

        assertEquals(listOf(false), results)
        assertEquals("the explanation closed", 0, composeTestRule.onAllNodesWithText("NOT NOW").fetchSemanticsNodes().size)
    }

    @Test
    fun alreadyAllowed_reportsAtOnce_withoutAsking() {
        shadowOf(composeTestRule.activity.application).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
        showRequester()

        composeTestRule.onNodeWithText("USE MY LOCATION").performClick()

        assertEquals(listOf(true), results)
        assertEquals(0, composeTestRule.onAllNodesWithText("NOT NOW").fetchSemanticsNodes().size)
    }
}
