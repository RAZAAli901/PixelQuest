package com.pixelquest.app.ui.screens.insight

import com.pixelquest.app.domain.ai.AiErrorCopy
import org.junit.Assert.assertEquals
import org.junit.Test

/** A build without AI set up, or a used-up day, isn't a glitch: each has its own heading. */
class AiErrorHeadingTest {

    @Test
    fun notSetUp_andUsedUp_haveTheirOwnHeadings() {
        assertEquals("Coach not set up", aiErrorHeading(AiErrorCopy.NOT_CONFIGURED))
        assertEquals("That's all for today", aiErrorHeading(AiErrorCopy.DAILY_LIMIT))
    }

    @Test
    fun realFailures_sayTheBriefDidntCome() {
        assertEquals("Couldn't get a brief", aiErrorHeading(AiErrorCopy.UNREADABLE))
        assertEquals("Couldn't get a brief", aiErrorHeading(AiErrorCopy.OFFLINE))
        assertEquals("Couldn't get a brief", aiErrorHeading(AiErrorCopy.BUSY))
    }
}
