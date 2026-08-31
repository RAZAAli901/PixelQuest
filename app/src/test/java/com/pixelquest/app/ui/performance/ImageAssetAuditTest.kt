package com.pixelquest.app.ui.performance

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ImageAssetAuditTest {

    @Test
    fun verifyDrawableAssets_sizeEfficiency() {
        val drawableDir = File("src/main/res/drawable")
        if (!drawableDir.exists()) return

        val files = drawableDir.listFiles() ?: return
        files.forEach { file ->
            if (file.name.endsWith(".png") || file.name.endsWith(".jpg")) {
                val sizeKb = file.length() / 1024
                assertTrue(
                    "Asset ${file.name} size ($sizeKb KB) should be under 50 KB",
                    sizeKb < 50
                )
            }
        }
    }
}
