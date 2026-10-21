package com.pixelquest.app.ui.screens.onboarding

import androidx.compose.runtime.Composable
import com.pixelquest.app.ui.components.AvatarClass
import com.pixelquest.app.ui.screens.AvatarChoiceGrid

@Composable
fun OnboardingAvatarStepScreen(
    selectedAvatarId: String,
    onAvatarSelected: (String) -> Unit,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit
) {
    OnboardingStepFrame(
        step = 2,
        title = "Choose your avatar",
        body = "Pick the hero who stands for you. You can change it any time.",
        primary = "Next",
        onPrimary = onNextClick,
        onBack = onBackClick
    ) {
        AvatarChoiceGrid(selected = AvatarClass.from(selectedAvatarId), onSelect = { onAvatarSelected(it.id) })
    }
}
