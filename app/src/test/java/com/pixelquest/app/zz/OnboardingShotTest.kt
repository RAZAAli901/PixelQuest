package com.pixelquest.app.zz

import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.ui.screens.onboarding.OnboardingDifficultyStepScreen
import com.pixelquest.app.ui.screens.onboarding.OnboardingWelcomeScreen
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.PqSkin
import org.junit.Test

class OnboardingShotTest : ScratchShot() {
    @Test fun welcome() = shot("ob-welcome", PqSkin.Comic) { PixelQuestTheme { OnboardingWelcomeScreen(onRestoreClick = {}, autoBackupDay = "11 Oct") } }
    @Test fun difficulty() = shot("ob-difficulty", PqSkin.Pixel) {
        PixelQuestTheme { OnboardingDifficultyStepScreen(DifficultyLevel.MEDIUM, {}, {}, {}) }
    }
}
