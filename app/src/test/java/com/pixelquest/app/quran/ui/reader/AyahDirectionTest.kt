package com.pixelquest.app.quran.ui.reader

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.local.RoomTransactionRunner
import com.pixelquest.app.quran.data.AndroidQuranAssetSource
import com.pixelquest.app.quran.data.QuranImporter
import com.pixelquest.app.quran.data.QuranRepositoryImpl
import com.pixelquest.app.quran.domain.Ayah
import com.pixelquest.app.quran.ui.QuranTypography
import com.pixelquest.app.ui.theme.PixelQuestTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The Arabic is laid out right to left, from the right edge, its ayah sign at its end on the left;
 * the translation left to right; each so whatever the phone's own direction. Measured on Al-Ikhlas's
 * first ayah, as the bundle holds it, in Amiri Quran (Robolectric's native graphics measure text).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class AyahDirectionTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()

    @After
    fun tearDown() {
        db.close()
    }

    private fun firstAyahOfAlIkhlas(): Ayah = runBlocking {
        QuranImporter(AndroidQuranAssetSource(context), db.quranDao(), RoomTransactionRunner(db)) { QuranImporter.TRANSLATION_FILE }.import()
        QuranRepositoryImpl(db.quranDao()).ayah(112, 1)!!
    }

    private fun layout(tag: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        composeTestRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult].action!!.invoke(results)
        return results.single()
    }

    private fun check(phone: LayoutDirection) {
        val ayah = firstAyahOfAlIkhlas()
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides phone) {
                PixelQuestTheme { AyahItem(ayah = ayah, mode = DisplayMode.BOTH, typography = QuranTypography.of()) }
            }
        }
        composeTestRule.waitForIdle()

        val arabic = layout(ReaderTags.arabic(1))
        val width = arabic.size.width.toFloat()
        assertEquals(ResolvedTextDirection.Rtl, arabic.getParagraphDirection(0))
        assertTrue("it starts at the right edge", arabic.getLineRight(0) > width * 0.95f)
        // Its first letter on the right, the sign's number (the last character) further left.
        val first = arabic.getHorizontalPosition(0, usePrimaryDirection = true)
        val last = arabic.getHorizontalPosition(arabic.layoutInput.text.length - 1, usePrimaryDirection = true)
        assertTrue("the sign comes after the text, to its left ($last < $first)", last < first)

        val translation = layout(ReaderTags.translation(1))
        assertEquals(ResolvedTextDirection.Ltr, translation.getParagraphDirection(0))
        assertTrue("it starts at the left edge", translation.getLineLeft(0) < 1f)
    }

    @Test
    fun onAPhoneReadingLeftToRight() = check(LayoutDirection.Ltr)

    @Test
    fun onAPhoneReadingRightToLeft_eachKeepsItsOwnDirection() = check(LayoutDirection.Rtl)
}
