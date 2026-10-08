package com.pixelquest.app.domain.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** A build that can't reach Gemini doesn't offer the AI Coach opt-in. */
class AiAvailabilityTest {

    @Test
    fun throughTheProxy_aRealSupabaseProjectIsNeeded() {
        assertTrue(AiAvailability.isConfigured(viaProxy = true, supabaseUrl = "https://abcd.supabase.co", geminiApiKey = ""))
        assertFalse(AiAvailability.isConfigured(viaProxy = true, supabaseUrl = "https://placeholder-project.supabase.co", geminiApiKey = "real-key"))
    }

    @Test
    fun directly_aRealKeyIsNeeded() {
        assertTrue(AiAvailability.isConfigured(viaProxy = false, supabaseUrl = "", geminiApiKey = "AIza-real"))
        assertFalse(AiAvailability.isConfigured(viaProxy = false, supabaseUrl = "https://abcd.supabase.co", geminiApiKey = "placeholder-gemini-key"))
        assertFalse(AiAvailability.isConfigured(viaProxy = false, supabaseUrl = "", geminiApiKey = " "))
    }

    @Test
    fun theSettingsCard_offersTheOptInOnlyWhenItCanWork_butAlwaysLetsYouTurnItOff() {
        assertEquals(AiCoachSetting.CAN_OPT_IN, AiCoachSetting.of(isEnabled = false, availableInBuild = true))
        assertEquals(AiCoachSetting.NOT_IN_THIS_BUILD, AiCoachSetting.of(isEnabled = false, availableInBuild = false))
        assertEquals(AiCoachSetting.ENABLED, AiCoachSetting.of(isEnabled = true, availableInBuild = false))
    }
}
