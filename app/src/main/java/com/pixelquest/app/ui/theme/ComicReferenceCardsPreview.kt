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
import com.pixelquest.app.ui.components.ComicPanel
import com.pixelquest.app.ui.components.ComicPanelVariant

/**
 * Step 30: Compose Preview replicating a small representative piece of the Nitnode reference —
 * stat cards in each of the three reference container colors (burnt orange, sky blue, lavender)
 * with solid black border and hard-edged flat offset drop shadow.
 */
@Preview(name = "Nitnode Reference Stat Cards", showBackground = true, backgroundColor = 0xFFFAF8F5)
@Composable
fun ComicReferenceCardsPreview() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ComicTokens.PaperBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "NITNODE REFERENCE FIDELITY",
            style = ComicTypography.headlineMedium,
            color = ComicTokens.CoralRed
        )
        Text(
            text = "Validating 3 signature container colors with 2.5dp solid black border and 4dp flat offset shadow",
            style = ComicTypography.bodySmall,
            color = ComicTokens.TextSecondary
        )

        // 1. Burnt Orange Stat Card (~#F0A868)
        ComicPanel(
            modifier = Modifier.fillMaxWidth(),
            variant = ComicPanelVariant.BURNT_ORANGE,
            contentPadding = 16.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CURRENT STREAK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "14 DAYS",
                        style = ComicTypography.displayMedium,
                        color = Color.Black
                    )
                }
                Text(
                    text = "🔥",
                    fontSize = 28.sp
                )
            }
        }

        // 2. Sky Blue Stat Card (~#8ECAE6)
        ComicPanel(
            modifier = Modifier.fillMaxWidth(),
            variant = ComicPanelVariant.SKY_BLUE,
            contentPadding = 16.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "COMPLETION RATE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "94.2%",
                        style = ComicTypography.displayMedium,
                        color = Color.Black
                    )
                }
                Text(
                    text = "⚡",
                    fontSize = 28.sp
                )
            }
        }

        // 3. Lavender Stat Card (~#B8A4D4)
        ComicPanel(
            modifier = Modifier.fillMaxWidth(),
            variant = ComicPanelVariant.LAVENDER,
            contentPadding = 16.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TOTAL XP EARNED",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "3,450 XP",
                        style = ComicTypography.displayMedium,
                        color = Color.Black
                    )
                }
                Text(
                    text = "⭐",
                    fontSize = 28.sp
                )
            }
        }
    }
}
