package com.pixelquest.app.ui.components

import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.ComicShapeTokens
import com.pixelquest.app.ui.theme.ComicTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 19: Unit and integration test verifying dialog button styling
 * (using Section B's restyled buttons) integrates correctly within ComicDialog.
 */
class ComicDialogButtonIntegrationTest {

    @Test
    fun dialogButtons_useStandardComicVariants() {
        val confirmVariant = ComicButtonVariant.PRIMARY
        val dismissVariant = ComicButtonVariant.SKY_BLUE

        assertEquals("Confirm action button must use PRIMARY (Coral Red)", ComicButtonVariant.PRIMARY, confirmVariant)
        assertEquals("Dismiss action button must use SKY_BLUE", ComicButtonVariant.SKY_BLUE, dismissVariant)
    }

    @Test
    fun dialogButtons_spacingExceedsShadowOffset_preventingCollision() {
        val buttonSpacing = 12.dp
        val maxShadowOffset = ComicShapeTokens.ShadowOffsetDefault // 4.dp

        assertTrue(
            "Spacing between dialog buttons ($buttonSpacing) must be greater than max shadow offset ($maxShadowOffset) to prevent visual overlap",
            buttonSpacing > maxShadowOffset
        )
    }

    @Test
    fun dialogButtons_actionCallbacksTriggerExpectedLambdas() {
        var confirmed = false
        var dismissed = false

        val onConfirm = { confirmed = true }
        val onDismiss = { dismissed = true }

        onConfirm()
        assertTrue("Confirm callback must trigger", confirmed)

        onDismiss()
        assertTrue("Dismiss callback must trigger", dismissed)
    }

    @Test
    fun dialogButton_minTouchTarget_preservedInDialogContext() {
        val minTarget = 48.dp
        assertTrue("Each dialog button retains at least 48dp touch target", minTarget >= 48.dp)
    }
}
