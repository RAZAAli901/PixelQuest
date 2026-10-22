package com.pixelquest.app.chess

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.pixelquest.app.chess.domain.ChessColour
import com.pixelquest.app.chess.domain.ChessGame
import com.pixelquest.app.chess.domain.ClockSync
import com.pixelquest.app.chess.domain.GameResult
import com.pixelquest.app.chess.domain.GameStatus
import com.pixelquest.app.chess.domain.HeadToHead
import com.pixelquest.app.chess.domain.Rating
import com.pixelquest.app.chess.domain.ResultReason
import com.pixelquest.app.chess.domain.TimeControl
import com.pixelquest.app.chess.ui.ChessBoardTags
import com.pixelquest.app.chess.ui.ChessGameActions
import com.pixelquest.app.chess.ui.ChessGameScreen
import com.pixelquest.app.chess.ui.ChessPartyState
import com.pixelquest.app.chess.ui.ChessSessionState
import com.pixelquest.app.chess.ui.ChessTab
import com.pixelquest.app.chess.ui.ChessTabActions
import com.pixelquest.app.chess.ui.ChessTabTags
import com.pixelquest.app.chess.ui.GameOverTags
import com.pixelquest.app.chess.ui.SessionPhase
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.PqSkin
import com.pixelquest.app.ui.theme.PqSkinState
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Instant
import kotlin.math.abs

/**
 * Screenshots of chess (J168) in each look: the board mid-game, the end card, and the study
 * party's Chess tab, saved to app/build/outputs/chess-screenshots for review. As with the other
 * screenshot tests, golden images would drift with each machine's fonts, so the checks are that
 * each has a picture in it, the board is square, and its squares are the design's colours.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w411dp-h1600dp-xhdpi")
class ChessScreenshotTest(private val look: Look) {

    enum class Look(val skin: PqSkin, val mode: ThemeMode, val simple: Boolean, val dark: Int, val light: Int) {
        COMIC(PqSkin.Comic, ThemeMode.Comic, simple = false, dark = 0xFF8ECAE6.toInt(), light = 0xFFFFFDF8.toInt()),
        PIXEL(PqSkin.Pixel, ThemeMode.Pixel, simple = false, dark = 0xFFE8A33D.toInt(), light = 0xFFFFFAF0.toInt()),
        SIMPLE(PqSkin.Comic, ThemeMode.Comic, simple = true, dark = 0xFF8ECAE6.toInt(), light = 0xFFFFFDF8.toInt()),
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun looks() = Look.entries.map { arrayOf<Any>(it) }
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @After
    fun resetSkin() {
        PqSkinState.skin = PqSkin.Comic
    }

    private val t = Instant.parse("2026-10-22T10:00:00Z")
    private val midGame = "r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/3P1N2/PPP2PPP/RNBQK2R w KQkq - 1 5"
    private fun game(status: GameStatus) = ChessGame(
        "g", "p", "me", "sara", TimeControl.THREE, true, status,
        if (status == GameStatus.FINISHED) GameResult.WHITE else null, if (status == GameStatus.FINISHED) ResultReason.CHECKMATE else null,
        midGame, ChessColour.WHITE, 132_000, 161_000, 8, t, t, t, if (status == GameStatus.FINISHED) t else null,
        whiteChange = 14, blackChange = -14, ratingApplied = status == GameStatus.FINISHED
    )

    private fun capture(name: String): Bitmap {
        composeTestRule.waitForIdle()
        val view = composeTestRule.activity.window.decorView.rootView
        val window = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        composeTestRule.runOnUiThread { view.draw(Canvas(window)) }
        File("build/outputs/chess-screenshots").apply { mkdirs() }.let { dir ->
            File(dir, "$name-${look.name.lowercase()}.png").outputStream().use { window.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        val colours = HashSet<Int>()
        for (x in 0 until window.width step 3) for (y in 0 until window.height step 3) colours += window.getPixel(x, y)
        assertTrue("$name $look is blank (${colours.size} colours)", colours.size > 20)
        return window
    }

    private fun near(a: Int, b: Int) = listOf(16, 8, 0).all { shift -> abs((a shr shift and 0xFF) - (b shr shift and 0xFF)) <= 12 }

    @Test
    fun theBoard_andTheEndCard() {
        PqSkinState.skin = look.skin
        var state by androidx.compose.runtime.mutableStateOf(
            ChessSessionState(SessionPhase.ACTIVE, game(GameStatus.ACTIVE), me = "me", myColour = ChessColour.WHITE, watchers = 4, names = mapOf("me" to "raza", "sara" to "sara"), points = mapOf("me" to 1046, "sara" to 1102), simple = look.simple)
        )
        composeTestRule.setContent { PixelQuestTheme(themeMode = look.mode) { ChessGameScreen(state, ClockSync(), ChessGameActions()) } }
        val board = composeTestRule.onNodeWithTag(ChessBoardTags.BOARD).getBoundsInRoot()
        assertTrue("$look: the board is square", abs((board.right - board.left).value - (board.bottom - board.top).value) < 2f)
        val shot = capture("game")
        // a8's square: a light one; b8's: dark. Their top right corners, clear of the coordinates and pieces.
        val density = composeTestRule.activity.resources.displayMetrics.density
        fun pixelOf(square: String): Int {
            val b = composeTestRule.onNodeWithTag(ChessBoardTags.square(square)).getBoundsInRoot()
            return shot.getPixel(((b.right.value - 3) * density).toInt(), ((b.top.value + 3) * density).toInt())
        }
        assertTrue("$look: a8 is the design's light (${Integer.toHexString(pixelOf("a8"))})", near(pixelOf("a8"), look.light))
        assertTrue("$look: b8 is the design's dark (${Integer.toHexString(pixelOf("b8"))})", near(pixelOf("b8"), look.dark))

        state = state.copy(phase = SessionPhase.FINISHED, game = game(GameStatus.FINISHED), headToHead = listOf(HeadToHead("me", "sara", null, 5, 1, 2)))
        // Ended by checkmate on screen: the card comes after the board's stamp, and its animations settle.
        composeTestRule.mainClock.advanceTimeBy(3_000)
        composeTestRule.onNodeWithTag(GameOverTags.CARD).assertExists()
        capture("over")
    }

    @Test
    fun theChessTab() {
        PqSkinState.skin = look.skin
        val state = ChessPartyState(
            me = "me", simple = look.simple, names = mapOf("me" to "raza", "sara" to "sara", "ali" to "ali"),
            liveGames = listOf(game(GameStatus.ACTIVE).copy(whiteId = "ali", blackId = "sara", watchers = 3)),
            leaderboard = listOf(Rating("sara", TimeControl.THREE, 1102, 20, 12, 3, 5, 1110), Rating("me", TimeControl.THREE, 1060, 19, 9, 4, 6, 1070), Rating("ali", TimeControl.THREE, 998, 17, 6, 2, 9, 1010))
        )
        composeTestRule.setContent { PixelQuestTheme(themeMode = look.mode) { Column { ChessTab(state, ChessTabActions()) } } }
        composeTestRule.onNodeWithTag(ChessTabTags.TAB).assertExists()
        capture("tab")
    }
}
