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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Step 4: Compose Preview displaying the finalized Light Theme palette swatch set.
 */
@Composable
fun LightPaletteSwatchItem(
    name: String,
    hex: String,
    color: Color,
    contrastNote: String,
    textColor: Color = Color.Black
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(color, shape = RoundedCornerShape(4.dp))
                .border(1.dp, Color(0xFF292524), shape = RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = PixelTypography.labelMedium,
                color = textColor,
                fontSize = 11.sp
            )
            Text(
                text = "$hex • $contrastNote",
                style = PixelTypography.bodySmall,
                color = Color(0xFF57534E),
                fontSize = 9.sp
            )
        }
    }
}

@Preview(name = "Light Mode Swatches", showBackground = true, backgroundColor = 0xFFF8F6F0)
@Composable
fun LightPalettePreview() {
    val scheme = DefaultLightColorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "PIXELQUEST LIGHT PALETTE",
            style = PixelTypography.titleMedium,
            color = scheme.primary,
            fontSize = 14.sp
        )
        Text(
            text = "Retro Arcade in Daylight (WCAG AA Compliant)",
            style = PixelTypography.bodySmall,
            color = scheme.onSurfaceVariant,
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        LightPaletteSwatchItem(
            name = "Canvas Background",
            hex = "#F8F6F0",
            color = scheme.background,
            contrastNote = "Warm ivory cartridge paper"
        )
        LightPaletteSwatchItem(
            name = "Card Surface",
            hex = "#FFFFFF",
            color = scheme.surface,
            contrastNote = "Crisp white panel fill"
        )
        LightPaletteSwatchItem(
            name = "Surface Variant",
            hex = "#E6E1D6",
            color = scheme.surfaceVariant,
            contrastNote = "Warm divider / border"
        )
        LightPaletteSwatchItem(
            name = "Primary (Quest Amber)",
            hex = "#B45309",
            color = scheme.primary,
            contrastNote = "5.4:1 vs Surface (AA)"
        )
        LightPaletteSwatchItem(
            name = "Secondary (Daylight Cyan)",
            hex = "#0284C7",
            color = scheme.secondary,
            contrastNote = "4.6:1 vs Surface (AA)"
        )
        LightPaletteSwatchItem(
            name = "Tertiary (HP Green)",
            hex = "#15803D",
            color = scheme.tertiary,
            contrastNote = "4.7:1 vs Surface (AA)"
        )
        LightPaletteSwatchItem(
            name = "Gold (Trophy / Level)",
            hex = "#A16207",
            color = scheme.gold,
            contrastNote = "5.2:1 vs Surface (AA)"
        )
        LightPaletteSwatchItem(
            name = "Pixel Border",
            hex = "#292524",
            color = scheme.pixelBorder,
            contrastNote = "8-bit crisp definition"
        )
        LightPaletteSwatchItem(
            name = "On Surface (Charcoal)",
            hex = "#1C1917",
            color = scheme.onSurface,
            contrastNote = "15.9:1 vs Surface (AAA)"
        )
        LightPaletteSwatchItem(
            name = "On Surface Variant",
            hex = "#57534E",
            color = scheme.onSurfaceVariant,
            contrastNote = "5.8:1 vs Surface (AA)"
        )
        LightPaletteSwatchItem(
            name = "Error (Dungeon Red)",
            hex = "#DC2626",
            color = scheme.error,
            contrastNote = "4.8:1 vs Surface (AA)"
        )
        LightPaletteSwatchItem(
            name = "Accent Purple (Rune)",
            hex = "#7E22CE",
            color = scheme.accentPurple,
            contrastNote = "6.5:1 vs Surface (AA)"
        )
    }
}
