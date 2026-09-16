package com.pixelquest.app.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Step 3: Verify the light palette meets WCAG AA contrast ratios (>= 4.5:1 for normal text).
 */
class LightPaletteContrastTest {

    private fun calculateLuminance(color: Color): Double {
        fun linearize(component: Float): Double {
            return if (component <= 0.03928f) {
                component / 12.92
            } else {
                ((component + 0.055) / 1.055).pow(2.4)
            }
        }

        val r = linearize(color.red)
        val g = linearize(color.green)
        val b = linearize(color.blue)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun calculateContrastRatio(c1: Color, c2: Color): Double {
        val l1 = calculateLuminance(c1)
        val l2 = calculateLuminance(c2)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    @Test
    fun lightColorScheme_meetsWcagAaContrastRequirements() {
        val scheme = DefaultLightColorScheme

        // 1. onBackground on background (>= 4.5:1, target AAA >= 7.0:1)
        val onBgContrast = calculateContrastRatio(scheme.onBackground, scheme.background)
        assertTrue("onBackground contrast was $onBgContrast, expected >= 7.0:1", onBgContrast >= 7.0)

        // 2. onSurface on surface (>= 4.5:1, target AAA >= 7.0:1)
        val onSurfaceContrast = calculateContrastRatio(scheme.onSurface, scheme.surface)
        assertTrue("onSurface contrast was $onSurfaceContrast, expected >= 7.0:1", onSurfaceContrast >= 7.0)

        // 3. onSurfaceVariant on surface (>= 4.5:1 for secondary text)
        val onSurfaceVariantContrast = calculateContrastRatio(scheme.onSurfaceVariant, scheme.surface)
        assertTrue("onSurfaceVariant contrast was $onSurfaceVariantContrast, expected >= 4.5:1", onSurfaceVariantContrast >= 4.5)

        // 4. primary on surface (>= 4.5:1 for text/accent)
        val primarySurfaceContrast = calculateContrastRatio(scheme.primary, scheme.surface)
        assertTrue("primary contrast on surface was $primarySurfaceContrast, expected >= 4.5:1", primarySurfaceContrast >= 4.5)

        // 5. secondary on surface (>= 4.5:1)
        val secondarySurfaceContrast = calculateContrastRatio(scheme.secondary, scheme.surface)
        assertTrue("secondary contrast on surface was $secondarySurfaceContrast, expected >= 4.5:1", secondarySurfaceContrast >= 4.5)

        // 6. tertiary on surface (>= 4.5:1)
        val tertiarySurfaceContrast = calculateContrastRatio(scheme.tertiary, scheme.surface)
        assertTrue("tertiary contrast on surface was $tertiarySurfaceContrast, expected >= 4.5:1", tertiarySurfaceContrast >= 4.5)

        // 7. error on surface (>= 4.5:1)
        val errorSurfaceContrast = calculateContrastRatio(scheme.error, scheme.surface)
        assertTrue("error contrast on surface was $errorSurfaceContrast, expected >= 4.5:1", errorSurfaceContrast >= 4.5)

        // 8. gold on surface (>= 4.5:1)
        val goldSurfaceContrast = calculateContrastRatio(scheme.gold, scheme.surface)
        assertTrue("gold contrast on surface was $goldSurfaceContrast, expected >= 4.5:1", goldSurfaceContrast >= 4.5)

        // 9. onPrimary on primary (>= 4.5:1)
        val onPrimaryContrast = calculateContrastRatio(scheme.onPrimary, scheme.primary)
        assertTrue("onPrimary contrast was $onPrimaryContrast, expected >= 4.5:1", onPrimaryContrast >= 4.5)

        // 10. onSecondary on secondary (>= 4.5:1)
        val onSecondaryContrast = calculateContrastRatio(scheme.onSecondary, scheme.secondary)
        assertTrue("onSecondary contrast was $onSecondaryContrast, expected >= 4.5:1", onSecondaryContrast >= 4.5)

        // 11. onTertiary on tertiary (>= 4.5:1)
        val onTertiaryContrast = calculateContrastRatio(scheme.onTertiary, scheme.tertiary)
        assertTrue("onTertiary contrast was $onTertiaryContrast, expected >= 4.5:1", onTertiaryContrast >= 4.5)
    }
}
