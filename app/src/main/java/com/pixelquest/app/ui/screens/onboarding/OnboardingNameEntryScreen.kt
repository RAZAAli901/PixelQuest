package com.pixelquest.app.ui.screens.onboarding

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pixelquest.app.ui.components.PixelTextField

@Composable
fun OnboardingNameEntryScreen(
    username: String,
    onUsernameChange: (String) -> Unit,
    isValid: Boolean,
    nameError: String? = null,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit
) {
    OnboardingStepFrame(
        step = 1,
        title = "Name your hero",
        body = "Every quest begins with a hero's name.",
        primary = "Next",
        onPrimary = onNextClick,
        onBack = onBackClick,
        primaryEnabled = isValid
    ) {
        PixelTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = "Hero name",
            placeholder = "e.g. PixelKnight",
            errorText = nameError,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
