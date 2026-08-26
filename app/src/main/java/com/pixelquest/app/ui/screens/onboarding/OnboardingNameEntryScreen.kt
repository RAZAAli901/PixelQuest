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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelButtonVariant
import com.pixelquest.app.ui.components.PixelTextField
import com.pixelquest.app.ui.theme.PixelBackgroundDark
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelTextWhite
import com.pixelquest.app.ui.theme.PixelTypography

@Composable
fun OnboardingNameEntryScreen(
    username: String,
    onUsernameChange: (String) -> Unit,
    isValid: Boolean,
    nameError: String? = null,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PixelBackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "👑 NAME YOUR HERO",
                style = PixelTypography.titleLarge,
                color = PixelGold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Every legendary quest begins with a hero's name.",
                style = PixelTypography.bodyMedium,
                color = PixelTextWhite,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            PixelTextField(
                value = username,
                onValueChange = onUsernameChange,
                label = "HERO USERNAME",
                placeholder = "e.g. PixelKnight",
                errorText = nameError,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
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
                enabled = isValid,
                variant = PixelButtonVariant.YELLOW,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
