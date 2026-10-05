package com.pixelquest.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * THEMING.md lists contrast ratios as "`#FG` on `#BG` | **N:1**". Until Day 29 several were estimates
 * (gold was listed at 5.2:1 and is 4.9:1), so this recomputes every listed pair with the WCAG 2.1
 * formula and fails when the doc and the colours disagree.
 */
class ThemingDocContrastTest {

    private val row = Regex("""`#([0-9A-Fa-f]{6})` on `#([0-9A-Fa-f]{6})`.*?\*\*([\d.]+):1\*\*""")

    private fun luminance(hex: String): Double {
        fun channel(i: Int): Double {
            val c = hex.substring(i, i + 2).toInt(16) / 255.0
            return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(0) + 0.7152 * channel(2) + 0.0722 * channel(4)
    }

    private fun contrast(a: String, b: String): Double {
        val l1 = luminance(a) + 0.05
        val l2 = luminance(b) + 0.05
        return maxOf(l1, l2) / minOf(l1, l2)
    }

    @Test
    fun everyListedRatio_matchesItsColours() {
        val doc = File("../THEMING.md").takeIf { it.exists() } ?: File("THEMING.md")
        assertTrue("THEMING.md must exist", doc.exists())

        val rows = doc.readLines().mapNotNull { line -> row.find(line)?.destructured }
        assertTrue("THEMING.md should list contrast ratios", rows.size >= 20)

        rows.forEach { (fg, bg, listed) ->
            val actual = contrast(fg, bg)
            // Listed to one decimal (or two when just under a threshold), so allow rounding.
            assertEquals("#$fg on #$bg is listed as $listed:1", listed.toDouble(), actual, 0.06)
        }
    }
}
