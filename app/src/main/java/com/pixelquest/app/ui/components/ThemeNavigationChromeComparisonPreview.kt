package com.pixelquest.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.ui.navigation.Screen
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.PixelTypography
import com.pixelquest.app.ui.theme.ThemeMode

/**
 * Step 6 (Day 23): Compose Preview comparing navigation chrome (Top Bar, Bottom Nav, FAB)
 * across all three themes: Pixel (Dark Retro), Light (Daylight), and Comic (Pop-Art Ink).
 */
@Composable
private fun NavigationChromeShowcase(title: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PixelTheme.colors.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = PixelTypography.titleMedium,
            color = PixelTheme.colors.primary,
            fontSize = 12.sp
        )

        // 1. Top App Bar Chrome
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "TOP APP BAR",
                style = PixelTypography.bodySmall,
                color = PixelTheme.colors.onSurfaceVariant,
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            PixelTopAppBar(
                title = "QUEST LOG",
                navigationIcon = {
                    Text("◀", style = PixelTypography.titleMedium, color = PixelTheme.colors.primary)
                },
                actions = {
                    Text("⚙️", fontSize = 16.sp, modifier = Modifier.padding(horizontal = 4.dp))
                }
            )
        }

        // 2. Floating Action Button
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "FLOATING ACTION BUTTON",
                style = PixelTypography.bodySmall,
                color = PixelTheme.colors.onSurfaceVariant,
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            PixelFloatingActionButton(onClick = {})
        }

        // 3. Bottom Navigation Bar Chrome
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "BOTTOM NAVIGATION BAR",
                style = PixelTypography.bodySmall,
                color = PixelTheme.colors.onSurfaceVariant,
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            PixelBottomNavBar(
                currentRoute = Screen.Tasks.route,
                onNavigate = {}
            )
        }
    }
}

@Preview(name = "Navigation Chrome - Pixel Mode", showBackground = true, widthDp = 380)
@Composable
private fun NavigationChromePixelPreview() {
    PixelQuestTheme(themeMode = ThemeMode.Pixel) {
        NavigationChromeShowcase("PIXEL MODE NAVIGATION CHROME")
    }
}

@Preview(name = "Navigation Chrome - Light Mode", showBackground = true, widthDp = 380)
@Composable
private fun NavigationChromeLightPreview() {
    PixelQuestTheme(themeMode = ThemeMode.Light) {
        NavigationChromeShowcase("LIGHT MODE NAVIGATION CHROME")
    }
}

@Preview(name = "Navigation Chrome - Comic Mode", showBackground = true, widthDp = 380)
@Composable
private fun NavigationChromeComicPreview() {
    PixelQuestTheme(themeMode = ThemeMode.Comic) {
        NavigationChromeShowcase("COMIC MODE NAVIGATION CHROME")
    }
}

@Preview(name = "Navigation Chrome - 3-Theme Comparison", showBackground = true, widthDp = 380)
@Composable
private fun NavigationChromeComparisonPreview() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        PixelQuestTheme(themeMode = ThemeMode.Pixel) {
            NavigationChromeShowcase("1. PIXEL MODE")
        }
        Spacer(modifier = Modifier.height(12.dp))
        PixelQuestTheme(themeMode = ThemeMode.Light) {
            NavigationChromeShowcase("2. LIGHT MODE")
        }
        Spacer(modifier = Modifier.height(12.dp))
        PixelQuestTheme(themeMode = ThemeMode.Comic) {
            NavigationChromeShowcase("3. COMIC MODE")
        }
    }
}
