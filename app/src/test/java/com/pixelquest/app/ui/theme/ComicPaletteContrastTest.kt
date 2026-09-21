package com.pixelquest.app.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Step 3: Verify the Comic palette meets WCAG AA contrast ratios (>= 4.5:1 for normal text),
 * and container variants support high-contrast black ink text.
 */
class ComicPaletteContrastTest {

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
    fun comicColorScheme_meetsWcagAaContrastRequirements() {
        val scheme = DefaultComicColorScheme

        // 1. onBackground on background (>= 4.5:1, target AAA >= 7.0:1)
        val onBgContrast = calculateContrastRatio(scheme.onBackground, scheme.background)
        assertTrue("onBackground contrast was $onBgContrast, expected >= 7.0:1", onBgContrast >= 7.0)

        // 2. onSurface on surface (>= 4.5:1, target AAA >= 7.0:1)
        val onSurfaceContrast = calculateContrastRatio(scheme.onSurface, scheme.surface)
        assertTrue("onSurface contrast was $onSurfaceContrast, expected >= 7.0:1", onSurfaceContrast >= 7.0)

        // 3. onSurfaceVariant on surface (>= 4.5:1 for secondary text)
        val onSurfaceVariantContrast = calculateContrastRatio(scheme.onSurfaceVariant, scheme.surface)
        assertTrue("onSurfaceVariant contrast was $onSurfaceVariantContrast, expected >= 4.5:1", onSurfaceVariantContrast >= 4.5)

        // 4. onPrimary (black text) on primary (coral-red CTA fill) (>= 4.5:1)
        val onPrimaryContrast = calculateContrastRatio(scheme.onPrimary, scheme.primary)
        assertTrue("onPrimary (black text) on primary (coral-red) was $onPrimaryContrast, expected >= 4.5:1", onPrimaryContrast >= 4.5)

        // 5. onPrimaryContainer (black text) on primaryContainer (burnt orange) (>= 4.5:1)
        val onPrimaryContainerContrast = calculateContrastRatio(scheme.onPrimaryContainer, scheme.primaryContainer)
        assertTrue("onPrimaryContainer on burnt orange was $onPrimaryContainerContrast, expected >= 4.5:1", onPrimaryContainerContrast >= 4.5)

        // 6. onSecondary (black text) on secondary (sky blue) (>= 4.5:1)
        val onSecondaryContrast = calculateContrastRatio(scheme.onSecondary, scheme.secondary)
        assertTrue("onSecondary on sky blue was $onSecondaryContrast, expected >= 4.5:1", onSecondaryContrast >= 4.5)

        // 7. onTertiary (black text) on tertiary (lavender) (>= 4.5:1)
        val onTertiaryContrast = calculateContrastRatio(scheme.onTertiary, scheme.tertiary)
        assertTrue("onTertiary on lavender was $onTertiaryContrast, expected >= 4.5:1", onTertiaryContrast >= 4.5)

        // 8. Container variants direct contrast with black ink text
        val burntOrangeContrast = calculateContrastRatio(ComicTokens.SolidBlack, scheme.burntOrange)
        assertTrue("Black ink text on burnt orange was $burntOrangeContrast, expected >= 4.5:1", burntOrangeContrast >= 4.5)

        val skyBlueContrast = calculateContrastRatio(ComicTokens.SolidBlack, scheme.skyBlue)
        assertTrue("Black ink text on sky blue was $skyBlueContrast, expected >= 4.5:1", skyBlueContrast >= 4.5)

        val lavenderContrast = calculateContrastRatio(ComicTokens.SolidBlack, scheme.lavender)
        assertTrue("Black ink text on lavender was $lavenderContrast, expected >= 4.5:1", lavenderContrast >= 4.5)

        // 9. pixelBorder / comicBorder on paper background (solid black outline definition)
        val borderBackgroundContrast = calculateContrastRatio(scheme.pixelBorder, scheme.background)
        assertTrue("Border contrast on background was $borderBackgroundContrast, expected >= 7.0:1", borderBackgroundContrast >= 7.0)

        // 10. error on surface (>= 4.5:1)
        val errorSurfaceContrast = calculateContrastRatio(scheme.error, scheme.surface)
        assertTrue("error contrast on surface was $errorSurfaceContrast, expected >= 4.5:1", errorSurfaceContrast >= 4.5)
    }
}
