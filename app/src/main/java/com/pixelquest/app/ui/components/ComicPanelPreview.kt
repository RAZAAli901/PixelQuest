package com.pixelquest.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.pixelquest.app.ui.theme.ComicTokens

/**
 * Step 10: Compose Preview demonstrating [ComicPanel] in isolation.
 * Displays various panel variants (White Surface, Burnt Orange, Sky Blue, Lavender, Newsprint Paper)
 * with solid black borders and unblurred offset drop shadows.
 */
@Preview(name = "Comic Panel Variants Preview", showBackground = true, backgroundColor = 0xFFFAF8F5)
@Composable
fun ComicPanelPreview() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ComicTokens.PaperBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "COMIC PANEL PROTOTYPES",
            color = ComicTokens.CoralRed,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "Isolated shape language demonstration: solid black border + offset shadow",
            color = ComicTokens.TextSecondary,
            fontSize = 11.sp
        )

        // 1. Surface Variant (White Card)
        ComicPanel(
            modifier = Modifier.fillMaxWidth(),
            variant = ComicPanelVariant.SURFACE
        ) {
            Column {
                Text(
                    text = "WHITE SURFACE PANEL",
                    color = ComicTokens.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Standard comic panel card with solid 2.5dp black ink border and 4dp flat black drop shadow.",
                    color = ComicTokens.TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        // 2. Burnt Orange Container Variant
        ComicPanel(
            modifier = Modifier.fillMaxWidth(),
            variant = ComicPanelVariant.BURNT_ORANGE
        ) {
            Column {
                Text(
                    text = "BURNT ORANGE CONTAINER (#F0A868)",
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "High-contrast action container matching Nitnode reference palette.",
                    color = Color.Black,
                    fontSize = 11.sp
                )
            }
        }

        // 3. Sky Blue Container Variant
        ComicPanel(
            modifier = Modifier.fillMaxWidth(),
            variant = ComicPanelVariant.SKY_BLUE
        ) {
            Column {
                Text(
                    text = "SKY BLUE CONTAINER (#8ECAE6)",
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Cool energetic stat container with crisp black ink text.",
                    color = Color.Black,
                    fontSize = 11.sp
                )
            }
        }

        // 4. Lavender Container Variant
        ComicPanel(
            modifier = Modifier.fillMaxWidth(),
            variant = ComicPanelVariant.LAVENDER
        ) {
            Column {
                Text(
                    text = "LAVENDER CONTAINER (#B8A4D4)",
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Muted pop-art accent container with 10dp rounded corners.",
                    color = Color.Black,
                    fontSize = 11.sp
                )
            }
        }

        // 5. Compact Side-by-Side Stat Panels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ComicPanel(
                modifier = Modifier.weight(1f),
                variant = ComicPanelVariant.BURNT_ORANGE,
                contentPadding = 12.dp
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "STREAK", fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Text(text = "14 DAYS", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            ComicPanel(
                modifier = Modifier.weight(1f),
                variant = ComicPanelVariant.SKY_BLUE,
                contentPadding = 12.dp
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "COMPLETED", fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Text(text = "92%", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
