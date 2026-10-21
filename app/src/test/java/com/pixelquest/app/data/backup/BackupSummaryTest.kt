package com.pixelquest.app.data.backup

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.pixelquest.app.ui.screens.settings.RestoreDataConfirmDialog
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The restore dialog says what the backup holds; it used to warn without saying what was in the file. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class BackupSummaryTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val profile = """"userProfile": {"id": 1, "username": "Aria", "avatarId": "avatar_mage", "level": 5, "totalXp": 2400}"""
    private val streak = """"streak": {"id": 1, "currentStreak": 4, "longestStreak": 9}"""
    private fun quests(n: Int) = (1..n).joinToString(",", "\"tasks\": [", "]") {
        """{"id": $it, "name": "Q$it", "scheduledTime": "09:00", "scheduledDay": "2026-10-01", "recurrenceType": "DAILY", "category": "OTHER"}"""
    }

    @Test
    fun aBackupFromThisVersion() {
        val json = """{"formatVersion": 4, "appVersion": "1.2.0", "settings": {"sound": false}, $profile, $streak, ${quests(12)}}"""
        assertEquals(
            "Aria, level 5, a 4-day streak and 12 quests, with settings. Saved by PixelQuest 1.2.0.",
            BackupSummary.describe(DataExportImport.importFromJson(json))
        )
    }

    @Test
    fun aV1_1_0Backup_withoutAVersion() {
        assertEquals(
            "Aria, level 5, a 4-day streak and 1 quest. Saved by an earlier PixelQuest.",
            BackupSummary.describe(DataExportImport.importFromJson("""{$profile, $streak, ${quests(1)}}"""))
        )
    }

    @Test
    fun noStreak_isLeftOut() {
        val json = """{$profile, "streak": {"id": 1, "currentStreak": 0, "longestStreak": 9}, ${quests(2)}}"""
        assertEquals("Aria, level 5 and 2 quests. Saved by an earlier PixelQuest.", BackupSummary.describe(DataExportImport.importFromJson(json)))
    }

    @Test
    fun theDialog_showsIt() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                RestoreDataConfirmDialog(onConfirm = {}, onDismiss = {}, summary = "Aria, level 5 and 2 quests.")
            }
        }
        composeTestRule.onNodeWithText("This backup has Aria, level 5 and 2 quests.", substring = true).assertExists()
    }

    @Test
    fun onTheWelcomeScreen_itDoesntWarnAboutAHeroThatIsntThere() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                RestoreDataConfirmDialog(onConfirm = {}, onDismiss = {}, summary = "Aria, level 5 and 2 quests.", replacesAGame = false)
            }
        }
        composeTestRule.onNodeWithText("RESTORE THIS BACKUP?").assertExists()
        composeTestRule.onNodeWithText("come back from it", substring = true).assertExists()
        assertEquals(0, composeTestRule.onAllNodesWithText("overwrite", substring = true).fetchSemanticsNodes().size)
    }
}
