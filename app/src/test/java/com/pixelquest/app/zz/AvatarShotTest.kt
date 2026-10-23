package com.pixelquest.app.zz

import com.pixelquest.app.ui.components.AvatarClass
import com.pixelquest.app.ui.screens.AvatarPickerScreen
import com.pixelquest.app.ui.screens.LeaderboardLockedScreen
import com.pixelquest.app.ui.theme.PqSkin
import org.junit.Test

class AvatarShotTest : ScratchShot() {
    @Test fun avatar() = shot("avatar", PqSkin.Comic) { AvatarPickerScreen(AvatarClass.Hero, AvatarClass.Mage, {}, {}, {}) }
    @Test fun locked() = shot("locked", PqSkin.Comic) { LeaderboardLockedScreen(null, {}, {}, cloudAvailable = true) }
    @Test fun lockedPixel() = shot("locked", PqSkin.Pixel) { LeaderboardLockedScreen(null, {}, {}, cloudAvailable = true) }
}
