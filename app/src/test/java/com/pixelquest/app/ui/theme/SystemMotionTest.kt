package com.pixelquest.app.ui.theme

import android.app.Application
import android.content.Context
import android.provider.Settings
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Android's own "Remove animations" reduces motion in PixelQuest too, without the app's REDUCE
 * MOTION switch, and turning it on or off takes effect while the app is open.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SystemMotionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun setAnimatorScale(scale: Float) {
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, scale)
        // What Android does when the setting changes.
        context.contentResolver.notifyChange(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), null)
    }

    @Test
    fun removeAnimations_reducesMotion_evenWithTheAppSwitchOff_andFollowsChanges() {
        setAnimatorScale(0f)
        var reduced: Boolean? = null
        composeTestRule.setContent {
            PixelQuestTheme(isReduceMotion = rememberReduceMotion(appSetting = false)) {
                reduced = LocalReduceMotion.current
            }
        }
        composeTestRule.waitForIdle()
        assertEquals(true, reduced)

        setAnimatorScale(1f) // animations back on in Android's settings
        composeTestRule.waitForIdle()
        assertEquals(false, reduced)
    }

    @Test
    fun theAppSwitch_stillWorksOnItsOwn() {
        setAnimatorScale(1f)
        var reduced: Boolean? = null
        composeTestRule.setContent {
            PixelQuestTheme(isReduceMotion = rememberReduceMotion(appSetting = true)) {
                reduced = LocalReduceMotion.current
            }
        }
        composeTestRule.waitForIdle()
        assertEquals(true, reduced)
        assertEquals(false, SystemMotion.animationsOff(context.contentResolver))
    }
}
