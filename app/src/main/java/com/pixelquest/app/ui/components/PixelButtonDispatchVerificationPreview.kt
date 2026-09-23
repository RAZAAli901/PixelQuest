package com.pixelquest.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.ui.theme.DefaultComicColorScheme
import com.pixelquest.app.ui.theme.DefaultLightColorScheme
import com.pixelquest.app.ui.theme.DefaultPixelColorScheme
import com.pixelquest.app.ui.theme.LocalAppColorScheme
import com.pixelquest.app.ui.theme.LocalThemeMode
import com.pixelquest.app.ui.theme.ThemeMode

/**
 * Step 4: Verification preview testing PixelButton dispatch across Pixel, Light,
 * and Comic themes using explicit CompositionLocal overrides.
 *
 * Comic mode itself remains strictly gated from end users per Day 20.
 */
@Preview(name = "PixelButton Dispatch Verification", showBackground = true)
@Composable
fun PixelButtonDispatchVerificationPreview() {
    var lastClicked by remember { mutableStateOf("None") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E2E))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "PIXELBUTTON THEME DISPATCH (Last: $lastClicked)",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )

        // 1. Pixel Mode (Retro Dark 8-bit)
        CompositionLocalProvider(
            LocalThemeMode provides ThemeMode.Pixel,
            LocalAppColorScheme provides DefaultPixelColorScheme
        ) {
            Text(text = "Pixel Mode (Default):", color = Color(0xFFFFCC00), fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PixelButton(
                    text = "CONFIRM",
                    onClick = { lastClicked = "Pixel Yellow" },
                    variant = PixelButtonVariant.YELLOW
                )
                PixelButton(
                    text = "CANCEL",
                    onClick = { lastClicked = "Pixel Blue" },
                    variant = PixelButtonVariant.BLUE
                )
            }
        }

        // 2. Light Mode (Retro Arcade Daylight)
        CompositionLocalProvider(
            LocalThemeMode provides ThemeMode.Light,
            LocalAppColorScheme provides DefaultLightColorScheme
        ) {
            Text(text = "Light Mode:", color = Color(0xFFB45309), fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PixelButton(
                    text = "CONFIRM",
                    onClick = { lastClicked = "Light Amber" },
                    variant = PixelButtonVariant.YELLOW
                )
                PixelButton(
                    text = "CANCEL",
                    onClick = { lastClicked = "Light Sky" },
                    variant = PixelButtonVariant.BLUE
                )
            }
        }

        // 3. Comic Mode (Pop-Art Solid Border + Flat Shadow)
        CompositionLocalProvider(
            LocalThemeMode provides ThemeMode.Comic,
            LocalAppColorScheme provides DefaultComicColorScheme
        ) {
            Text(text = "Comic Mode (Dispatched):", color = Color(0xFFFF5A4E), fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PixelButton(
                    text = "CONFIRM",
                    onClick = { lastClicked = "Comic Coral" },
                    variant = PixelButtonVariant.YELLOW
                )
                PixelButton(
                    text = "CANCEL",
                    onClick = { lastClicked = "Comic Sky" },
                    variant = PixelButtonVariant.BLUE
                )
            }
        }
    }
}
