package com.pixelquest.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.pixelquest.app.ui.components.ComicPanel
import com.pixelquest.app.ui.components.ComicPanelVariant

/**
 * Step 34: Cross-check the typography scale against the Nitnode reference:
 * - Bold heavy headline display font (Bangers) for high-impact titles
 * - Handwritten/marker accent line style (Kalam) for "NO FLUFF, NO 12-PAGE REPORT..."
 * - Clean sans-serif for high-legibility UI body copy
 */
@Preview(name = "Nitnode Typography Fidelity Cross-Check", showBackground = true, backgroundColor = 0xFFFAF8F5)
@Composable
fun ComicTypographyFidelityPreview() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ComicTokens.PaperBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header
        Text(
            text = "TYPOGRAPHY SCALE FIDELITY",
            style = ComicTypography.headlineMedium,
            color = ComicTokens.CoralRed
        )

        // Hero Card replicating Nitnode's header typography pairing
        ComicPanel(
            modifier = Modifier.fillMaxWidth(),
            variant = ComicPanelVariant.SURFACE,
            contentPadding = 20.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // 1. Handwritten marker accent callout (Reference: "NO FLUFF, NO 12-PAGE REPORT...")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "✏️",
                        fontSize = 16.sp
                    )
                    Text(
                        text = "NO FLUFF, NO 12-PAGE REPORT — JUST RAW ACTION",
                        style = ComicCalloutStyles.calloutMedium,
                        color = ComicTokens.CoralRed
                    )
                }

                // 2. Bold heavy display headline
                Text(
                    text = "BUILD BULLETPROOF HABITS THAT STICK",
                    style = ComicTypography.displayLarge,
                    color = ComicTokens.SolidBlack
                )

                // 3. Clean readable sans-serif body
                Text(
                    text = "Transform your daily quest log into an unstoppable momentum engine. " +
                            "Every completed habit awards XP, builds streaks, and levels up your hero profile.",
                    style = ComicTypography.bodyMedium,
                    color = ComicTokens.TextPrimary
                )
            }
        }

        // Accent Callout Sticker in Lavender container
        ComicPanel(
            modifier = Modifier.fillMaxWidth(),
            variant = ComicPanelVariant.LAVENDER,
            contentPadding = 16.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "CRITICAL INSIGHT",
                    style = ComicTypography.headlineSmall,
                    color = ComicTokens.SolidBlack
                )
                Text(
                    text = "“90% of failures happen from setting too many quests at once.”",
                    style = ComicCalloutStyles.calloutLarge,
                    color = ComicTokens.SolidBlack
                )
                Text(
                    text = "Focus on 3 micro-habits before expanding your quest journal.",
                    style = ComicTypography.bodySmall,
                    color = ComicTokens.TextSecondary
                )
            }
        }

        // Font Family Pairing Summary
        ComicPanel(
            modifier = Modifier.fillMaxWidth(),
            variant = ComicPanelVariant.SKY_BLUE,
            contentPadding = 14.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "FONT ROLE ARCHITECTURE",
                    style = ComicTypography.titleMedium,
                    color = ComicTokens.SolidBlack
                )
                Text(
                    text = "• Headlines: Bangers Display (Heavy comic punch)",
                    style = ComicTypography.bodySmall,
                    color = ComicTokens.SolidBlack
                )
                Text(
                    text = "• Callouts: Kalam Marker (Authentic handwritten accent)",
                    style = ComicTypography.bodySmall,
                    color = ComicTokens.SolidBlack
                )
                Text(
                    text = "• Body/UI: Clean Sans-Serif (Uncompromising legibility)",
                    style = ComicTypography.bodySmall,
                    color = ComicTokens.SolidBlack
                )
            }
        }
    }
}
