package com.pixelquest.app.zz

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.pixelquest.app.ui.theme.PqSkin
import com.pixelquest.app.ui.theme.PqSkinState
import org.junit.After
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Not committed: renders screens to PNGs in the scratchpad, to look at. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w380dp-h800dp-xhdpi")
abstract class ScratchShot {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @After
    fun reset() { PqSkinState.skin = PqSkin.Comic }

    fun shot(name: String, skin: PqSkin, content: @Composable () -> Unit) {
        PqSkinState.skin = skin
        rule.setContent(content)
        rule.waitForIdle()
        val view = rule.activity.window.decorView
        val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bmp))
        val out = File(System.getProperty("shotDir") ?: "C:/Users/tipto/AppData/Local/Temp/claude/C--Users-tipto-OneDrive-Desktop-PixelQuest/bc08e3cf-c4b1-4b32-bf0a-83df842dac72/scratchpad/shots", "$name-${skin.name.lowercase()}.png")
        out.parentFile.mkdirs()
        out.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
