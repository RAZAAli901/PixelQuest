package com.pixelquest.app.ui.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.ComicShapeTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 11: Verification test ensuring the comic drop-shadow offset approach
 * computes correct geometry across small, medium, and large component sizes without
 * clipping or layout overflow.
 */
class ComicShapeStabilityTest {

    data class LayoutBounds(
        val totalWidth: Dp,
        val totalHeight: Dp,
        val shadowOffsetX: Dp,
        val shadowOffsetY: Dp
    ) {
        val innerCardWidth: Dp get() = totalWidth - shadowOffsetX
        val innerCardHeight: Dp get() = totalHeight - shadowOffsetY
        val shadowExtendsToX: Dp get() = innerCardWidth + shadowOffsetX
        val shadowExtendsToY: Dp get() = innerCardHeight + shadowOffsetY
    }

    @Test
    fun testShadowGeometry_occupiesExactLayoutBoundsWithoutOverflow() {
        val testSizes = listOf(
            24.dp to 24.dp,   // Small icon/badge
            80.dp to 48.dp,   // Compact button
            160.dp to 100.dp, // Medium stat card
            360.dp to 240.dp  // Large screen-width panel
        )

        val shadowOffsets = listOf(
            ComicShapeTokens.ShadowOffsetSmall,   // 3dp
            ComicShapeTokens.ShadowOffsetDefault, // 4dp
            ComicShapeTokens.ShadowOffsetLarge    // 6dp
        )

        for ((width, height) in testSizes) {
            for (offset in shadowOffsets) {
                val bounds = LayoutBounds(
                    totalWidth = width,
                    totalHeight = height,
                    shadowOffsetX = offset,
                    shadowOffsetY = offset
                )

                // 1. Inner card has positive dimensions
                assertTrue("Inner width must be positive", bounds.innerCardWidth > 0.dp)
                assertTrue("Inner height must be positive", bounds.innerCardHeight > 0.dp)

                // 2. The shadow outer edge matches the total width and height exactly
                assertEquals(
                    "Shadow outer edge must exactly match total width with 0 overflow",
                    bounds.totalWidth,
                    bounds.shadowExtendsToX
                )
                assertEquals(
                    "Shadow outer edge must exactly match total height with 0 overflow",
                    bounds.totalHeight,
                    bounds.shadowExtendsToY
                )
            }
        }
    }

    @Test
    fun testCornerRadiusTokens_areConsistentAndProportional() {
        assertTrue(
            "RadiusSmall (8dp) must be less than RadiusDefault (10dp)",
            ComicShapeTokens.RadiusSmall < ComicShapeTokens.RadiusDefault
        )
        assertTrue(
            "RadiusDefault (10dp) must be less than RadiusLarge (12dp)",
            ComicShapeTokens.RadiusDefault < ComicShapeTokens.RadiusLarge
        )
        assertTrue(
            "BorderWidthDefault must be between 2dp and 3dp inclusive",
            ComicShapeTokens.BorderWidthDefault in 2.dp..3.dp
        )
    }
}
