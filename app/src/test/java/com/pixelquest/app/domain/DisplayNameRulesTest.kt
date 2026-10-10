package com.pixelquest.app.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The app's name filter agrees with the server's trigger on the shared list of names
 * (supabase/tests/display_names.json, which the local-database test also uses). Both used to refuse
 * ordinary names like "Titan_Slayer" and "Stitch" because "tit" matched inside them.
 */
class DisplayNameRulesTest {

    private val names = run {
        val file = listOf(File("../supabase/tests/display_names.json"), File("supabase/tests/display_names.json")).first { it.exists() }
        Json.parseToJsonElement(file.readText()).jsonObject
    }

    private fun list(key: String) = names[key]!!.jsonArray.map { it.jsonPrimitive.content }

    @Test
    fun ordinaryNames_pass() {
        for (name in list("allowed")) {
            assertTrue(name, DisplayNameModerator.isAppropriate(name))
            assertTrue(name, com.pixelquest.app.ui.screens.account.AccountViewModel.validateDisplayName(name) == null)
        }
    }

    @Test
    fun offensiveNames_areRefused() {
        for (name in list("blocked")) {
            assertFalse(name, DisplayNameModerator.isAppropriate(name))
        }
    }
}
