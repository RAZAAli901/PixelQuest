package com.pixelquest.app.ui.notifications

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class NotificationsOffBannerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun banner_rendersTextAndInvokesAllow() {
        var allowClicked = 0
        composeTestRule.setContent {
            PixelQuestTheme {
                NotificationsOffBanner(onAllow = { allowClicked++ })
            }
        }

        composeTestRule.onNodeWithText("Reminders are off").assertIsDisplayed()
        composeTestRule.onNodeWithText("ALLOW").performClick()
        assertEquals(1, allowClicked)

        // Without onDismiss provided, close button does not exist
        composeTestRule.onNodeWithContentDescription("Dismiss notification reminder").assertDoesNotExist()
    }

    @Test
    fun banner_withOnDismiss_invokesDismiss() {
        var dismissClicked = 0
        composeTestRule.setContent {
            PixelQuestTheme {
                NotificationsOffBanner(
                    onAllow = {},
                    onDismiss = { dismissClicked++ },
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Dismiss notification reminder")
            .assertIsDisplayed()
            .performClick()

        assertEquals(1, dismissClicked)
    }

    @Test
    fun notificationAsk_storesBannerDismissal() {
        val context = ApplicationProvider.getApplicationContext<Application>()

        NotificationAsk.setBannerDismissed(context, false)
        assertFalse(NotificationAsk.isBannerDismissed(context))

        NotificationAsk.setBannerDismissed(context, true)
        assertTrue(NotificationAsk.isBannerDismissed(context))

        NotificationAsk.setBannerDismissed(context, false)
        assertFalse(NotificationAsk.isBannerDismissed(context))
    }
}
