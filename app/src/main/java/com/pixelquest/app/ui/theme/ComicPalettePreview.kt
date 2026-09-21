package com.pixelquest.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Step 4: Compose Preview displaying the finalized Comic Book Theme palette swatch set.
 */
@Composable
fun ComicPaletteSwatchItem(
    name: String,
    hex: String,
    color: Color,
    contrastNote: String,
    textColor: Color = Color.Black
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Comic swatch with solid black border & offset shadow effect
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(Color.Black, shape = RoundedCornerShape(8.dp))
                .padding(end = 2.dp, bottom = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(color, shape = RoundedCornerShape(8.dp))
                    .border(2.dp, Color.Black, shape = RoundedCornerShape(8.dp))
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$hex • $contrastNote",
                color = Color(0xFF4A4A4A),
                fontSize = 10.sp
            )
        }
    }
}

@Preview(name = "Comic Mode Swatches", showBackground = true, backgroundColor = 0xFFFAF8F5)
@Composable
fun ComicPalettePreview() {
    val scheme = DefaultComicColorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "COMIC BOOK UI PALETTE",
            color = scheme.primary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "Nitnode-Inspired Bold Pop-Art Palette (WCAG AA Compliant)",
            color = scheme.onSurfaceVariant,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Reference Core Accent & Containers
        ComicPaletteSwatchItem(
            name = "Primary / CTA Accent (Coral Red)",
            hex = "#FF5A4E",
            color = scheme.primary,
            contrastNote = "6.77:1 with Black Ink text (AA)"
        )
        ComicPaletteSwatchItem(
            name = "Container 1 (Burnt Orange)",
            hex = "#F0A868",
            color = scheme.burntOrange,
            contrastNote = "10.2:1 with Black Ink (AAA)"
        )
        ComicPaletteSwatchItem(
            name = "Container 2 (Sky Blue)",
            hex = "#8ECAE6",
            color = scheme.skyBlue,
            contrastNote = "12.6:1 with Black Ink (AAA)"
        )
        ComicPaletteSwatchItem(
            name = "Container 3 (Lavender)",
            hex = "#B8A4D4",
            color = scheme.lavender,
            contrastNote = "9.4:1 with Black Ink (AAA)"
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Surfaces and Outlines
        ComicPaletteSwatchItem(
            name = "Paper Background",
            hex = "#FAF8F5",
            color = scheme.background,
            contrastNote = "Warm comic newsprint canvas"
        )
        ComicPaletteSwatchItem(
            name = "Panel Surface (White)",
            hex = "#FFFFFF",
            color = scheme.surface,
            contrastNote = "Crisp panel card fill"
        )
        ComicPaletteSwatchItem(
            name = "Surface Variant (Paper Dark)",
            hex = "#F4EFE6",
            color = scheme.surfaceVariant,
            contrastNote = "Subtle card & divider variant"
        )
        ComicPaletteSwatchItem(
            name = "Comic Border & Shadow (Solid Black)",
            hex = "#000000",
            color = scheme.comicBorder,
            contrastNote = "Hard-edged ink stroke & flat drop shadow"
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Text & Status
        ComicPaletteSwatchItem(
            name = "Ink Text Primary",
            hex = "#1A1A1A",
            color = scheme.onSurface,
            contrastNote = "16.1:1 on Surface (AAA)"
        )
        ComicPaletteSwatchItem(
            name = "Ink Text Secondary",
            hex = "#4A4A4A",
            color = scheme.onSurfaceVariant,
            contrastNote = "8.6:1 on Surface (AAA)"
        )
        ComicPaletteSwatchItem(
            name = "Gold Action Accent",
            hex = "#FFB703",
            color = scheme.gold,
            contrastNote = "Comic star & trophy highlight"
        )
        ComicPaletteSwatchItem(
            name = "Action Error Red",
            hex = "#D32F2F",
            color = scheme.error,
            contrastNote = "Comic danger alert fill"
        )
    }
}
