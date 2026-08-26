package com.pixelquest.app.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.components.PixelAvatarGrid
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelButtonVariant
import com.pixelquest.app.ui.theme.PixelBackgroundDark
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelTextWhite
import com.pixelquest.app.ui.theme.PixelTypography

@Composable
fun OnboardingAvatarStepScreen(
    selectedAvatarId: String,
    onAvatarSelected: (String) -> Unit,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PixelBackgroundDark)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🧙 CHOOSE YOUR AVATAR",
            style = PixelTypography.titleLarge,
            color = PixelGold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Pick an 8-bit avatar to represent your hero in PixelQuest.",
            style = PixelTypography.bodyMedium,
            color = PixelTextWhite,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        PixelAvatarGrid(
            selectedAvatarId = selectedAvatarId,
            onAvatarSelected = onAvatarSelected,
            modifier = Modifier.weight(1f)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PixelButton(
                text = "◀ BACK",
                onClick = onBackClick,
                variant = PixelButtonVariant.BLUE,
                modifier = Modifier.weight(1f)
            )
            PixelButton(
                text = "NEXT ▶",
                onClick = onNextClick,
                variant = PixelButtonVariant.YELLOW,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
