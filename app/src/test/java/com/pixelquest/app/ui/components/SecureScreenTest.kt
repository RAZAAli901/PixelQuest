package com.pixelquest.app.ui.components

import android.app.Application
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** S043: sensitive screens set FLAG_SECURE while shown, and clear it when the last one leaves. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SecureScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun secure() = composeTestRule.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0

    @Test
    fun theFlag_followsTheScreens_nestedOnesToo() {
        var outer by mutableStateOf(true)
        var inner by mutableStateOf(true)
        var enabled by mutableStateOf(true)
        composeTestRule.setContent {
            if (outer) SecureScreen()
            if (inner) SecureScreen(enabled = enabled)
        }
        composeTestRule.waitForIdle()
        assertTrue("shown: no screenshots", secure())
        inner = false
        composeTestRule.waitForIdle()
        assertTrue("the outer screen still holds it", secure())
        outer = false
        composeTestRule.waitForIdle()
        assertFalse("both gone: screenshots allowed again", secure())
        inner = true
        enabled = false
        composeTestRule.waitForIdle()
        assertFalse("a screen that doesn't need it (a friendly game) leaves it off", secure())
    }
}
