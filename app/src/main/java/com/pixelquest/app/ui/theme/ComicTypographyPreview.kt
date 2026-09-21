package com.pixelquest.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Step 15: Compose Preview showing the full Comic Typography scale in action:
 * - Bangers display font for bold action headlines
 * - Clean readable sans for UI body and labels
 * - Kalam marker font for callout stickers and accents
 */
@Preview(name = "Comic Typography Scale Preview", showBackground = true, backgroundColor = 0xFFFAF8F5)
@Composable
fun ComicTypographyPreview() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ComicTokens.PaperBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "COMIC TYPOGRAPHY SCALE",
            style = ComicTypography.displaySmall,
            color = ComicTokens.CoralRed
        )

        // Section 1: Bangers Display & Headlines
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ComicTokens.PanelSurface, shape = RoundedCornerShape(10.dp))
                .comicBorder(width = 2.5.dp, color = Color.Black, cornerRadius = 10.dp)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "HEADLINE & TITLE DISPLAY (BANGERS)",
                style = ComicTypography.labelMedium,
                color = ComicTokens.TextSecondary
            )
            Text(
                text = "DISPLAY LARGE: LEVEL 42 HERO!",
                style = ComicTypography.displayLarge,
                color = ComicTokens.TextPrimary
            )
            Text(
                text = "Display Medium: Mission Accomplished",
                style = ComicTypography.displayMedium,
                color = ComicTokens.TextPrimary
            )
            Text(
                text = "Headline Large: Daily Habit Quest Briefing",
                style = ComicTypography.headlineLarge,
                color = ComicTokens.TextPrimary
            )
            Text(
                text = "Title Large: Supercharged Habit Streak",
                style = ComicTypography.titleLarge,
                color = ComicTokens.TextPrimary
            )
        }

        // Section 2: Kalam Marker Accent Callouts (Nitnode Style)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ComicTokens.BurntOrange, shape = RoundedCornerShape(10.dp))
                .comicBorder(width = 2.5.dp, color = Color.Black, cornerRadius = 10.dp)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "MARKER CALLOUT ACCENTS (KALAM)",
                style = ComicTypography.labelMedium,
                color = Color.Black
            )
            Text(
                text = "\"NO FLUFF, NO 12-PAGE REPORT — JUST IMMEDIATE ACTION!\"",
                style = ComicCalloutStyles.calloutLarge,
                color = Color.Black
            )
            Text(
                text = "Note: Streak multiplier active until midnight!",
                style = ComicCalloutStyles.calloutMedium,
                color = Color.Black
            )
            Text(
                text = "Pro tip: Complete morning workouts before 9:00 AM",
                style = ComicCalloutStyles.calloutSmall,
                color = Color.Black
            )
        }

        // Section 3: Clean Readable Sans Body & Labels
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ComicTokens.SkyBlue, shape = RoundedCornerShape(10.dp))
                .comicBorder(width = 2.5.dp, color = Color.Black, cornerRadius = 10.dp)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "READABLE UI SANS (BODY & LABELS)",
                style = ComicTypography.labelMedium,
                color = Color.Black
            )
            Text(
                text = "Body Large: Habit completion history is tracked accurately in Room DB regardless of the active theme.",
                style = ComicTypography.bodyLarge,
                color = Color.Black
            )
            Text(
                text = "Body Medium: Clean high-contrast secondary information designed for mobile readability.",
                style = ComicTypography.bodyMedium,
                color = Color.Black
            )
            Text(
                text = "Body Small: Tiny metadata caption at 10sp with crisp letterform rendering.",
                style = ComicTypography.bodySmall,
                color = Color.Black
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "TAG: HABIT",
                    style = ComicTypography.labelLarge,
                    color = Color.Black
                )
                Text(
                    text = "STATUS: ACTIVE",
                    style = ComicTypography.labelMedium,
                    color = Color.Black
                )
            }
        }
    }
}
