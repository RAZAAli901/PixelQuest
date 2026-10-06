package com.pixelquest.app.ui.screens.insight

import com.pixelquest.app.domain.ai.AiErrorCopy
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

/** "COMMUNICATION GLITCH!" over "isn't set up in this build" read wrong on the device. */
class AiErrorHeadingTest {

    @Test
    fun notSetUp_andUsedUp_haveTheirOwnHeadings() {
        assertEquals("COACH NOT SET UP", aiErrorHeading(AiErrorCopy.NOT_CONFIGURED, ThemeMode.Comic))
        assertEquals("THAT'S ALL FOR TODAY!", aiErrorHeading(AiErrorCopy.DAILY_LIMIT, ThemeMode.Comic))
        assertEquals("AI Coach not set up", aiErrorHeading(AiErrorCopy.NOT_CONFIGURED, ThemeMode.Light))
        assertEquals("[DAILY LIMIT REACHED]", aiErrorHeading(AiErrorCopy.DAILY_LIMIT, ThemeMode.Pixel))
    }

    @Test
    fun realFailures_keepTheOriginalHeadings() {
        assertEquals("COMMUNICATION GLITCH!", aiErrorHeading(AiErrorCopy.UNREADABLE, ThemeMode.Comic))
        assertEquals("Insight Generation Error", aiErrorHeading(AiErrorCopy.OFFLINE, ThemeMode.Light))
        assertEquals("[TRANSMISSION FAILED]", aiErrorHeading(AiErrorCopy.BUSY, ThemeMode.Pixel))
        assertEquals("[TRANSMISSION FAILED]", aiErrorHeading(AiErrorCopy.BUSY, ThemeMode.System))
    }
}
